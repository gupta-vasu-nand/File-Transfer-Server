package org.vng.filetransferserver.controller

import org.vng.filetransferserver.dto.*
import org.vng.filetransferserver.service.FileService
import org.vng.filetransferserver.service.StreamingService
import org.vng.filetransferserver.util.FileUtils
import mu.KotlinLogging
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import jakarta.servlet.http.HttpServletRequest
import java.io.File

private val logger = KotlinLogging.logger {}

@RestController
@RequestMapping("/api")
class MediaController(
    private val fileService: FileService,
    private val streamingService: StreamingService
) {

    // File Management Endpoints
    @PostMapping("/files/upload")
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(value = "path", required = false) path: String?
    ): ResponseEntity<FileUploadResponseDTO> {
        logger.info { "Uploading file: ${file.originalFilename}" }
        val response = fileService.uploadFile(file, path)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/files")
    fun listFiles(): ResponseEntity<List<MediaInfoDTO>> {
        val files = fileService.getAllMediaFiles()
        return ResponseEntity.ok(files)
    }

    // Fixed: Use path variable with proper pattern
    @GetMapping("/files/{filename:.+}")
    fun downloadFile(@PathVariable filename: String): ResponseEntity<Resource> {
        logger.info { "Downloading file: $filename" }
        val resource = fileService.downloadFile(filename)
        val contentType = FileUtils.detectContentType(filename)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${File(filename).name}\"")
            .body(resource)
    }

    @DeleteMapping("/files/{filename:.+}")
    fun deleteFile(@PathVariable filename: String): ResponseEntity<Map<String, Any>> {
        fileService.deleteFile(filename)
        return ResponseEntity.ok(mapOf("message" to "File deleted successfully"))
    }

    @GetMapping("/files/{filename:.+}/metadata")
    fun getFileMetadata(@PathVariable filename: String): ResponseEntity<MediaInfoDTO> {
        val metadata = fileService.getFileMetadata(filename)
        return ResponseEntity.ok(metadata)
    }

    @PutMapping("/files/move")
    fun moveFile(
        @RequestParam source: String,
        @RequestParam destination: String
    ): ResponseEntity<Map<String, Any>> {
        fileService.moveFile(source, destination)
        return ResponseEntity.ok(mapOf("message" to "File moved successfully"))
    }

    @PutMapping("/files/copy")
    fun copyFile(
        @RequestParam source: String,
        @RequestParam destination: String
    ): ResponseEntity<Map<String, Any>> {
        fileService.copyFile(source, destination)
        return ResponseEntity.ok(mapOf("message" to "File copied successfully"))
    }

    @PutMapping("/files/rename")
    fun renameFile(
        @RequestParam oldName: String,
        @RequestParam newName: String
    ): ResponseEntity<Map<String, Any>> {
        fileService.renameFile(oldName, newName)
        return ResponseEntity.ok(mapOf("message" to "File renamed successfully"))
    }

    @DeleteMapping("/files/batch")
    fun batchDelete(@RequestBody filenames: List<String>): ResponseEntity<BatchOperationResponseDTO> {
        val response = fileService.batchDelete(filenames)
        return ResponseEntity.ok(response)
    }

    // Media Streaming Endpoints - Fixed patterns
    @GetMapping("/stream/{filename:.+}")
    fun streamMedia(
        @PathVariable filename: String,
        request: HttpServletRequest
    ): ResponseEntity<Resource> {
        return streamingService.streamMedia(filename, request)
    }

    @GetMapping("/media")
    fun listMedia(): ResponseEntity<List<MediaInfoDTO>> {
        val media = fileService.getAllMediaFiles()
        return ResponseEntity.ok(media)
    }

    @GetMapping("/media/{filename:.+}/info")
    fun getMediaInfo(@PathVariable filename: String): ResponseEntity<MediaInfoDTO> {
        val info = streamingService.getMediaInfo(filename)
        return ResponseEntity.ok(info)
    }

    @GetMapping("/preview/{filename:.+}")
    fun previewFile(@PathVariable filename: String): ResponseEntity<Resource> {
        val resource = streamingService.getPreview(filename)
        if (resource == null) {
            return ResponseEntity.notFound().build()
        }

        val contentType = FileUtils.detectContentType(filename)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(resource)
    }

    @GetMapping("/thumbnail/{filename:.+}")
    fun getThumbnail(@PathVariable filename: String): ResponseEntity<Resource> {
        val resource = streamingService.getThumbnail(filename)
        if (resource == null) {
            return ResponseEntity.notFound().build()
        }

        val contentType = FileUtils.detectContentType(filename)
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, contentType)
            .body(resource)
    }

    // Folder Management Endpoints - Fixed patterns
    @GetMapping("/folders/contents")
    fun getFolderContents(
        @RequestParam(required = false) path: String?
    ): ResponseEntity<FolderContentsDTO> {
        val contents = fileService.getFolderContents(path)
        return ResponseEntity.ok(contents)
    }

    @PostMapping("/folders")
    fun createFolder(@RequestParam path: String): ResponseEntity<Map<String, Any>> {
        fileService.createFolder(path)
        return ResponseEntity.ok(mapOf("message" to "Folder created successfully"))
    }

    @DeleteMapping("/folders/{path:.+}")
    fun deleteFolder(@PathVariable path: String): ResponseEntity<Map<String, Any>> {
        fileService.deleteFolder(path)
        return ResponseEntity.ok(mapOf("message" to "Folder deleted successfully"))
    }

    @PutMapping("/folders/move")
    fun moveFolder(
        @RequestParam source: String,
        @RequestParam destination: String
    ): ResponseEntity<Map<String, Any>> {
        fileService.moveFolder(source, destination)
        return ResponseEntity.ok(mapOf("message" to "Folder moved successfully"))
    }

    // Search Endpoints
    @GetMapping("/search")
    fun searchFiles(
        @RequestParam query: String,
        @RequestParam(required = false) folder: String?
    ): ResponseEntity<SearchResultDTO> {
        val results = fileService.searchFiles(query, folder)
        return ResponseEntity.ok(results)
    }

    // System Endpoints
    @GetMapping("/system/info")
    fun getSystemInfo(): ResponseEntity<FileSystemInfoDTO> {
        val info = fileService.getSystemInfo()
        return ResponseEntity.ok(info)
    }

    @GetMapping("/folders/tree")
    fun getFolderTree(): ResponseEntity<List<FolderInfoDTO>> {
        val tree = fileService.getFileTree()
        return ResponseEntity.ok(tree)
    }

    // Batch Operations
    @PostMapping("/batch")
    fun batchOperation(@RequestBody batchOp: BatchOperationDTO): ResponseEntity<BatchOperationResponseDTO> {
        val response = when (batchOp.operation.uppercase()) {
            "DELETE" -> fileService.batchDelete(batchOp.sources)
            "MOVE" -> fileService.batchMove(batchOp.sources, batchOp.destination ?: "")
            "COPY" -> fileService.batchCopy(batchOp.sources, batchOp.destination ?: "")
            else -> BatchOperationResponseDTO(0, batchOp.sources.size,
                batchOp.sources.map { BatchOperationResult(it, false, "Unknown operation") },
                "Batch operation failed")
        }
        return ResponseEntity.ok(response)
    }
}