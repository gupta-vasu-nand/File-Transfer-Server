package org.vng.filetransferserver.dto

import java.time.Instant

data class MediaInfoDTO(
    val filename: String,
    val title: String,
    val path: String,
    val parentFolder: String,
    val folderPath: String,
    val size: Long,
    val sizeFormatted: String,
    val contentType: String,
    val lastModified: Instant,
    val duration: Long? = null,
    val bitrate: Long? = null,
    val resolution: String? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val hasAudio: Boolean = true,
    val streamUrl: String,
    val downloadUrl: String,
    val isDirectory: Boolean = false
)