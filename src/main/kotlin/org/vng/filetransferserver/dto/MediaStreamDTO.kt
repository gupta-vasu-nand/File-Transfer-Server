package org.vng.filetransferserver.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class MediaStreamDTO(
    @JsonProperty("filename")
    val filename: String,

    @JsonProperty("title")
    val title: String,

    @JsonProperty("duration")
    val duration: Long? = null,

    @JsonProperty("size")
    val size: Long,

    @JsonProperty("sizeFormatted")
    val sizeFormatted: String,

    @JsonProperty("contentType")
    val contentType: String,

    @JsonProperty("thumbnail")
    val thumbnail: String? = null,

    @JsonProperty("streamUrl")
    val streamUrl: String,

    @JsonProperty("downloadUrl")
    val downloadUrl: String
)

data class StreamRangeDTO(
    @JsonProperty("start")
    val start: Long,

    @JsonProperty("end")
    val end: Long,

    @JsonProperty("totalSize")
    val totalSize: Long,

    @JsonProperty("contentType")
    val contentType: String
)