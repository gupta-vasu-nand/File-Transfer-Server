package org.vng.filetransferserver.dto

import java.time.Instant

data class FolderInfoDTO(
    val name: String,
    val path: String,
    val fileCount: Int,
    val totalSize: Long,
    val totalSizeFormatted: String,
    val lastModified: Instant,
    val children: List<FolderInfoDTO>? = null
)