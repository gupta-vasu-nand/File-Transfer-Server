package org.vng.filetransferserver.dto

import java.time.Instant

data class FileMetadataDTO(
    val filename: String,
    val path: String,
    val size: Long,
    val sizeFormatted: String,
    val contentType: String,
    val lastModified: Instant,
    val isDirectory: Boolean,
    val parentPath: String?
)