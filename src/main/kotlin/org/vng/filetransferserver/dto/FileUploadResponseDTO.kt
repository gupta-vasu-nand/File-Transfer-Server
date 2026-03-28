package org.vng.filetransferserver.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class FileUploadResponseDTO(
    @JsonProperty("filename")
    val filename: String,

    @JsonProperty("originalFilename")
    val originalFilename: String,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("contentType")
    val contentType: String,

    @JsonProperty("uploadTime")
    val uploadTime: LocalDateTime = LocalDateTime.now(),

    @JsonProperty("downloadUrl")
    val downloadUrl: String,

    @JsonProperty("message")
    val message: String = "File uploaded successfully"
)