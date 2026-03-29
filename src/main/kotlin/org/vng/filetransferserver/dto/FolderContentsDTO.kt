package org.vng.filetransferserver.dto


data class FolderContentsDTO(
    val currentPath: String,
    val parentPath: String?,
    val folders: List<FolderInfoDTO>,
    val files: List<MediaInfoDTO>,
    val totalSize: Long,
    val totalSizeFormatted: String,
    val fileCount: Int,
    val folderCount: Int
)