package org.vng.filetransferserver.dto

import java.time.LocalDateTime

data class ErrorResponseDTO(
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String?
)