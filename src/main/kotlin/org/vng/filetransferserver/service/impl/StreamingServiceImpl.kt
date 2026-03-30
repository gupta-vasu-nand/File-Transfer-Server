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
        val contentType = tika.detect(targetFile)

        // Parse Range header
        val rangeHeader = request.getHeader("Range")
        val (start, end) = parseRange(rangeHeader, fileSize)

        try {
            response.setHeader(HttpHeaders.CONTENT_TYPE, contentType)
            response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes")

            if (rangeHeader == null) {
                // Return full file
                response.setHeader(HttpHeaders.CONTENT_LENGTH, fileSize.toString())
                response.status = HttpStatus.OK.value()

                file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalWritten = 0L
                    try {
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            try {
                                response.outputStream.write(buffer, 0, bytesRead)
                                totalWritten += bytesRead
                                // Flush periodically but catch disconnects
                                if (totalWritten % (8192 * 10) == 0L) {
                                    try {
                                        response.outputStream.flush()
                                    } catch (e: IOException) {
                                        if (isClientDisconnect(e)) {
                                            logger.debug { "Client disconnected during flush at ${totalWritten}/${fileSize} bytes" }
                                            return
                                        }
                                        throw e
                                    }
                                }
                            } catch (e: IOException) {
                                if (isClientDisconnect(e)) {
                                    logger.debug { "Client disconnected during write at ${totalWritten}/${fileSize} bytes" }
                                    return
                                }
                                throw e
                            }
                        }
                        // Final flush
                        try {
                            response.outputStream.flush()
                        } catch (e: IOException) {
                            if (isClientDisconnect(e)) {
                                logger.debug { "Client disconnected during final flush" }
                                return
                            }
                            throw e
                        }
                    } catch (e: IOException) {
                        if (isClientDisconnect(e)) {
                            logger.debug { "Client disconnected during streaming: $path" }
                            return
                        }
                        throw e
                    }
                }
                logger.debug { "Stream completed: $path, total: $fileSize bytes" }
            } else {
                // Return partial content
                val contentLength = end - start + 1
                response.setHeader(HttpHeaders.CONTENT_LENGTH, contentLength.toString())
                response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$fileSize")
                response.status = HttpStatus.PARTIAL_CONTENT.value()

                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(start)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var bytesRemaining = contentLength
                    var totalWritten = 0L

                    try {
                        while (bytesRemaining > 0 && raf.filePointer < fileSize) {
                            val readSize = minOf(buffer.size, bytesRemaining.toInt())
                            bytesRead = raf.read(buffer, 0, readSize)
                            if (bytesRead == -1) break

                            try {
                                response.outputStream.write(buffer, 0, bytesRead)
                                totalWritten += bytesRead
                                bytesRemaining -= bytesRead

                                // Flush periodically
                                if (totalWritten % (8192 * 10) == 0L) {
                                    try {
                                        response.outputStream.flush()
                                    } catch (e: IOException) {
                                        if (isClientDisconnect(e)) {
                                            logger.debug { "Client disconnected during partial flush at ${totalWritten}/${contentLength} bytes" }
                                            return
                                        }
                                        throw e
                                    }
                                }
                            } catch (e: IOException) {
                                if (isClientDisconnect(e)) {
                                    logger.debug { "Client disconnected during partial write at ${totalWritten}/${contentLength} bytes" }
                                    return
                                }
                                throw e
                            }
                        }
                        // Final flush
                        try {
                            response.outputStream.flush()
                        } catch (e: IOException) {
                            if (isClientDisconnect(e)) {
                                logger.debug { "Client disconnected during final partial flush" }
                                return
                            }
                            throw e
                        }
                    } catch (e: IOException) {
                        if (isClientDisconnect(e)) {
                            logger.debug { "Client disconnected during partial streaming: $path" }
                            return
                        }
                        throw e
                    }
                    logger.debug { "Partial stream completed: $path, range: $rangeHeader, sent: $totalWritten bytes" }
                }
            }
        } catch (e: IOException) {
            if (isClientDisconnect(e)) {
                logger.debug { "Client disconnected during streaming setup: $path" }
                return
            }
            logger.error(e) { "Error streaming file: $path" }
            // Only try to send error if response is still usable
            if (!response.isCommitted) {
                try {
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Streaming error")
                } catch (ex: IllegalStateException) {
                    logger.debug { "Response already committed, cannot send error" }
                } catch (ex: IOException) {
                    logger.debug { "Failed to send error response" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error during streaming: $path" }
            if (!response.isCommitted) {
                try {
                    response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Streaming error")
                } catch (ex: IllegalStateException) {
                    logger.debug { "Response already committed, cannot send error" }
                } catch (ex: IOException) {
                    logger.debug { "Failed to send error response" }
                }
            }
        }
    }

    private fun isClientDisconnect(e: IOException): Boolean {
        val message = e.message ?: ""
        return message.contains("Broken pipe") ||
                message.contains("Connection reset") ||
                message.contains("An established connection was aborted") ||
                message.contains("Software caused connection abort") ||
                e is org.apache.catalina.connector.ClientAbortException
    }

    private fun parseRange(rangeHeader: String?, fileSize: Long): Pair<Long, Long> {
        if (rangeHeader == null) {
            return 0L to (fileSize - 1)
        }

        try {
            val bytesRange = rangeHeader.replace("bytes=", "")
            val parts = bytesRange.split("-")
            val start = parts[0].toLongOrNull() ?: 0L
            val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
                parts[1].toLongOrNull() ?: (fileSize - 1)
            } else {
                fileSize - 1
            }

            return start to end.coerceAtMost(fileSize - 1)
        } catch (e: Exception) {
            logger.warn(e) { "Invalid range header: $rangeHeader" }
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