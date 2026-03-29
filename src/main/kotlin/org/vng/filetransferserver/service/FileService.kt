package org.vng.filetransferserver.service

import org.vng.filetransferserver.dto.*
import org.springframework.core.io.Resource
import org.springframework.web.multipart.MultipartFile

interface FileService {
    fun uploadFile(file: MultipartFile, subPath: String?): FileUploadResponseDTO
    fun downloadFile(filename: String): Resource
    fun getFileMetadata(filename: String): MediaInfoDTO
    fun deleteFile(filename: String): Boolean
    fun moveFile(source: String, destination: String): Boolean
    fun copyFile(source: String, destination: String): Boolean
    fun renameFile(oldName: String, newName: String): Boolean
    fun batchDelete(filenames: List<String>): BatchOperationResponseDTO
    fun batchMove(sources: List<String>, destination: String): BatchOperationResponseDTO
    fun batchCopy(sources: List<String>, destination: String): BatchOperationResponseDTO
    fun getFolderContents(path: String?): FolderContentsDTO
    fun createFolder(path: String): Boolean
    fun deleteFolder(path: String): Boolean
    fun moveFolder(source: String, destination: String): Boolean
    fun searchFiles(query: String, folder: String?): SearchResultDTO
    fun getAllMediaFiles(): List<MediaInfoDTO>
    fun getSystemInfo(): FileSystemInfoDTO
    fun getFileTree(): List<FolderInfoDTO>
}