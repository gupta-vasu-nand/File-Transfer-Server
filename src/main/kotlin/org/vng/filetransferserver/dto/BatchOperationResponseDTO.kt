package org.vng.filetransferserver.dto

data class BatchOperationResponseDTO(
    val successCount: Int,
    val failureCount: Int,
    val results: List<BatchOperationResult>,
    val message: String
)

data class BatchOperationResult(
    val source: String,
    val success: Boolean,
    val message: String? = null
)