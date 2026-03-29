package org.vng.filetransferserver.util

import java.util.*

object FileUtils {

    fun sanitizeFilename(filename: String): String {
        var sanitized = filename
            .replace("../", "")
            .replace("..\\", "")
            .replace("/", "_")
            .replace("\\", "_")
//            .replace("\0", "")

        if (sanitized.isEmpty() || sanitized == "." || sanitized == "..") {
            sanitized = "file_${System.currentTimeMillis()}"
        }

        return sanitized
    }

    fun generateUniqueFilename(originalFilename: String): String {
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().substring(0, 8)
        val extension = getFileExtension(originalFilename)
        val nameWithoutExt = originalFilename.substringBeforeLast(".")

        return "${sanitizeFilename(nameWithoutExt)}_${timestamp}_${uuid}${extension}"
    }

    fun getFileExtension(filename: String): String {
        val lastDot = filename.lastIndexOf('.')
        return if (lastDot > 0) filename.substring(lastDot).lowercase() else ""
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun detectContentType(filename: String): String {
        val extension = getFileExtension(filename)
        return when (extension) {
            ".mp4", ".webm", ".mkv", ".avi", ".mov", ".mpeg" -> "video/${extension.substring(1)}"
            ".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a" -> "audio/${extension.substring(1)}"
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp" -> "image/${extension.substring(1)}"
            ".pdf" -> "application/pdf"
            ".txt" -> "text/plain"
            ".html", ".htm" -> "text/html"
            ".css" -> "text/css"
            ".js" -> "application/javascript"
            ".json" -> "application/json"
            ".xml" -> "application/xml"
            ".zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }

    fun detectResolution(filename: String): String? {
        val lowerName = filename.lowercase()
        return when {
            lowerName.contains("1080p") || lowerName.contains("1080") -> "1920x1080"
            lowerName.contains("720p") || lowerName.contains("720") -> "1280x720"
            lowerName.contains("480p") || lowerName.contains("480") -> "854x480"
            lowerName.contains("360p") || lowerName.contains("360") -> "640x360"
            lowerName.contains("4k") || lowerName.contains("2160p") -> "3840x2160"
            else -> null
        }
    }

    fun getFileTypeCategory(filename: String): String {
        val extension = getFileExtension(filename)
        return when (extension) {
            in listOf(".mp4", ".webm", ".mkv", ".avi", ".mov", ".mpeg", ".flv") -> "video"
            in listOf(".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a") -> "audio"
            in listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp") -> "image"
            in listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt") -> "document"
            in listOf(".js", ".html", ".css", ".json", ".xml", ".py", ".java", ".kt", ".cpp", ".c") -> "code"
            in listOf(".zip", ".rar", ".7z", ".tar", ".gz") -> "archive"
            else -> "other"
        }
    }

    fun extractCodeLanguage(filename: String): String {
        val extension = getFileExtension(filename)
        return when (extension) {
            ".js" -> "javascript"
            ".ts" -> "typescript"
            ".html", ".htm" -> "html"
            ".css" -> "css"
            ".json" -> "json"
            ".xml" -> "xml"
            ".py" -> "python"
            ".java" -> "java"
            ".kt" -> "kotlin"
            ".cpp", ".c" -> "cpp"
            ".php" -> "php"
            ".rb" -> "ruby"
            ".go" -> "go"
            ".rs" -> "rust"
            ".swift" -> "swift"
            else -> "text"
        }
    }

    fun getFileIcon(filename: String): String {
        val category = getFileTypeCategory(filename)
        return when (category) {
            "video" -> "video"
            "audio" -> "music"
            "image" -> "image"
            "document" -> "file-text"
            "code" -> "code"
            "archive" -> "archive"
            else -> "file"
        }
    }
}