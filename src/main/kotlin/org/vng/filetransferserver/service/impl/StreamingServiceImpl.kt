package org.vng.filetransferserver.service.impl

import org.vng.filetransferserver.dto.MediaInfoDTO
import org.vng.filetransferserver.exception.FileNotFoundException
import org.vng.filetransferserver.service.StreamingService
import org.vng.filetransferserver.util.FileUtils
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import jakarta.servlet.http.HttpServletRequest

private val logger = KotlinLogging.logger {}

@Service
class StreamingServiceImpl(
    @Qualifier("storageDirectory") private val storagePath: Path
) : StreamingService {

    override fun streamMedia(filename: String, request: HttpServletRequest): ResponseEntity<Resource> {
        try {
            val file = findFile(filename)
            val resource = FileSystemResource(file)
            val contentType = determineContentType(file)
            val fileSize = file.length()

            // Check if it's an image - serve directly without range support
            if (contentType.startsWith("image/")) {
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, fileSize.toString())
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(resource)
            }

            val rangeHeader = request.getHeader("Range")

            if (rangeHeader == null) {
                // Return full file
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_LENGTH, fileSize.toString())
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource)
            }

            // Parse range header
            val range = parseRangeHeader(rangeHeader, fileSize)
            val start = range.first
            val end = range.second
            val contentLength = end - start + 1

            logger.debug { "Streaming range: $start-$end of $fileSize for ${file.name}" }

            val headers = HttpHeaders()
            headers.add(HttpHeaders.CONTENT_TYPE, contentType)
            headers.add(HttpHeaders.CONTENT_RANGE, "bytes $start-$end/$fileSize")
            headers.add(HttpHeaders.CONTENT_LENGTH, contentLength.toString())
            headers.add(HttpHeaders.ACCEPT_RANGES, "bytes")
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache")

            return ResponseEntity
                .status(HttpStatus.PARTIAL_CONTENT)
                .headers(headers)
                .body(createPartialResource(resource, start, end))

        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Error streaming file: $filename" }
            throw e
        }
    }

    override fun getMediaInfo(filename: String): MediaInfoDTO {
        val file = findFile(filename)
        val relativePath = storagePath.relativize(file.toPath()).toString()

        return MediaInfoDTO(
            filename = file.name,
            title = file.nameWithoutExtension,
            path = relativePath,
            parentFolder = file.parentFile?.name ?: "",
            folderPath = if (file.parentFile == storagePath.toFile()) ""
            else storagePath.relativize(file.parentFile?.toPath() ?: storagePath).toString(),
            size = file.length(),
            sizeFormatted = FileUtils.formatFileSize(file.length()),
            contentType = determineContentType(file),
            lastModified = java.time.Instant.ofEpochMilli(file.lastModified()),
            duration = null,
            bitrate = null,
            resolution = FileUtils.detectResolution(file.name),
            videoCodec = null,
            audioCodec = null,
            hasAudio = true,
            streamUrl = "/api/stream/$relativePath",
            downloadUrl = "/api/files/$relativePath"
        )
    }

    override fun getThumbnail(filename: String): Resource? {
        try {
            val file = findFile(filename)
            val contentType = determineContentType(file)

            // For images, return the image itself as thumbnail
            if (contentType.startsWith("image/")) {
                return FileSystemResource(file)
            }

            return null
        } catch (e: Exception) {
            logger.debug { "Could not generate thumbnail for: $filename" }
            return null
        }
    }

    override fun getPreview(filename: String): Resource? {
        try {
            val file = findFile(filename)
            val contentType = determineContentType(file)

            // Preview for images
            if (contentType.startsWith("image/")) {
                return FileSystemResource(file)
            }

            // Preview for text-based files
            if (contentType.startsWith("text/") ||
                contentType == "application/json" ||
                contentType == "application/xml" ||
                FileUtils.getFileTypeCategory(filename) == "code") {
                return FileSystemResource(file)
            }

            // For PDFs
            if (contentType == "application/pdf") {
                return FileSystemResource(file)
            }

            return null
        } catch (e: Exception) {
            logger.debug { "Could not generate preview for: $filename" }
            return null
        }
    }

    private fun findFile(filename: String): File {
        val decodedFilename = java.net.URLDecoder.decode(filename, "UTF-8")

        // 1. Try to resolve the path exactly as requested
        var file = storagePath.resolve(decodedFilename).normalize().toFile()

        // 2. If not found, check categorized subfolders (Audio, Videos, etc.)
        if (!file.exists()) {
            // Logic from your FileServiceImpl to determine the folder
            val extension = decodedFilename.substringAfterLast(".", "").lowercase()
            val typeFolder = when (".$extension") {
                in listOf(".mp4", ".webm", ".mkv", ".avi", ".mov", ".mpeg", ".flv", ".m4v") -> "Videos"
                in listOf(".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a") -> "Audio"
                in listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp") -> "Images"
                in listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx") -> "Documents"
                else -> null
            }

            if (typeFolder != null) {
                val categorizedFile = storagePath.resolve(typeFolder).resolve(decodedFilename).normalize().toFile()
                if (categorizedFile.exists()) {
                    file = categorizedFile
                }
            }
        }

        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("File not found: $filename")
        }

        // Security check
        if (!file.absolutePath.startsWith(storagePath.toAbsolutePath().toString())) {
            throw SecurityException("Path traversal detected")
        }

        return file
    }

    private fun determineContentType(file: File): String {
        val fileName = file.name.lowercase()
        return when {
            fileName.endsWith(".mp4") -> "video/mp4"
            fileName.endsWith(".webm") -> "video/webm"
            fileName.endsWith(".mkv") -> "video/x-matroska"
            fileName.endsWith(".avi") -> "video/x-msvideo"
            fileName.endsWith(".mov") -> "video/quicktime"
            fileName.endsWith(".mp3") -> "audio/mpeg"
            fileName.endsWith(".wav") -> "audio/wav"
            fileName.endsWith(".flac") -> "audio/flac"
            fileName.endsWith(".aac") -> "audio/aac"
            fileName.endsWith(".ogg") -> "audio/ogg"
            fileName.endsWith(".m4a") -> "audio/mp4"
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".gif") -> "image/gif"
            fileName.endsWith(".webp") -> "image/webp"
            fileName.endsWith(".bmp") -> "image/bmp"
            fileName.endsWith(".svg") -> "image/svg+xml"
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".txt") -> "text/plain"
            fileName.endsWith(".html") -> "text/html"
            fileName.endsWith(".css") -> "text/css"
            fileName.endsWith(".js") -> "application/javascript"
            fileName.endsWith(".json") -> "application/json"
            fileName.endsWith(".xml") -> "application/xml"
            else -> {
                val mimeType = Files.probeContentType(file.toPath())
                mimeType ?: "application/octet-stream"
            }
        }
    }

    private fun parseRangeHeader(rangeHeader: String, fileSize: Long): Pair<Long, Long> {
        // Format: "bytes=start-end"
        val rangePattern = Regex("bytes=(\\d*)-(\\d*)")
        val matchResult = rangePattern.find(rangeHeader)

        if (matchResult != null) {
            val startStr = matchResult.groupValues[1]
            val endStr = matchResult.groupValues[2]

            val start = if (startStr.isNotEmpty()) startStr.toLong() else 0L
            val end = if (endStr.isNotEmpty()) endStr.toLong() else fileSize - 1

            // Validate range bounds
            val validStart = start.coerceIn(0, fileSize - 1)
            val validEnd = end.coerceIn(validStart, fileSize - 1)

            return Pair(validStart, validEnd)
        }

        return Pair(0, fileSize - 1)
    }

    private fun createPartialResource(resource: Resource, start: Long, end: Long): Resource {
        return object : Resource {
            override fun getInputStream(): InputStream {
                val inputStream = resource.inputStream
                // Skip to start position
                var bytesToSkip = start
                while (bytesToSkip > 0) {
                    val skipped = inputStream.skip(bytesToSkip)
                    if (skipped <= 0) break
                    bytesToSkip -= skipped
                }
                return inputStream
            }

            override fun exists(): Boolean = resource.exists()
            override fun isReadable(): Boolean = resource.isReadable
            override fun isOpen(): Boolean = resource.isOpen
            override fun getURL(): java.net.URL = resource.url
            override fun getURI(): java.net.URI = resource.uri
            override fun getFile(): File = resource.file
            override fun contentLength(): Long = end - start + 1
            override fun lastModified(): Long = resource.lastModified()

            override fun createRelative(relativePath: String): Resource {
                // Create a new resource relative to the current resource's parent
                val currentFile = resource.file
                val parentDir = currentFile.parentFile
                if (parentDir != null) {
                    val relativeFile = File(parentDir, relativePath)
                    if (relativeFile.exists()) {
                        return FileSystemResource(relativeFile)
                    }
                }
                // Fallback: return the original resource
                return resource
            }

            override fun getFilename(): String? = resource.filename
            override fun getDescription(): String = "Partial content from ${resource.description}"
        }
    }
}