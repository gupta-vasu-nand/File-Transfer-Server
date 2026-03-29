package org.vng.filetransferserver.dto

import java.time.LocalDateTime

data class FileUploadResponseDTO(
    val filename: String,
    val originalFilename: String,
    val size: Long,
    val contentType: String,
    val uploadTime: LocalDateTime,
    val downloadUrl: String,
    val message: String
)