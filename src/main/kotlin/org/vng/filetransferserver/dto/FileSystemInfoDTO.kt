package org.vng.filetransferserver.dto

data class FileSystemInfoDTO(
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long,
    val totalSpaceFormatted: String,
    val freeSpaceFormatted: String,
    val usedSpaceFormatted: String,
    val usagePercentage: Int
)