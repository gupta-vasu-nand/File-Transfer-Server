package org.vng.filetransferserver.service.impl

import org.vng.filetransferserver.dto.*
import org.vng.filetransferserver.exception.*
import org.vng.filetransferserver.service.FileService
import org.vng.filetransferserver.util.FileUtils
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.*
import java.time.Instant
import java.time.LocalDateTime
import kotlin.io.path.*
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

@Service
class FileServiceImpl(
    @Qualifier("storageDirectory") private val storagePath: Path
) : FileService {

    @Value("\${file.max-size:1073741824}")
    private lateinit var maxFileSize: String

    override fun uploadFile(file: MultipartFile, subPath: String?): FileUploadResponseDTO {
        val originalFilename = file.originalFilename ?: file.name
        val sanitizedFilename = FileUtils.sanitizeFilename(originalFilename)
        val uniqueFilename = FileUtils.generateUniqueFilename(sanitizedFilename)

        // Determine target directory based on file type
        val typeFolder = getFileTypeFolder(originalFilename)

        // Build the target path
        val targetDir = if (!subPath.isNullOrBlank()) {
            // If subPath is provided (user selected a folder), use that
            storagePath.resolve(subPath).normalize()
        } else {
            // Otherwise use the type-based folder
            storagePath.resolve(typeFolder)
        }

        if (!targetDir.startsWith(storagePath)) {
            throw InvalidFileTypeException("Invalid path traversal attempt")
        }

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir)
            logger.info { "Created directory: ${targetDir.toAbsolutePath()}" }
        }

        val targetPath = targetDir.resolve(uniqueFilename)

        if (Files.exists(targetPath)) {
            throw FileStorageException("File already exists: $uniqueFilename")
        }

        try {
            file.inputStream.use { input ->
                Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }

            logger.info { "Uploaded file: ${targetPath.toAbsolutePath()} (${file.size} bytes)" }

            // Get the relative path from storage root
            val relativePath = storagePath.relativize(targetPath).toString()

            return FileUploadResponseDTO(
                filename = uniqueFilename,
                originalFilename = originalFilename,
                size = file.size,
                contentType = file.contentType ?: "application/octet-stream",
                uploadTime = LocalDateTime.now(),
                downloadUrl = "/api/files/$relativePath",
                message = "File uploaded successfully to ${targetDir.fileName}"
            )
        } catch (e: Exception) {
            logger.error(e) { "Failed to upload file" }
            throw FileStorageException("Failed to upload file: ${e.message}", e)
        }
    }

    private fun getFileTypeFolder(filename: String): String {
        val extension = FileUtils.getFileExtension(filename)
        return when (extension) {
            in listOf(".mp4", ".webm", ".mkv", ".avi", ".mov", ".mpeg", ".flv", ".m4v") -> "Videos"
            in listOf(".mp3", ".wav", ".flac", ".aac", ".ogg", ".m4a") -> "Audio"
            in listOf(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg", ".bmp") -> "Images"
            in listOf(".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx") -> "Documents"
            in listOf(
                ".js",
                ".html",
                ".css",
                ".json",
                ".xml",
                ".py",
                ".java",
                ".kt",
                ".cpp",
                ".c",
                ".php",
                ".rb",
                ".go",
                ".ts",
                ".kt",
                ".kts"
            ) -> "Code"

            else -> "Others"
        }
    }

    override fun downloadFile(filename: String): Resource {
        val file = findFile(filename)
        logger.debug { "Downloading file: ${file.absolutePath}" }
        return FileSystemResource(file)
    }

    override fun getFileMetadata(filename: String): MediaInfoDTO {
        val file = findFile(filename)
        val relativePath = storagePath.relativize(file.toPath()).toString()
        return buildMediaInfoDTO(file, relativePath)
    }

    override fun deleteFile(filename: String): Boolean {
        val file = findFile(filename)
        return try {
            Files.deleteIfExists(file.toPath())
            logger.info { "Deleted file: ${file.absolutePath}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete file" }
            throw FileStorageException("Failed to delete file: ${e.message}", e)
        }
    }

    override fun moveFile(source: String, destination: String): Boolean {
        val sourceFile = findFile(source)
        val destPath = storagePath.resolve(destination).normalize()

        if (!destPath.startsWith(storagePath)) {
            throw InvalidFileTypeException("Invalid destination path")
        }

        val destParent = destPath.parent
        if (destParent != null && !Files.exists(destParent)) {
            Files.createDirectories(destParent)
        }

        return try {
            Files.move(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING)
            logger.info { "Moved file from $source to $destination" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to move file" }
            throw FileStorageException("Failed to move file: ${e.message}", e)
        }
    }

    override fun copyFile(source: String, destination: String): Boolean {
        val sourceFile = findFile(source)
        val destPath = storagePath.resolve(destination).normalize()

        if (!destPath.startsWith(storagePath)) {
            throw InvalidFileTypeException("Invalid destination path")
        }

        val destParent = destPath.parent
        if (destParent != null && !Files.exists(destParent)) {
            Files.createDirectories(destParent)
        }

        return try {
            Files.copy(sourceFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING)
            logger.info { "Copied file from $source to $destination" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to copy file" }
            throw FileStorageException("Failed to copy file: ${e.message}", e)
        }
    }

    override fun renameFile(oldName: String, newName: String): Boolean {
        val oldFile = findFile(oldName)
        val newPath = oldFile.parentFile.toPath().resolve(FileUtils.sanitizeFilename(newName))

        return try {
            Files.move(oldFile.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING)
            logger.info { "Renamed file from $oldName to ${newPath.fileName}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to rename file" }
            throw FileStorageException("Failed to rename file: ${e.message}", e)
        }
    }

    override fun batchDelete(filenames: List<String>): BatchOperationResponseDTO {
        val results = filenames.map { filename ->
            try {
                deleteFile(filename)
                BatchOperationResult(filename, true)
            } catch (e: Exception) {
                BatchOperationResult(filename, false, e.message)
            }
        }

        return BatchOperationResponseDTO(
            successCount = results.count { it.success },
            failureCount = results.count { !it.success },
            results = results,
            message = "Batch delete completed"
        )
    }

    override fun batchMove(sources: List<String>, destination: String): BatchOperationResponseDTO {
        val results = sources.map { source ->
            try {
                val destPath = Paths.get(destination, Paths.get(source).fileName.toString()).toString()
                moveFile(source, destPath)
                BatchOperationResult(source, true)
            } catch (e: Exception) {
                BatchOperationResult(source, false, e.message)
            }
        }

        return BatchOperationResponseDTO(
            successCount = results.count { it.success },
            failureCount = results.count { !it.success },
            results = results,
            message = "Batch move completed"
        )
    }

    override fun batchCopy(sources: List<String>, destination: String): BatchOperationResponseDTO {
        val results = sources.map { source ->
            try {
                val destPath = Paths.get(destination, Paths.get(source).fileName.toString()).toString()
                copyFile(source, destPath)
                BatchOperationResult(source, true)
            } catch (e: Exception) {
                BatchOperationResult(source, false, e.message)
            }
        }

        return BatchOperationResponseDTO(
            successCount = results.count { it.success },
            failureCount = results.count { !it.success },
            results = results,
            message = "Batch copy completed"
        )
    }

    override fun getFolderContents(path: String?): FolderContentsDTO {
        val targetPath = if (path.isNullOrBlank()) {
            storagePath
        } else {
            val resolved = storagePath.resolve(path).normalize()
            if (!resolved.startsWith(storagePath)) {
                throw InvalidFileTypeException("Invalid path")
            }
            resolved
        }

        logger.debug { "Getting contents for: ${targetPath.toAbsolutePath()}" }

        if (!Files.exists(targetPath) || !Files.isDirectory(targetPath)) {
            throw FileNotFoundException("Directory not found: ${targetPath.toAbsolutePath()}")
        }

        val folders = mutableListOf<FolderInfoDTO>()
        val files = mutableListOf<MediaInfoDTO>()
        var totalSize = 0L

        Files.newDirectoryStream(targetPath).use { stream ->
            stream.forEach { entry ->
                if (Files.isDirectory(entry)) {
                    val folderInfo = getFolderInfo(entry)
                    folders.add(folderInfo)
                    totalSize += folderInfo.totalSize
                } else {
                    val relativePath = storagePath.relativize(entry).toString()
                    val mediaInfo = buildMediaInfoDTO(entry.toFile(), relativePath)
                    files.add(mediaInfo)
                    totalSize += mediaInfo.size
                }
            }
        }

        folders.sortBy { it.name.lowercase() }
        files.sortBy { it.filename.lowercase() }

        val parentPath = if (targetPath == storagePath) {
            null
        } else {
            storagePath.relativize(targetPath.parent).toString().ifEmpty { null }
        }

        return FolderContentsDTO(
            currentPath = if (targetPath == storagePath) "" else storagePath.relativize(targetPath).toString(),
            parentPath = parentPath,
            folders = folders,
            files = files,
            totalSize = totalSize,
            totalSizeFormatted = FileUtils.formatFileSize(totalSize),
            fileCount = files.size,
            folderCount = folders.size
        )
    }

    override fun createFolder(path: String): Boolean {
        val targetPath = storagePath.resolve(path).normalize()

        if (!targetPath.startsWith(storagePath)) {
            throw InvalidFileTypeException("Invalid folder path")
        }

        return try {
            Files.createDirectories(targetPath)
            logger.info { "Created folder: ${targetPath.toAbsolutePath()}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to create folder" }
            throw DirectoryOperationException("Failed to create folder: ${e.message}")
        }
    }

    override fun deleteFolder(path: String): Boolean {
        val targetPath = storagePath.resolve(path).normalize()

        if (!targetPath.startsWith(storagePath) || targetPath == storagePath) {
            throw InvalidFileTypeException("Cannot delete root directory")
        }

        return try {
            targetPath.toFile().deleteRecursively()
            logger.info { "Deleted folder: ${targetPath.toAbsolutePath()}" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete folder" }
            throw DirectoryOperationException("Failed to delete folder: ${e.message}")
        }
    }

    override fun moveFolder(source: String, destination: String): Boolean {
        val sourcePath = storagePath.resolve(source).normalize()
        val destPath = storagePath.resolve(destination).normalize()

        if (!sourcePath.startsWith(storagePath) || !destPath.startsWith(storagePath)) {
            throw InvalidFileTypeException("Invalid path")
        }

        if (sourcePath == storagePath) {
            throw DirectoryOperationException("Cannot move root directory")
        }

        return try {
            Files.move(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING)
            logger.info { "Moved folder from $source to $destination" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Failed to move folder" }
            throw DirectoryOperationException("Failed to move folder: ${e.message}")
        }
    }

    override fun searchFiles(query: String, folder: String?): SearchResultDTO {
        val searchPath = if (folder.isNullOrBlank()) {
            storagePath
        } else {
            storagePath.resolve(folder).normalize()
        }

        val results = mutableListOf<MediaInfoDTO>()
        val searchTime = measureTimeMillis {
            searchInDirectory(searchPath, query.lowercase(), results)
        }

        return SearchResultDTO(
            query = query,
            totalResults = results.size,
            results = results,
            searchTime = searchTime
        )
    }

    override fun getAllMediaFiles(): List<MediaInfoDTO> {
        val mediaFiles = mutableListOf<MediaInfoDTO>()
        collectAllFiles(storagePath, mediaFiles)
        return mediaFiles
    }

    override fun getSystemInfo(): FileSystemInfoDTO {
        val totalSpace = storagePath.toFile().totalSpace
        val freeSpace = storagePath.toFile().freeSpace
        val usedSpace = totalSpace - freeSpace
        val usagePercentage = ((usedSpace.toDouble() / totalSpace) * 100).toInt()

        return FileSystemInfoDTO(
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            usedSpace = usedSpace,
            totalSpaceFormatted = FileUtils.formatFileSize(totalSpace),
            freeSpaceFormatted = FileUtils.formatFileSize(freeSpace),
            usedSpaceFormatted = FileUtils.formatFileSize(usedSpace),
            usagePercentage = usagePercentage
        )
    }

    override fun getFileTree(): List<FolderInfoDTO> {
        return buildFolderTree(storagePath)
    }

    private fun findFile(filename: String): File {
        val decodedFilename = java.net.URLDecoder.decode(filename, "UTF-8")

        // 1. Try to resolve the path as provided
        var file = storagePath.resolve(decodedFilename).normalize().toFile()

        // 2. If not found, check if it's inside a type-based subfolder
        if (!file.exists()) {
            val typeFolder = getFileTypeFolder(decodedFilename)
            file = storagePath.resolve(typeFolder).resolve(decodedFilename).normalize().toFile()
        }

        logger.debug { "Looking for file: ${file.absolutePath}" }

        if (!file.exists() || !file.isFile) {
            logger.error { "File not found: ${file.absolutePath}" }
            throw FileNotFoundException("File not found: $filename")
        }

        if (!file.absolutePath.startsWith(storagePath.toAbsolutePath().toString())) {
            throw InvalidFileTypeException("Path traversal detected")
        }

        return file
    }

    private fun buildMediaInfoDTO(file: File, relativePath: String): MediaInfoDTO {
        val parentFolder = file.parentFile?.name ?: ""
        val folderPath = if (file.parentFile == storagePath.toFile()) {
            ""
        } else {
            storagePath.relativize(file.parentFile?.toPath() ?: storagePath).toString()
        }

        return MediaInfoDTO(
            filename = file.name,
            title = file.nameWithoutExtension,
            path = relativePath,
            parentFolder = parentFolder,
            folderPath = folderPath,
            size = file.length(),
            sizeFormatted = FileUtils.formatFileSize(file.length()),
            contentType = FileUtils.detectContentType(file.name),
            lastModified = Instant.ofEpochMilli(file.lastModified()),
            duration = null,
            bitrate = null,
            resolution = FileUtils.detectResolution(file.name),
            videoCodec = null,
            audioCodec = null,
            hasAudio = true,
            streamUrl = "/api/stream/$relativePath",
            downloadUrl = "/api/files/$relativePath",
            isDirectory = false
        )
    }

    private fun getFolderInfo(folderPath: Path): FolderInfoDTO {
        var fileCount = 0
        var totalSize = 0L

        folderPath.toFile().walkTopDown().forEach { file ->
            if (file.isFile) {
                fileCount++
                totalSize += file.length()
            }
        }

        return FolderInfoDTO(
            name = folderPath.fileName.toString(),
            path = storagePath.relativize(folderPath).toString(),
            fileCount = fileCount,
            totalSize = totalSize,
            totalSizeFormatted = FileUtils.formatFileSize(totalSize),
            lastModified = Instant.ofEpochMilli(folderPath.toFile().lastModified())
        )
    }

    private fun searchInDirectory(directory: Path, query: String, results: MutableList<MediaInfoDTO>) {
        try {
            Files.newDirectoryStream(directory).use { stream ->
                stream.forEach { entry ->
                    if (Files.isDirectory(entry)) {
                        searchInDirectory(entry, query, results)
                    } else {
                        if (entry.fileName.toString().lowercase().contains(query)) {
                            results.add(buildMediaInfoDTO(entry.toFile(), storagePath.relativize(entry).toString()))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error searching directory: $directory" }
        }
    }

    private fun collectAllFiles(directory: Path, files: MutableList<MediaInfoDTO>) {
        try {
            Files.newDirectoryStream(directory).use { stream ->
                stream.forEach { entry ->
                    if (Files.isDirectory(entry)) {
                        collectAllFiles(entry, files)
                    } else {
                        files.add(buildMediaInfoDTO(entry.toFile(), storagePath.relativize(entry).toString()))
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error collecting files from: $directory" }
        }
    }

    private fun buildFolderTree(directory: Path): List<FolderInfoDTO> {
        val folders = mutableListOf<FolderInfoDTO>()

        try {
            Files.newDirectoryStream(directory) { entry ->
                Files.isDirectory(entry)
            }.use { stream ->
                stream.forEach { entry ->
                    folders.add(getFolderInfo(entry))
                    // Recursively add subfolders
                    folders.addAll(buildFolderTree(entry))
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error building folder tree" }
        }

        return folders.distinctBy { it.path }.sortedBy { it.name.lowercase() }
    }
}