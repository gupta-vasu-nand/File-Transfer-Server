package org.vng.filetransferserver.util

import java.text.DecimalFormat

object FileUtils {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }

    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[^a-zA-Z0-9.\\- _]"), "")
            .replace(Regex("\\.{2,}"), ".")
            .trim()
            .takeIf { it.isNotEmpty() } ?: "unnamed"
    }

    fun normalizePath(path: String): String {
        return path
            .replace("\\", "/")
            .replace(Regex("/+"), "/")
            .trim('/')
    }

    fun getFileExtension(filename: String): String {
        val lastIndex = filename.lastIndexOf('.')
        return if (lastIndex == -1) "" else filename.substring(lastIndex + 1).lowercase()
    }

    fun isVideoFile(contentType: String, filename: String): Boolean {
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "webm", "m4v", "mpeg", "mpg")
        return contentType.startsWith("video/") || videoExtensions.contains(getFileExtension(filename))
    }

    fun isAudioFile(contentType: String, filename: String): Boolean {
        val audioExtensions = listOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus", "wma")
        return contentType.startsWith("audio/") || audioExtensions.contains(getFileExtension(filename))
    }

    fun isImageFile(contentType: String, filename: String): Boolean {
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg")
        return contentType.startsWith("image/") || imageExtensions.contains(getFileExtension(filename))
    }

    fun isDocumentFile(contentType: String, filename: String): Boolean {
        val docExtensions = listOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
        return contentType in listOf("application/pdf", "text/plain", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                docExtensions.contains(getFileExtension(filename))
    }

    fun isCodeFile(filename: String): Boolean {
        val codeExtensions = listOf("js", "html", "css", "json", "xml", "py", "java", "kt",
            "cpp", "c", "php", "rb", "go", "ts", "tsx", "jsx", "sql", "sh", "yaml", "yml", "md")
        return codeExtensions.contains(getFileExtension(filename))
    }
}