package org.vng.filetransferserver.controller

/*
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.vng.filetransferserver.dto.FileMetadataDTO
import org.vng.filetransferserver.dto.FileUploadResponseDTO
import org.vng.filetransferserver.exception.FileNotFoundException
import org.vng.filetransferserver.service.FileService
import org.vng.filetransferserver.service.MediaInfoDTO
import org.vng.filetransferserver.service.StreamingService
import org.vng.filetransferserver.service.impl.ClientDisconnectedException

@RestController
@RequestMapping("/api")
class FileController(
    private val fileService: FileService,
    private val streamingService: StreamingService
) {

    private val logger = LoggerFactory.getLogger(FileController::class.java)

    @PostMapping("/files/upload")
    fun uploadFile(@RequestParam("file") file: MultipartFile): ResponseEntity<FileUploadResponseDTO> {
        logger.info("Received upload request for file: ${file.originalFilename}")
        val filename = fileService.uploadFile(file)

        val downloadUrl = "http://localhost:9090/api/files/$filename"

        val response = FileUploadResponseDTO(
            filename = filename,
            originalFilename = file.originalFilename ?: filename,
            size = file.size,
            contentType = file.contentType ?: "application/octet-stream",
            uploadTime = java.time.LocalDateTime.now(),
            downloadUrl = downloadUrl,
            message = "File uploaded successfully"
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/files")
    fun listFiles(): ResponseEntity<List<FileMetadataDTO>> {
        logger.info("Received request to list all files")
        return ResponseEntity.ok(fileService.listFiles())
    }

    @GetMapping("/files/{filename}")
    fun downloadFile(@PathVariable("filename") filename: String): ResponseEntity<Resource> {
        logger.info("Received download request for file: $filename")
        val resource = fileService.downloadFile(filename)
        val metadata = fileService.getFileMetadata(filename)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${metadata.filename}\"")
            .contentType(org.springframework.http.MediaType.parseMediaType(metadata.contentType ?: "application/octet-stream"))
            .contentLength(metadata.size)
            .body(resource)
    }

    @DeleteMapping("/files/{filename}")
    fun deleteFile(@PathVariable("filename") filename: String): ResponseEntity<Map<String, String>> {
        logger.info("Received delete request for file: $filename")
        fileService.deleteFile(filename)
        return ResponseEntity.ok(mapOf("message" to "File deleted successfully", "filename" to filename))
    }

    @GetMapping("/files/{filename}/metadata")
    fun getFileMetadata(@PathVariable("filename") filename: String): ResponseEntity<FileMetadataDTO> {
        logger.info("Received metadata request for file: $filename")
        return ResponseEntity.ok(fileService.getFileMetadata(filename))
    }

    // Streaming endpoint with range support
    @GetMapping("/stream/{filename}")
    fun streamMedia(
        @PathVariable("filename") filename: String,
        @RequestHeader(value = "Range", required = false) rangeHeader: String?,
        response: HttpServletResponse
    ): ResponseEntity<Any> {
        logger.info("Received stream request for: $filename, Range: $rangeHeader")

        try {
            val streamResponse = streamingService.streamMedia(filename, rangeHeader)

            val headers = HttpHeaders().apply {
                set("Accept-Ranges", "bytes")
                set("Content-Type", streamResponse.contentType)
                set("Content-Length", streamResponse.contentLength.toString())
                set("Cache-Control", "no-cache")
                set("Connection", "keep-alive")

                if (rangeHeader != null) {
                    set(HttpHeaders.CONTENT_RANGE, "bytes ${streamResponse.rangeStart}-${streamResponse.rangeEnd}/${streamResponse.totalSize}")
                }
            }

            val status = if (rangeHeader != null) HttpStatus.PARTIAL_CONTENT else HttpStatus.OK

            return ResponseEntity.status(status)
                .headers(headers)
                .body(streamResponse.resource)

        } catch (e: ClientDisconnectedException) {
            // Client disconnected - don't log as error
            logger.debug("Client disconnected during streaming: ${e.message}")
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build()
        } catch (e: FileNotFoundException) {
            logger.warn("File not found: $filename")
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        } catch (e: Exception) {
            // Don't try to send error response if it's a connection issue
            if (e.message?.contains("broken pipe") == true ||
                e.message?.contains("aborted") == true ||
                e.cause?.message?.contains("broken pipe") == true) {
                logger.debug("Connection issue during streaming: ${e.message}")
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).build()
            }

            logger.error("Error streaming media: ${e.message}", e)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }

    // Get media information
    @GetMapping("/media/{filename}/info")
    fun getMediaInfo(@PathVariable("filename") filename: String): ResponseEntity<MediaInfoDTO> {
        logger.info("Received media info request for: $filename")
        val mediaInfo = streamingService.getMediaInfo(filename)
        return ResponseEntity.ok(mediaInfo)
    }

    // Get all media files (videos and audio)
    @GetMapping("/media")
    fun listMediaFiles(): ResponseEntity<List<MediaInfoDTO>> {
        logger.info("Received request to list all media files")
        val allFiles = fileService.listFiles()
        val mediaFiles = allFiles.filter {
            it.contentType?.startsWith("video/") == true ||
                    it.contentType?.startsWith("audio/") == true
        }.map { file ->
            streamingService.getMediaInfo(file.filename)
        }
        return ResponseEntity.ok(mediaFiles)
    }
}

 */