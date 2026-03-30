package org.vng.filetransferserver.dto

data class SearchResultDTO(
    val query: String,
    val results: List<MediaInfoDTO>,
    val count: Int
)