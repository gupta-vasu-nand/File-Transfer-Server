package org.vng.filetransferserver.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class FileMetadataDTO(
    @JsonProperty("filename")
    val filename: String,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("sizeFormatted")
    val sizeFormatted: String,

    @JsonProperty("contentType")
    val contentType: String?,

    @JsonProperty("lastModified")
    val lastModified: Instant,

    @JsonProperty("downloadUrl")
    val downloadUrl: String
)