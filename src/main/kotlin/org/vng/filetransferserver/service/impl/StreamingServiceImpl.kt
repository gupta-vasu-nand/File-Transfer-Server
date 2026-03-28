package org.vng.filetransferserver.service.impl

import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.vng.filetransferserver.config.StorageConfig
import org.vng.filetransferserver.exception.FileNotFoundException
import org.vng.filetransferserver.exception.FileStorageException
import org.vng.filetransferserver.service.MediaInfoDTO
import org.vng.filetransferserver.service.MediaMetadata
import org.vng.filetransferserver.service.StreamResponse
import org.vng.filetransferserver.service.StreamingService
import org.vng.filetransferserver.util.FileUtils
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

@Service
class StreamingServiceImpl(
    private val storageConfig: StorageConfig,
    private val fileUtils: FileUtils
) : StreamingService {

    private val logger = LoggerFactory.getLogger(StreamingServiceImpl::class.java)

    companion object {
        private val BUFFER_SIZE = 8192 // 8KB buffer for streaming
    }

    override fun streamMedia(filename: String, rangeHeader: String?): StreamResponse {
        val sanitizedFilename = fileUtils.sanitizeFilename(filename)
        val filePath = storageConfig.getStoragePath().resolve(sanitizedFilename)

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            logger.warn("Media file not found: $sanitizedFilename")
            throw FileNotFoundException("Media file not found: $filename")
        }

        val contentType = detectContentType(filePath, filename) ?: "application/octet-stream"
        val fileSize = Files.size(filePath)

        return if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            handleRangeRequest(filePath, contentType, fileSize, rangeHeader)
        } else {
            handleFullRequest(filePath, contentType, fileSize)
        }
    }

    private fun handleRangeRequest(
        filePath: Path,
        contentType: String,
        fileSize: Long,
        rangeHeader: String
    ): StreamResponse {
        try {
            val range = parseRange(rangeHeader, fileSize)
            val inputStream = Files.newInputStream(filePath)
            inputStream.skip(range.start)

            // Create a custom InputStreamResource that handles disconnections
            val resource = object : InputStreamResource(inputStream) {
                override fun getInputStream(): InputStream {
                    return inputStream
                }

                override fun contentLength(): Long {
                    return range.end - range.start + 1
                }
            }

            logger.info("Streaming range ${range.start}-${range.end} of ${filePath.fileName}")

            return StreamResponse(
                resource = resource,
                contentType = contentType,
                contentLength = range.end - range.start + 1,
                rangeStart = range.start,
                rangeEnd = range.end,
                totalSize = fileSize
            )
        } catch (e: IOException) {
            if (e.message?.contains("aborted") == true || e.message?.contains("broken pipe") == true) {
                logger.warn("Client disconnected during streaming: ${e.message}")
                // Rethrow as a specific exception that won't trigger error response
                throw ClientDisconnectedException("Client disconnected", e)
            }
            logger.error("Failed to stream media: ${e.message}", e)
            throw FileStorageException("Failed to stream media: ${e.message}", e)
        }
    }

    private fun handleFullRequest(
        filePath: Path,
        contentType: String,
        fileSize: Long
    ): StreamResponse {
        try {
            val inputStream: InputStream = Files.newInputStream(filePath)
            val resource = object : InputStreamResource(inputStream) {
                override fun getInputStream(): InputStream {
                    return inputStream
                }
            }

            logger.info("Streaming full file: ${filePath.fileName}")

            return StreamResponse(
                resource = resource,
                contentType = contentType,
                contentLength = fileSize,
                rangeStart = 0,
                rangeEnd = fileSize - 1,
                totalSize = fileSize
            )
        } catch (e: IOException) {
            if (e.message?.contains("aborted") == true || e.message?.contains("broken pipe") == true) {
                logger.warn("Client disconnected during streaming: ${e.message}")
                throw ClientDisconnectedException("Client disconnected", e)
            }
            logger.error("Failed to stream media: ${e.message}", e)
            throw FileStorageException("Failed to stream media: ${e.message}", e)
        }
    }

    override fun getMediaMetadata(filename: String): MediaMetadata {
        val sanitizedFilename = fileUtils.sanitizeFilename(filename)
        val filePath = storageConfig.getStoragePath().resolve(sanitizedFilename)

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw FileNotFoundException("Media file not found: $filename")
        }

        val contentType = detectContentType(filePath, filename) ?: "application/octet-stream"
        val size = Files.size(filePath)

        return MediaMetadata(
            filename = sanitizedFilename,
            contentType = contentType,
            size = size,
            supportsRange = true
        )
    }

    override fun getMediaInfo(filename: String): MediaInfoDTO {
        val sanitizedFilename = fileUtils.sanitizeFilename(filename)
        val filePath = storageConfig.getStoragePath().resolve(sanitizedFilename)

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw FileNotFoundException("Media file not found: $filename")
        }

        val contentType = detectContentType(filePath, filename) ?: "application/octet-stream"
        val size = Files.size(filePath)

        // Extract title from filename (remove extension)
        val title = sanitizedFilename.substringBeforeLast(".")

        // Detect media type
        val isVideo = contentType.startsWith("video/")

        // Calculate bitrate approximation
        val bitrate = if (isVideo) {
            (size * 8) / 1024 // Kbps approximation
        } else null

        // Extract resolution from filename if possible
        val resolution = when {
            sanitizedFilename.contains("1080p") -> "1920x1080"
            sanitizedFilename.contains("720p") -> "1280x720"
            sanitizedFilename.contains("480p") -> "854x480"
            else -> null
        }

        // Detect codecs from filename
        val (videoCodec, audioCodec) = detectCodecsFromFilename(sanitizedFilename)

        return MediaInfoDTO(
            filename = sanitizedFilename,
            title = title,
            duration = null,
            size = size,
            sizeFormatted = fileUtils.formatFileSize(size),
            contentType = contentType,
            bitrate = bitrate,
            resolution = resolution,
            thumbnail = null,
            videoCodec = videoCodec,
            audioCodec = audioCodec,
            audioChannels = null,
            hasAudio = audioCodec != null
        )
    }

    private fun detectContentType(filePath: Path, filename: String): String? {
        val extension = filename.substringAfterLast(".").lowercase()

        val mimeTypes = mapOf(
            "mp4" to "video/mp4",
            "mkv" to "video/x-matroska",
            "avi" to "video/x-msvideo",
            "mov" to "video/quicktime",
            "webm" to "video/webm",
            "mp3" to "audio/mpeg",
            "m4a" to "audio/mp4",
            "wav" to "audio/wav",
            "flac" to "audio/flac",
            "ogg" to "audio/ogg"
        )

        return mimeTypes[extension] ?: Files.probeContentType(filePath)
    }

    private fun detectCodecsFromFilename(filename: String): Pair<String?, String?> {
        var videoCodec: String? = null
        var audioCodec: String? = null

        val lowerFilename = filename.lowercase()

        when {
            lowerFilename.contains("h265") || lowerFilename.contains("hevc") -> videoCodec = "H.265/HEVC"
            lowerFilename.contains("h264") || lowerFilename.contains("x264") -> videoCodec = "H.264"
            lowerFilename.contains("vp9") -> videoCodec = "VP9"
            lowerFilename.contains("vp8") -> videoCodec = "VP8"
        }

        when {
            lowerFilename.contains("aac") -> audioCodec = "AAC"
            lowerFilename.contains("mp3") -> audioCodec = "MP3"
            lowerFilename.contains("ac3") || lowerFilename.contains("ac-3") -> audioCodec = "AC-3"
            lowerFilename.contains("eac3") -> audioCodec = "E-AC-3"
            lowerFilename.contains("dts") -> audioCodec = "DTS"
            lowerFilename.contains("opus") -> audioCodec = "Opus"
        }

        return Pair(videoCodec, audioCodec)
    }

    private fun parseRange(rangeHeader: String, fileSize: Long): Range {
        val bytesRange = rangeHeader.substringAfter("bytes=")
        val parts = bytesRange.split("-")

        val start = if (parts[0].isNotEmpty()) parts[0].toLong() else 0
        val end = if (parts.size > 1 && parts[1].isNotEmpty()) {
            parts[1].toLong().coerceAtMost(fileSize - 1)
        } else {
            fileSize - 1
        }

        return Range(start, end)
    }

    private data class Range(val start: Long, val end: Long)
}

// Custom exception for client disconnection
class ClientDisconnectedException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)