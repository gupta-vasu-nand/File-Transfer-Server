package org.vng.filetransferserver.service.impl

import mu.KotlinLogging
import org.apache.tika.Tika
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.vng.filetransferserver.config.StorageProperties
import org.vng.filetransferserver.exception.FileNotFoundException
import org.vng.filetransferserver.exception.PathTraversalException
import org.vng.filetransferserver.service.StreamingService
import org.vng.filetransferserver.util.FileUtils
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val logger = KotlinLogging.logger {}

@Service
class StreamingServiceImpl(
    private val storageProperties: StorageProperties
) : StreamingService {

    private val storagePath: Path = storageProperties.getStoragePath()
    private val tika = Tika()

    private fun resolvePath(path: String): Path {
        val normalized = FileUtils.normalizePath(path)
        val targetPath = storagePath.resolve(normalized).normalize()

        if (!targetPath.startsWith(storagePath)) {
            logger.warn { "Path traversal attempt: $path" }
            throw PathTraversalException("Access denied: Invalid path")
        }

        return targetPath
    }

    override fun streamMedia(path: String, request: HttpServletRequest, response: HttpServletResponse) {
        logger.info { "Streaming media: $path" }

        val targetFile = resolvePath(path)

        if (!Files.exists(targetFile) || Files.isDirectory(targetFile)) {
            throw FileNotFoundException("File not found: $path")
        }

        val file = targetFile.toFile()
        val fileSize = file.length()

        // Handle empty file case
        if (fileSize == 0L) {
            logger.warn { "Attempted to stream empty file: $path" }
            response.status = HttpStatus.NO_CONTENT.value()
            return
        }

        val contentType = tika.detect(targetFile)

        // Parse Range header
        val rangeHeader = request.getHeader("Range")

        try {
            response.setHeader(HttpHeaders.CONTENT_TYPE, contentType)
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes")

            if (rangeHeader == null) {
                // Return full file
                streamFullFile(file, fileSize, response, path)
            } else {
                // Return partial content
                streamPartialFile(file, fileSize, rangeHeader, response, path)
            }
        } catch (e: IOException) {
            if (isClientDisconnect(e)) {
                logger.debug { "Client disconnected during streaming: $path" }
                return
            }
            logger.error(e) { "Error streaming file: $path" }
            if (!response.isCommitted) {
                try {
                    response.reset()
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Streaming error")
                } catch (ex: Exception) {
                    logger.debug { "Could not send error response: ${ex.message}" }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            logger.error(e) { "Index out of bounds for file: $path, fileSize: $fileSize" }
            if (!response.isCommitted) {
                try {
                    response.reset()
                    response.sendError(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value(), "Invalid range request")
                } catch (ex: Exception) {
                    logger.debug { "Could not send error response: ${ex.message}" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error during streaming: $path" }
            if (!response.isCommitted) {
                try {
                    response.reset()
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Streaming error")
                } catch (ex: Exception) {
                    logger.debug { "Could not send error response: ${ex.message}" }
                }
            }
        }
    }

    private fun streamFullFile(file: java.io.File, fileSize: Long, response: HttpServletResponse, path: String) {
        response.setHeader(HttpHeaders.CONTENT_LENGTH, fileSize.toString())
        response.status = HttpStatus.OK.value()

        try {
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalWritten = 0L
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    try {
                        response.outputStream.write(buffer, 0, bytesRead)
                        totalWritten += bytesRead
                        if (totalWritten % (8192 * 10) == 0L) {
                            response.outputStream.flush()
                        }
                    } catch (e: IOException) {
                        if (isClientDisconnect(e)) {
                            logger.debug { "Client disconnected during full streaming at ${totalWritten}/${fileSize} bytes" }
                            return
                        }
                        throw e
                    }
                }
                response.outputStream.flush()
            }
            logger.debug { "Full stream completed: $path, total: $fileSize bytes" }
        } catch (e: IOException) {
            if (isClientDisconnect(e)) {
                logger.debug { "Client disconnected during full streaming: $path" }
                return
            }
            throw e
        }
    }

    private fun streamPartialFile(
        file: java.io.File,
        fileSize: Long,
        rangeHeader: String,
        response: HttpServletResponse,
        path: String
    ) {
        val (start, end) = parseRange(rangeHeader, fileSize)

        // Validate range bounds
        if (start < 0 || end >= fileSize || start > end) {
            logger.warn { "Invalid range request: start=$start, end=$end, fileSize=$fileSize, header=$rangeHeader" }
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes */$fileSize")
            response.status = HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value()
            return
        }

        val contentLength = end - start + 1
        response.setHeader(HttpHeaders.CONTENT_LENGTH, contentLength.toString())
        response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$fileSize")
        response.status = HttpStatus.PARTIAL_CONTENT.value()

        try {
            RandomAccessFile(file, "r").use { raf ->
                raf.seek(start)
                // Use a larger buffer for better performance
                val bufferSize = 64 * 1024 // 64KB buffer
                val buffer = ByteArray(bufferSize)
                var bytesRemaining = contentLength
                var totalWritten = 0L

                while (bytesRemaining > 0) {
                    // Calculate read size safely using min of Long values
                    val readSize = minOf(bufferSize.toLong(), bytesRemaining).toInt()

                    // Ensure readSize is positive
                    if (readSize <= 0) {
                        logger.warn { "Invalid read size: $readSize, bytesRemaining: $bytesRemaining" }
                        break
                    }

                    val bytesRead = try {
                        raf.read(buffer, 0, readSize)
                    } catch (e: IndexOutOfBoundsException) {
                        logger.error(e) { "IndexOutOfBounds: readSize=$readSize, bytesRemaining=$bytesRemaining, filePointer=${raf.filePointer}, fileLength=${file.length()}" }
                        throw e
                    }

                    if (bytesRead == -1) {
                        logger.warn { "Unexpected EOF: expected $readSize bytes, got -1, filePointer=${raf.filePointer}, fileLength=${file.length()}" }
                        break
                    }

                    try {
                        response.outputStream.write(buffer, 0, bytesRead)
                        totalWritten += bytesRead
                        bytesRemaining -= bytesRead

                        // Flush periodically
                        if (totalWritten % (bufferSize * 10) == 0L) {
                            response.outputStream.flush()
                        }
                    } catch (e: IOException) {
                        if (isClientDisconnect(e)) {
                            logger.debug { "Client disconnected during partial streaming at ${totalWritten}/${contentLength} bytes" }
                            return
                        }
                        throw e
                    }
                }
                response.outputStream.flush()
                logger.debug { "Partial stream completed: $path, range: $rangeHeader, sent: $totalWritten bytes" }
            }
        } catch (e: IOException) {
            if (isClientDisconnect(e)) {
                logger.debug { "Client disconnected during partial streaming: $path" }
                return
            }
            throw e
        }
    }

    private fun isClientDisconnect(e: IOException): Boolean {
        val message = e.message ?: ""
        return message.contains("Broken pipe") ||
                message.contains("Connection reset") ||
                message.contains("An established connection was aborted") ||
                message.contains("Software caused connection abort") ||
                message.contains("Connection timed out") ||
                e is org.apache.catalina.connector.ClientAbortException
    }

    private fun parseRange(rangeHeader: String, fileSize: Long): Pair<Long, Long> {
        try {
            // Handle empty file case
            if (fileSize <= 0) {
                return 0L to 0L
            }

            val bytesRange = rangeHeader.replace("bytes=", "").trim()
            val parts = bytesRange.split("-")

            val start = if (parts[0].isNotEmpty()) {
                parts[0].toLong().coerceIn(0, fileSize - 1)
            } else {
                0L
            }

            val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].toLong().coerceIn(start, fileSize - 1)
            } else {
                // Open-ended range: from start to end of file
                fileSize - 1
            }

            return start to end
        } catch (e: Exception) {
            logger.warn(e) { "Invalid range header: $rangeHeader, using full range" }
            return 0L to (fileSize - 1)
        }
    }

    override fun getMediaInfo(path: String): Map<String, Any?> {
        logger.debug { "Getting media info: $path" }
        val targetFile = resolvePath(path)

        if (!Files.exists(targetFile) || Files.isDirectory(targetFile)) {
            throw FileNotFoundException("File not found: $path")
        }

        val file = targetFile.toFile()
        val contentType = tika.detect(targetFile)
        val isVideo = contentType.startsWith("video/")
        val isAudio = contentType.startsWith("audio/")

        return mapOf(
            "filename" to file.name,
            "path" to path,
            "size" to file.length(),
            "sizeFormatted" to FileUtils.formatFileSize(file.length()),
            "contentType" to contentType,
            "isVideo" to isVideo,
            "isAudio" to isAudio,
            "streamUrl" to "/api/stream?path=$path",
            "downloadUrl" to "/api/files/$path"
        )
    }

    override fun listMediaFiles(): List<Map<String, Any?>> {
        logger.debug { "Listing media files" }
        val mediaFiles = mutableListOf<Map<String, Any?>>()

        try {
            Files.walk(storagePath)
                .use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .forEach { file ->
                            val contentType = tika.detect(file)
                            if (contentType.startsWith("video/") || contentType.startsWith("audio/")) {
                                val relativePath = storagePath.relativize(file).toString()
                                mediaFiles.add(
                                    mapOf(
                                        "filename" to file.fileName.toString(),
                                        "path" to relativePath,
                                        "size" to Files.size(file),
                                        "contentType" to contentType,
                                        "streamUrl" to "/api/stream?path=${relativePath.replace("\\", "/")}"
                                    )
                                )
                            }
                        }
                }
        } catch (e: IOException) {
            logger.error(e) { "Failed to list media files" }
        }

        return mediaFiles
    }
}