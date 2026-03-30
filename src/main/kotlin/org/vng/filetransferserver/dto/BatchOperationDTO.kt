package org.vng.filetransferserver.dto

data class BatchOperationDTO(
    val operation: String, // "delete", "move", "copy"
    val files: List<String>,
    val destination: String? = null
)