package org.vng.filetransferserver.service.impl

import org.vng.filetransferserver.config.StorageConfig
import org.vng.filetransferserver.dto.FileMetadataDTO
import org.vng.filetransferserver.exception.FileNotFoundException
import org.vng.filetransferserver.exception.FileSizeLimitExceededException
import org.vng.filetransferserver.exception.FileStorageException
import org.vng.filetransferserver.exception.InvalidFileTypeException
import org.vng.filetransferserver.service.FileService
import org.vng.filetransferserver.util.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

@Service
class FileServiceImpl(
    private val storageConfig: StorageConfig,
    private val fileUtils: FileUtils
) : FileService {

    private val logger = LoggerFactory.getLogger(FileServiceImpl::class.java)

    override fun uploadFile(file: MultipartFile): String {
        validateFile(file)

        val originalFilename = fileUtils.sanitizeFilename(file.originalFilename ?: file.name)
        val filename = fileUtils.generateUniqueFilename(originalFilename)
        val targetLocation = storageConfig.getStoragePath().resolve(filename)

        logger.info("Uploading file: $originalFilename -> $filename (${file.size} bytes)")

        try {
            // Use streaming to handle large files
            file.inputStream.use { inputStream ->
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING)
            }

            logger.info("File uploaded successfully: $filename")
            return filename

        } catch (e: IOException) {
            logger.error("Failed to upload file: ${e.message}", e)
            throw FileStorageException("Failed to upload file: ${e.message}", e)
        }
    }

    override fun downloadFile(filename: String): Resource {
        val sanitizedFilename = fileUtils.sanitizeFilename(filename)
        val filePath = storageConfig.getStoragePath().resolve(sanitizedFilename)

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            logger.warn("File not found: $sanitizedFilename")
            throw FileNotFoundException("File not found: $filename")
        }

        logger.info("Downloading file: $sanitizedFilename")

        return try {
            val inputStream: InputStream = Files.newInputStream(filePath)
            InputStreamResource(inputStream)
        } catch (e: IOException) {
            logger.error("Failed to download file: ${e.message}", e)
            throw FileStorageException("Failed to download file: ${e.message}", e)
        }
    }

    override fun getFileMetadata(filename: String): FileMetadataDTO {
        val sanitizedFilename = fileUtils.sanitizeFilename(filename)
        val filePath = storageConfig.getStoragePath().resolve(sanitizedFilename)

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw FileNotFoundException("File not found: $filename")
        }

        return try {
            val contentType = Files.probeContentType(filePath)
            val attributes: BasicFileAttributes = Files.readAttributes(filePath, BasicFileAttributes::class.java)
            val size = attributes.size()
            val lastModified = attributes.lastModifiedTime().toInstant()

            FileMetadataDTO(
                filename = sanitizedFilename,
                size = size,
                sizeFormatted = fileUtils.formatFileSize(size),
                contentType = contentType,
                lastModified = lastModified,
                downloadUrl = buildDownloadUrl(sanitizedFilename)
            )
        } catch (e: IOException) {
            logger.error("Failed to read file metadata: ${e.message}", e)
            throw FileStorageException("Failed to read file metadata: ${e.message}", e)
        }
    }

    override fun listFiles(): List<FileMetadataDTO> {
        val storagePath = storageConfig.getStoragePath()

        return try {
            val fileList = mutableListOf<FileMetadataDTO>()

            Files.list(storagePath).use { stream ->
                stream.filter { path -> Files.isRegularFile(path) }
                    .forEach { path ->
                        try {
                            val filename = path.fileName.toString()
                            val metadata = getFileMetadata(filename)
                            fileList.add(metadata)
                        } catch (e: Exception) {
                            logger.warn("Failed to get metadata for file: ${path.fileName}", e)
                        }
                    }
            }

            // Sort using Comparator
            fileList.sortedWith(Comparator { a, b ->
                b.lastModified.compareTo(a.lastModified)
            })
        } catch (e: IOException) {
            logger.error("Failed to list files: ${e.message}", e)
            throw FileStorageException("Failed to list files: ${e.message}", e)
        }
    }

    override fun deleteFile(filename: String): Boolean {
        val sanitizedFilename = fileUtils.sanitizeFilename(filename)
        val filePath = storageConfig.getStoragePath().resolve(sanitizedFilename)

        if (!Files.exists(filePath)) {
            logger.warn("File not found for deletion: $sanitizedFilename")
            throw FileNotFoundException("File not found: $filename")
        }

        return try {
            val deleted = Files.deleteIfExists(filePath)
            if (deleted) {
                logger.info("File deleted successfully: $sanitizedFilename")
            }
            deleted
        } catch (e: IOException) {
            logger.error("Failed to delete file: ${e.message}", e)
            throw FileStorageException("Failed to delete file: ${e.message}", e)
        }
    }

    override fun fileExists(filename: String): Boolean {
        val sanitizedFilename = fileUtils.sanitizeFilename(filename)
        val filePath = storageConfig.getStoragePath().resolve(sanitizedFilename)
        return Files.exists(filePath) && Files.isRegularFile(filePath)
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw InvalidFileTypeException("Cannot upload empty file")
        }

        // Validate file size
        val maxSize = storageConfig.getMaxFileSize()
        if (file.size > maxSize) {
            val maxSizeMB = maxSize / (1024 * 1024)
            val fileSizeMB = file.size / (1024 * 1024)
            throw FileSizeLimitExceededException(
                "File size (${fileSizeMB}MB) exceeds maximum allowed size (${maxSizeMB}MB)"
            )
        }

        // Validate file extension if restrictions are configured
        val allowedExtensions = storageConfig.getAllowedExtensions()
        if (allowedExtensions.isNotEmpty() && allowedExtensions.first() != "*") {
            val originalFilename = file.originalFilename ?: file.name
            val extension = fileUtils.getFileExtension(originalFilename)

            if (!allowedExtensions.contains(extension.lowercase())) {
                throw InvalidFileTypeException(
                    "File type .$extension is not allowed. Allowed types: ${allowedExtensions.joinToString(", ")}"
                )
            }
        }
    }

    private fun buildDownloadUrl(filename: String): String {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/files/{filename}")
            .buildAndExpand(filename)
            .toUriString()
    }
}