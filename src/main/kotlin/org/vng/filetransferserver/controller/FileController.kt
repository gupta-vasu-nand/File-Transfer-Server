package org.vng.filetransferserver.controller

import org.vng.filetransferserver.dto.FileMetadataDTO
import org.vng.filetransferserver.dto.FileUploadResponseDTO
import org.vng.filetransferserver.service.FileService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestController
@RequestMapping("/api/files")
@Tag(name = "File Transfer", description = "File upload and download operations")
class FileController(
    private val fileService: FileService
) {

    private val logger = LoggerFactory.getLogger(FileController::class.java)

    @PostMapping("/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload a file", description = "Upload a file to the server")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            ApiResponse(responseCode = "400", description = "Invalid file or file type"),
            ApiResponse(responseCode = "413", description = "File size exceeds limit"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun uploadFile(
        @Parameter(description = "File to upload", required = true)
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<FileUploadResponseDTO> {

        logger.info("Received upload request for file: ${file.originalFilename}, size: ${file.size} bytes")

        val filename = fileService.uploadFile(file)

        val downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/files/{filename}")
            .buildAndExpand(filename)
            .toUriString()

        val response = FileUploadResponseDTO(
            filename = filename,
            originalFilename = file.originalFilename ?: filename,
            size = file.size,
            contentType = file.contentType ?: "application/octet-stream",
            downloadUrl = downloadUrl
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping
    @Operation(summary = "List all files", description = "Get list of all available files")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Files listed successfully"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun listFiles(): ResponseEntity<List<FileMetadataDTO>> {
        logger.info("Received request to list all files")
        val files = fileService.listFiles()
        return ResponseEntity.ok(files)
    }

    @GetMapping("/{filename}")
    @Operation(summary = "Download a file", description = "Download a file by its filename")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "File downloaded successfully", content = [Content(schema = Schema(type = "string", format = "binary"))]),
            ApiResponse(responseCode = "404", description = "File not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun downloadFile(
        @Parameter(description = "Filename to download", required = true)
        @PathVariable("filename") filename: String
    ): ResponseEntity<Resource> {

        logger.info("Received download request for file: $filename")

        val resource = fileService.downloadFile(filename)
        val metadata = fileService.getFileMetadata(filename)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${metadata.filename}\"")
            .contentType(MediaType.parseMediaType(metadata.contentType ?: "application/octet-stream"))
            .contentLength(metadata.size)
            .body(resource)
    }

    @DeleteMapping("/{filename}")
    @Operation(summary = "Delete a file", description = "Delete a file by its filename")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "File deleted successfully"),
            ApiResponse(responseCode = "404", description = "File not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun deleteFile(
        @Parameter(description = "Filename to delete", required = true)
        @PathVariable("filename") filename: String
    ): ResponseEntity<Map<String, String>> {

        logger.info("Received delete request for file: $filename")

        val deleted = fileService.deleteFile(filename)

        return ResponseEntity.ok(
            mapOf(
                "message" to "File deleted successfully",
                "filename" to filename,
                "deleted" to deleted.toString()
            )
        )
    }

    @GetMapping("/{filename}/metadata")
    @Operation(summary = "Get file metadata", description = "Get metadata for a specific file")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Metadata retrieved successfully"),
            ApiResponse(responseCode = "404", description = "File not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun getFileMetadata(
        @Parameter(description = "Filename to get metadata for", required = true)
        @PathVariable("filename") filename: String
    ): ResponseEntity<FileMetadataDTO> {

        logger.info("Received metadata request for file: $filename")

        val metadata = fileService.getFileMetadata(filename)
        return ResponseEntity.ok(metadata)
    }
}