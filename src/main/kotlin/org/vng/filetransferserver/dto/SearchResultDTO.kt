package org.vng.filetransferserver.dto

data class SearchResultDTO(
    val query: String,
    val totalResults: Int,
    val results: List<MediaInfoDTO>,
    val searchTime: Long
)