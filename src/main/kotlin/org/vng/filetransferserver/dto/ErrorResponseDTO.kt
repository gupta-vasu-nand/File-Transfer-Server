package org.vng.filetransferserver.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ErrorResponseDTO(
    @JsonProperty("timestamp")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("status")
    val status: Int,

    @JsonProperty("error")
    val error: String,

    @JsonProperty("message")
    val message: String,

    @JsonProperty("path")
    val path: String?
)