package org.vng.filetransferserver.service

import org.springframework.core.io.Resource

interface StreamingService {
    fun streamMedia(filename: String, rangeHeader: String?): StreamResponse
    fun getMediaMetadata(filename: String): MediaMetadata
    fun getMediaInfo(filename: String): MediaInfoDTO
}

data class StreamResponse(
    val resource: Resource,
    val contentType: String,
    val contentLength: Long,
    val rangeStart: Long,
    val rangeEnd: Long,
    val totalSize: Long
)

data class MediaMetadata(
    val filename: String,
    val contentType: String,
    val size: Long,
    val supportsRange: Boolean = true
)

data class MediaInfoDTO(
    val filename: String,
    val title: String,
    val duration: Long?,
    val size: Long,
    val sizeFormatted: String,
    val contentType: String,
    val bitrate: Long?,
    val resolution: String?,
    val thumbnail: String?,
    val videoCodec: String? = null,      // NEW
    val audioCodec: String? = null,      // NEW
    val audioChannels: Int? = null,      // NEW
    val hasAudio: Boolean = true         // NEW
)