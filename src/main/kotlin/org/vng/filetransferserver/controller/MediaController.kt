package org.vng.filetransferserver.controller

import mu.KotlinLogging
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.vng.filetransferserver.dto.*
import org.vng.filetransferserver.service.FileService
import org.vng.filetransferserver.service.StreamingService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.vng.filetransferserver.exception.FileNotFoundException
import org.vng.filetransferserver.exception.InvalidFileTypeException
import org.vng.filetransferserver.exception.PathTraversalException
import java.nio.file.Files

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class MediaController(
    private val fileService: FileService, private val streamingService: StreamingService
) {

    // File Management Endpoints
    @PostMapping("/files/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile, @RequestParam(value = "path", required = false) folderPath: String?
    ): ResponseEntity<FileUploadResponseDTO> {
        logger.info { "POST /api/files/upload - file: ${file.originalFilename}, folder: $folderPath" }
        val response = fileService.uploadFile(file, folderPath)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/files")
    fun listAllFiles(): ResponseEntity<List<MediaInfoDTO>> {
        logger.info { "GET /api/files" }
        val files = fileService.listAllFiles()
        return ResponseEntity.ok(files)
    }

    @GetMapping("/files/{filename:.+}")
    fun downloadFile(
        @PathVariable filename: String, response: HttpServletResponse
    ) {
        logger.info { "GET /api/files/$filename" }
        val filePath = fileService.downloadFile(filename)
        val file = filePath.toFile()

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.name}\"")
        response.setHeader(HttpHeaders.CONTENT_LENGTH, file.length().toString())
        response.contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE

        FileSystemResource(file).inputStream.use { input ->
            input.copyTo(response.outputStream)
        }
        response.outputStream.flush()
    }

    @DeleteMapping("/files/{filename:.+}")
    fun deleteFile(@PathVariable filename: String, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        logger.info { "DELETE /api/files/$filename" }

        // Decode the filename if it was encoded
        val decodedFilename = try {
            java.net.URLDecoder.decode(filename, "UTF-8")
        } catch (e: Exception) {
            filename
        }

        logger.info { "Decoded filename: $decodedFilename" }

        return try {
            val success = fileService.deleteFile(decodedFilename)
            ResponseEntity.ok(
                mapOf(
                    "success" to success,
                    "message" to "File deleted successfully",
                    "filename" to decodedFilename
                )
            )
        } catch (e: FileNotFoundException) {
            logger.error { "File not found: $decodedFilename" }
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                mapOf(
                    "success" to false,
                    "message" to "File not found: $decodedFilename"
                )
            )
        } catch (e: InvalidFileTypeException) {
            logger.error { "Invalid file type: ${e.message}" }
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                mapOf(
                    "success" to false,
                    "message" to (e.message ?: "Invalid file type")
                )
            )
        } catch (e: PathTraversalException) {
            logger.error { "Path traversal attempt: ${e.message}" }
            ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                mapOf(
                    "success" to false,
                    "message" to "Access denied: Invalid path"
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deleting file: $decodedFilename" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                mapOf(
                    "success" to false,
                    "message" to "Failed to delete file: ${e.message}"
                )
            )
        }
    }

    @GetMapping("/files/{filename:.+}/metadata")
    fun getFileMetadata(@PathVariable filename: String): ResponseEntity<FileMetadataDTO> {
        logger.info { "GET /api/files/$filename/metadata" }
        val metadata = fileService.getFileMetadata(filename)
        return ResponseEntity.ok(metadata)
    }

    @PutMapping("/files/move")
    fun moveFile(
        @RequestParam source: String, @RequestParam destination: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info { "PUT /api/files/move - source: $source, destination: $destination" }
        val success = fileService.moveFile(source, destination)
        return ResponseEntity.ok(mapOf("success" to success))
    }

    @PutMapping("/files/copy")
    fun copyFile(
        @RequestParam source: String, @RequestParam destination: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info { "PUT /api/files/copy - source: $source, destination: $destination" }
        val success = fileService.copyFile(source, destination)
        return ResponseEntity.ok(mapOf("success" to success))
    }

    @PutMapping("/files/rename")
    fun renameFile(
        @RequestParam path: String, @RequestParam newName: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info { "PUT /api/files/rename - path: $path, newName: $newName" }
        val success = fileService.renameFile(path, newName)
        return ResponseEntity.ok(mapOf("success" to success))
    }

    // Folder Management Endpoints
    @GetMapping("/folders/contents")
    fun getFolderContents(
        @RequestParam(value = "path", required = false, defaultValue = "") path: String
    ): ResponseEntity<FolderContentsDTO> {
        logger.info { "GET /api/folders/contents - path: $path" }
        val contents = fileService.getFolderContents(path.ifEmpty { null })
        return ResponseEntity.ok(contents)
    }

    @PostMapping("/folders")
    fun createFolder(
        @RequestParam(value = "path") folderPath: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info { "POST /api/folders - path: $folderPath" }
        val success = fileService.createFolder(folderPath)
        return ResponseEntity.ok(mapOf("success" to success))
    }

    @DeleteMapping("/folders/{path:.+}")
    fun deleteFolder(@PathVariable path: String): ResponseEntity<Map<String, Any>> {
        logger.info { "DELETE /api/folders/$path" }
        val success = fileService.deleteFolder(path)
        return ResponseEntity.ok(mapOf("success" to success))
    }

    @PutMapping("/folders/move")
    fun moveFolder(
        @RequestParam source: String, @RequestParam destination: String
    ): ResponseEntity<Map<String, Any>> {
        logger.info { "PUT /api/folders/move - source: $source, destination: $destination" }
        val success = fileService.moveFolder(source, destination)
        return ResponseEntity.ok(mapOf("success" to success))
    }

    @GetMapping("/folders/tree")
    fun getFolderTree(): ResponseEntity<List<FolderInfoDTO>> {
        logger.info { "GET /api/folders/tree" }
        val tree = fileService.getFolderTree()
        return ResponseEntity.ok(tree)
    }

    // Streaming Endpoints
    @GetMapping("/stream")
    fun streamMedia(
        @RequestParam("path") path: String, request: HttpServletRequest, response: HttpServletResponse
    ) {
        logger.info { "GET /api/stream - path: $path" }
        streamingService.streamMedia(path, request, response)
    }

    @GetMapping("/media")
    fun listMediaFiles(): ResponseEntity<List<Map<String, Any?>>> {
        logger.info { "GET /api/media" }
        val mediaFiles = streamingService.listMediaFiles()
        return ResponseEntity.ok(mediaFiles)
    }

    @GetMapping("/media/info")
    fun getMediaInfo(@RequestParam("path") path: String): ResponseEntity<Map<String, Any?>> {
        logger.info { "GET /api/media/info - path: $path" }
        val info = streamingService.getMediaInfo(path)
        return ResponseEntity.ok(info)
    }

    // Search and System Endpoints
    @GetMapping("/search")
    fun searchFiles(
        @RequestParam query: String, @RequestParam(value = "folder", required = false) folderPath: String?
    ): ResponseEntity<SearchResultDTO> {
        logger.info { "GET /api/search - query: $query, folder: $folderPath" }
        val results = fileService.searchFiles(query, folderPath)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/system/info")
    fun getSystemInfo(): ResponseEntity<FileSystemInfoDTO> {
        logger.info { "GET /api/system/info" }
        val info = fileService.getSystemInfo()
        return ResponseEntity.ok(info)
    }

    // Preview Endpoints
    @GetMapping("/preview")
    fun getFilePreview(@RequestParam("path") path: String, response: HttpServletResponse) {
        logger.info { "GET /api/preview - path: $path" }
        val filePath = fileService.getFilePreview(path)
        val file = filePath.toFile()
        val contentType = fileService.getFileMetadata(path).contentType

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${file.name}\"")
        response.setHeader(HttpHeaders.CONTENT_LENGTH, file.length().toString())
        response.contentType = contentType

        FileSystemResource(file).inputStream.use { input ->
            input.copyTo(response.outputStream)
        }
        response.outputStream.flush()
    }

    @GetMapping("/thumbnail")
    fun getThumbnail(@RequestParam("path") path: String): ResponseEntity<ByteArray> {
        logger.info { "GET /api/thumbnail - path: $path" }
        val thumbnailPath = fileService.getThumbnail(path)
        if (thumbnailPath != null && Files.exists(thumbnailPath)) {
            val bytes = Files.readAllBytes(thumbnailPath)
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes)
        }
        return ResponseEntity.notFound().build()
    }

    // Batch Operations
    @PostMapping("/batch")
    fun batchOperation(@RequestBody request: BatchOperationDTO): ResponseEntity<BatchOperationResponseDTO> {
        logger.info { "POST /api/batch - operation: ${request.operation}, files: ${request.files.size}" }
        val response = fileService.batchOperation(request)
        return ResponseEntity.ok(response)
    }
}