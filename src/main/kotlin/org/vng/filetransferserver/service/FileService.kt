package org.vng.filetransferserver.service

import org.vng.filetransferserver.dto.FileMetadataDTO
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

interface FileService {

    /**
     * Upload a file to the storage
     * @param file The multipart file to upload
     * @return The filename of the stored file
     */
    fun uploadFile(file: MultipartFile): String

    /**
     * Download a file by its filename
     * @param filename The name of the file to download
     * @return Resource representing the file
     */
    fun downloadFile(filename: String): Resource

    /**
     * Get metadata for a specific file
     * @param filename The name of the file
     * @return File metadata DTO
     */
    fun getFileMetadata(filename: String): FileMetadataDTO

    /**
     * List all available files
     * @return List of file metadata DTOs
     */
    fun listFiles(): List<FileMetadataDTO>

    /**
     * Delete a file by its filename
     * @param filename The name of the file to delete
     * @return true if deleted successfully
     */
    fun deleteFile(filename: String): Boolean

    /**
     * Check if a file exists
     * @param filename The name of the file
     * @return true if file exists
     */
    fun fileExists(filename: String): Boolean
}