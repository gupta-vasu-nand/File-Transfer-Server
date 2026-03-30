package org.vng.filetransferserver.service.impl

import mu.KotlinLogging
import org.apache.tika.Tika
import org.springframework.core.io.FileSystemResource
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

        response.setHeader(HttpHeaders.CONTENT_TYPE, contentType)
        response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes")

        if (rangeHeader == null) {
            // Return full file
            response.setHeader(HttpHeaders.CONTENT_LENGTH, fileSize.toString())
            response.status = HttpStatus.OK.value()

            try {
                FileSystemResource(file).inputStream.use { input ->
                    input.copyTo(response.outputStream)
                }
                response.outputStream.flush()
            } catch (e: IOException) {
                if (e.message?.contains("Broken pipe") == true) {
                    logger.debug { "Client disconnected during streaming" }
                } else {
                    logger.error(e) { "Error streaming file" }
                    throw e
                }
            }
        } else {
            // Return partial content
            val contentLength = end - start + 1
            response.setHeader(HttpHeaders.CONTENT_LENGTH, contentLength.toString())
            response.setHeader(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$fileSize")
            response.status = HttpStatus.PARTIAL_CONTENT.value()

            try {
                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(start)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var bytesRemaining = contentLength

                    while (bytesRemaining > 0 && raf.filePointer < fileSize) {
                        bytesRead = raf.read(buffer, 0, minOf(buffer.size, bytesRemaining.toInt()))
                        if (bytesRead == -1) break
                        response.outputStream.write(buffer, 0, bytesRead)
                        bytesRemaining -= bytesRead
                    }
                }
                response.outputStream.flush()
            } catch (e: IOException) {
                if (e.message?.contains("Broken pipe") == true) {
                    logger.debug { "Client disconnected during streaming" }
                } else {
                    logger.error(e) { "Error streaming file" }
                    throw e
                }
            }
        }

        logger.debug { "Stream completed: $path, range: $rangeHeader" }
    }

    private fun parseRange(rangeHeader: String?, fileSize: Long): Pair<Long, Long> {
        if (rangeHeader == null) {
            return 0L to (fileSize - 1)
        }

        val bytesRange = rangeHeader.replace("bytes=", "")
        val parts = bytesRange.split("-")
        val start = parts[0].toLongOrNull() ?: 0L
        val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1].toLongOrNull() ?: (fileSize - 1)
        } else {
            fileSize - 1
        }

        return start to end
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
                .filter { Files.isRegularFile(it) }
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
        } catch (e: IOException) {
            logger.error(e) { "Failed to list media files" }
        }

        return mediaFiles
    }
}