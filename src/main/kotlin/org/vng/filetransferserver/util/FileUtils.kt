package org.vng.filetransferserver.util

import org.springframework.stereotype.Component
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Component
class FileUtils {

    private val sizeFormat = DecimalFormat("#,##0.00")

    /**
     * Sanitize filename to prevent path traversal attacks
     */
    fun sanitizeFilename(filename: String): String {
        // Remove any path traversal attempts
        var sanitized = filename
            .replace("../", "")
            .replace("..\\", "")
            .replace("/", "_")
            .replace("\\", "_")
            .trim()

        // If filename becomes empty after sanitization, generate a default one
        if (sanitized.isBlank()) {
            sanitized = "file_${System.currentTimeMillis()}"
        }

        return sanitized
    }

    /**
     * Generate a unique filename to prevent collisions
     */
    fun generateUniqueFilename(originalFilename: String): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val uniqueId = UUID.randomUUID().toString().take(8)
        val extension = getFileExtension(originalFilename)

        return if (extension.isNotEmpty()) {
            "${originalFilename.substringBeforeLast(".")}_${timestamp}_${uniqueId}.$extension"
        } else {
            "${originalFilename}_${timestamp}_$uniqueId"
        }
    }

    /**
     * Get file extension from filename
     */
    fun getFileExtension(filename: String): String {
        val lastDotIndex = filename.lastIndexOf(".")
        return if (lastDotIndex > 0 && lastDotIndex < filename.length - 1) {
            filename.substring(lastDotIndex + 1)
        } else {
            ""
        }
    }

    /**
     * Format file size to human-readable format
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${sizeFormat.format(bytes / 1024.0)} KB"
            bytes < 1024 * 1024 * 1024 -> "${sizeFormat.format(bytes / (1024.0 * 1024.0))} MB"
            else -> "${sizeFormat.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }

    /**
     * Validate if the file type is allowed
     */
    fun isAllowedFileType(filename: String, allowedExtensions: List<String>): Boolean {
        if (allowedExtensions.isEmpty() || allowedExtensions.first() == "*") {
            return true
        }

        val extension = getFileExtension(filename).lowercase()
        return allowedExtensions.contains(extension)
    }
}