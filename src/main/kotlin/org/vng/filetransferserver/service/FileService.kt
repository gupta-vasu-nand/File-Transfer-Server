package org.vng.filetransferserver.service

import org.springframework.web.multipart.MultipartFile
import org.vng.filetransferserver.dto.*
import java.nio.file.Path

interface FileService {
    fun uploadFile(file: MultipartFile, folderPath: String?): FileUploadResponseDTO
    fun downloadFile(path: String): Path
    fun deleteFile(path: String): Boolean
    fun getFileMetadata(path: String): FileMetadataDTO
    fun listAllFiles(): List<MediaInfoDTO>
    fun moveFile(sourcePath: String, destinationPath: String): Boolean
    fun copyFile(sourcePath: String, destinationPath: String): Boolean
    fun renameFile(path: String, newName: String): Boolean
    fun getFolderContents(folderPath: String?): FolderContentsDTO
    fun createFolder(folderPath: String): Boolean
    fun deleteFolder(folderPath: String): Boolean
    fun moveFolder(sourcePath: String, destinationPath: String): Boolean
    fun getFolderTree(): List<FolderInfoDTO>
    fun searchFiles(query: String, folderPath: String?): SearchResultDTO
    fun batchOperation(request: BatchOperationDTO): BatchOperationResponseDTO
    fun getSystemInfo(): FileSystemInfoDTO
    fun getFilePreview(path: String): Path
    fun getThumbnail(path: String): Path?
}