package org.vng.filetransferserver.dto

data class BatchOperationDTO(
    val operation: String, // DELETE, MOVE, COPY
    val sources: List<String>,
    val destination: String? = null
)