package org.vng.filetransferserver.dto

data class BatchOperationResponseDTO(
    val success: List<String>,
    val failed: List<Pair<String, String>>,
    val totalSuccess: Int,
    val totalFailed: Int
)