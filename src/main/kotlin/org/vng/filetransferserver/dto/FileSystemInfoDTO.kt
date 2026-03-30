package org.vng.filetransferserver.dto

data class FileSystemInfoDTO(
    val totalSpace: Long,
    val totalSpaceFormatted: String,
    val freeSpace: Long,
    val freeSpaceFormatted: String,
    val usedSpace: Long,
    val usedSpaceFormatted: String,
    val usagePercentage: Int
)