package org.vng.filetransferserver.service.impl

import mu.KotlinLogging
import org.apache.tika.Tika
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.vng.filetransferserver.config.StorageProperties
import org.vng.filetransferserver.dto.*
import org.vng.filetransferserver.exception.*
import org.vng.filetransferserver.service.FileService
import org.vng.filetransferserver.util.FileUtils
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Service
class FileServiceImpl(
    private val storageProperties: StorageProperties
) : FileService {

    private val storagePath: Path = storageProperties.getStoragePath()
    private val tika = Tika()

    init {
        initializeStorage()
    }

    private fun initializeStorage() {
        try {
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath)
                logger.info { "Created storage directory: $storagePath" }
            }
            logger.info { "Storage initialized at: $storagePath" }
        } catch (e: IOException) {
            logger.error(e) { "Failed to initialize storage" }
            throw FileStorageException("Failed to initialize storage", e)
        }
    }

    private fun resolvePath(path: String?): Path {
        val targetPath = if (path.isNullOrBlank()) {
            storagePath
        } else {
            val normalized = FileUtils.normalizePath(path)
            storagePath.resolve(normalized)
        }

        // Path traversal prevention
        if (!targetPath.normalize().startsWith(storagePath.normalize())) {
            logger.warn { "Path traversal attempt: $path" }
            throw PathTraversalException("Access denied: Invalid path")
        }

        return targetPath
    }

    override fun uploadFile(file: MultipartFile, folderPath: String?): FileUploadResponseDTO {
        logger.info { "Uploading file: ${file.originalFilename} to folder: $folderPath" }

        val targetDir = resolvePath(folderPath)
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir)
        }

        val sanitizedFilename = FileUtils.sanitizeFilename(file.originalFilename ?: file.name)
        val targetFile = targetDir.resolve(sanitizedFilename)

        if (Files.exists(targetFile)) {
            throw FileStorageException("File already exists: $sanitizedFilename")
        }

        try {
            file.transferTo(targetFile.toFile())
            logger.info { "File uploaded successfully: ${targetFile.toAbsolutePath()}" }

            val relativePath = storagePath.relativize(targetFile).toString()
            return FileUploadResponseDTO(
                filename = relativePath,
                originalFilename = sanitizedFilename,
                size = file.size,
                contentType = file.contentType ?: tika.detect(file.inputStream),
                uploadTime = LocalDateTime.now(),
                downloadUrl = "/api/files/${relativePath.replace("\\", "/")}",
                message = "File uploaded successfully"
            )
        } catch (e: IOException) {
            logger.error(e) { "Failed to upload file: ${file.originalFilename}" }
            throw FileStorageException("Failed to upload file", e)
        }
    }

    override fun downloadFile(path: String): Path {
        logger.info { "Downloading file: $path" }
        val targetFile = resolvePath(path)

        if (!Files.exists(targetFile) || Files.isDirectory(targetFile)) {
            throw FileNotFoundException("File not found: $path")
        }

        return targetFile
    }

    override fun deleteFile(path: String): Boolean {
        logger.info { "Deleting file: $path" }
        val targetFile = resolvePath(path)

        if (!Files.exists(targetFile)) {
            throw FileNotFoundException("File not found: $path")
        }

        if (Files.isDirectory(targetFile)) {
            throw InvalidFileTypeException("Cannot delete directory using file delete: $path")
        }

        try {
            Files.delete(targetFile)
            logger.info { "File deleted successfully: $path" }
            return true
        } catch (e: IOException) {
            logger.error(e) { "Failed to delete file: $path" }
            throw FileStorageException("Failed to delete file", e)
        }
    }

    override fun getFileMetadata(path: String): FileMetadataDTO {
        logger.debug { "Getting metadata for: $path" }
        val targetFile = resolvePath(path)

        if (!Files.exists(targetFile)) {
            throw FileNotFoundException("File not found: $path")
        }

        val attributes = Files.readAttributes(targetFile, BasicFileAttributes::class.java)
        val relativePath = storagePath.relativize(targetFile).toString()

        return FileMetadataDTO(
            filename = targetFile.fileName.toString(),
            path = relativePath,
            size = attributes.size(),
            sizeFormatted = FileUtils.formatFileSize(attributes.size()),
            contentType = if (Files.isRegularFile(targetFile)) tika.detect(targetFile) else "application/x-directory",
            lastModified = attributes.lastModifiedTime().toInstant(),
            isDirectory = Files.isDirectory(targetFile),
            parentPath = targetFile.parent?.let { storagePath.relativize(it).toString() }
        )
    }

    override fun listAllFiles(): List<MediaInfoDTO> {
        logger.debug { "Listing all files" }
        val files = mutableListOf<MediaInfoDTO>()

        try {
            Files.walk(storagePath)
                .use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .forEach { file ->
                            files.add(buildMediaInfoDTO(file))
                        }
                }
        } catch (e: IOException) {
            logger.error(e) { "Failed to list files" }
            throw FileStorageException("Failed to list files", e)
        }

        logger.info { "Found ${files.size} files" }
        return files
    }

    override fun moveFile(sourcePath: String, destinationPath: String): Boolean {
        logger.info { "Moving file from $sourcePath to $destinationPath" }
        val source = resolvePath(sourcePath)
        val destination = resolvePath(destinationPath)

        if (!Files.exists(source)) {
            throw FileNotFoundException("Source file not found: $sourcePath")
        }

        if (Files.isDirectory(source)) {
            throw InvalidFileTypeException("Cannot move directory using file move: $sourcePath")
        }

        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            logger.info { "File moved successfully" }
            return true
        } catch (e: IOException) {
            logger.error(e) { "Failed to move file" }
            throw FileStorageException("Failed to move file", e)
        }
    }

    override fun copyFile(sourcePath: String, destinationPath: String): Boolean {
        logger.info { "Copying file from $sourcePath to $destinationPath" }
        val source = resolvePath(sourcePath)
        val destination = resolvePath(destinationPath)

        if (!Files.exists(source)) {
            throw FileNotFoundException("Source file not found: $sourcePath")
        }

        if (Files.isDirectory(source)) {
            throw InvalidFileTypeException("Cannot copy directory using file copy: $sourcePath")
        }

        try {
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
            logger.info { "File copied successfully" }
            return true
        } catch (e: IOException) {
            logger.error(e) { "Failed to copy file" }
            throw FileStorageException("Failed to copy file", e)
        }
    }

    override fun renameFile(path: String, newName: String): Boolean {
        logger.info { "Renaming file: $path to $newName" }
        val source = resolvePath(path)

        if (!Files.exists(source)) {
            throw FileNotFoundException("File not found: $path")
        }

        val sanitizedNewName = FileUtils.sanitizeFilename(newName)
        val destination = source.parent.resolve(sanitizedNewName)

        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            logger.info { "File renamed successfully" }
            return true
        } catch (e: IOException) {
            logger.error(e) { "Failed to rename file" }
            throw FileStorageException("Failed to rename file", e)
        }
    }

    override fun getFolderContents(folderPath: String?): FolderContentsDTO {
        logger.info { "Getting folder contents for: $folderPath" }
        val targetDir = resolvePath(folderPath)

        if (!Files.exists(targetDir)) {
            throw DirectoryNotFoundException("Directory not found: $folderPath")
        }

        if (!Files.isDirectory(targetDir)) {
            throw InvalidFileTypeException("Path is not a directory: $folderPath")
        }

        val folders = mutableListOf<FolderInfoDTO>()
        val files = mutableListOf<MediaInfoDTO>()
        var totalSize = 0L

        try {
            Files.list(targetDir).use { stream ->
                stream.forEach { entry ->
                    val relativePath = storagePath.relativize(entry).toString()
                    if (Files.isDirectory(entry)) {
                        val folderInfo = buildFolderInfoDTO(entry, relativePath)
                        folders.add(folderInfo)
                    } else {
                        files.add(buildMediaInfoDTO(entry))
                        totalSize += Files.size(entry)
                    }
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Failed to list directory contents" }
            throw FileStorageException("Failed to list directory contents", e)
        }

        val parentPath = targetDir.parent?.let { storagePath.relativize(it).toString() }
            ?.takeIf { it.isNotEmpty() }

        return FolderContentsDTO(
            currentPath = folderPath ?: "",
            parentPath = parentPath,
            folders = folders.sortedBy { it.name.lowercase() },
            files = files.sortedBy { it.filename.lowercase() },
            totalSize = totalSize,
            totalSizeFormatted = FileUtils.formatFileSize(totalSize),
            fileCount = files.size,
            folderCount = folders.size
        )
    }

    override fun createFolder(folderPath: String): Boolean {
        logger.info { "Creating folder: $folderPath" }
        val targetDir = resolvePath(folderPath)

        if (Files.exists(targetDir)) {
            throw FileStorageException("Folder already exists: $folderPath")
        }

        try {
            Files.createDirectories(targetDir)
            logger.info { "Folder created successfully: $folderPath" }
            return true
        } catch (e: IOException) {
            logger.error(e) { "Failed to create folder" }
            throw FileStorageException("Failed to create folder", e)
        }
    }

    override fun deleteFolder(folderPath: String): Boolean {
        logger.info { "Deleting folder: $folderPath" }
        val targetDir = resolvePath(folderPath)

        if (!Files.exists(targetDir)) {
            throw DirectoryNotFoundException("Directory not found: $folderPath")
        }

        if (!Files.isDirectory(targetDir)) {
            throw InvalidFileTypeException("Path is not a directory: $folderPath")
        }

        try {
            Files.walk(targetDir)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
            logger.info { "Folder deleted successfully: $folderPath" }
            return true
        } catch (e: IOException) {
            logger.error(e) { "Failed to delete folder" }
            throw FileStorageException("Failed to delete folder", e)
        }
    }

    override fun moveFolder(sourcePath: String, destinationPath: String): Boolean {
        logger.info { "Moving folder from $sourcePath to $destinationPath" }
        val source = resolvePath(sourcePath)
        val destination = resolvePath(destinationPath)

        if (!Files.exists(source)) {
            throw DirectoryNotFoundException("Source directory not found: $sourcePath")
        }

        if (!Files.isDirectory(source)) {
            throw InvalidFileTypeException("Source is not a directory: $sourcePath")
        }

        try {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING)
            logger.info { "Folder moved successfully" }
            return true
        } catch (e: IOException) {
            logger.error(e) { "Failed to move folder" }
            throw FileStorageException("Failed to move folder", e)
        }
    }

    override fun getFolderTree(): List<FolderInfoDTO> {
        logger.debug { "Building folder tree" }
        return buildFolderTree(storagePath, "")
    }

    private fun buildFolderTree(dir: Path, relativePath: String): List<FolderInfoDTO> {
        val result = mutableListOf<FolderInfoDTO>()

        try {
            Files.list(dir).use { stream ->
                val folders = stream.filter { Files.isDirectory(it) }.toList()

                folders.sortedBy { it.fileName.toString().lowercase() }.forEach { folder ->
                    val folderRelativePath = if (relativePath.isEmpty()) {
                        folder.fileName.toString()
                    } else {
                        "$relativePath/${folder.fileName}"
                    }

                    val children = buildFolderTree(folder, folderRelativePath)
                    val folderInfo = buildFolderInfoDTO(folder, folderRelativePath, children)

                    result.add(folderInfo)
                }
            }
        } catch (e: IOException) {
            logger.error(e) { "Failed to build folder tree for: $dir" }
        }

        return result
    }

    private fun buildFolderInfoDTO(
        folder: Path,
        relativePath: String,
        children: List<FolderInfoDTO>? = null
    ): FolderInfoDTO {
        var fileCount = 0
        var totalSize = 0L

        try {
            Files.walk(folder)
                .use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .forEach { file ->
                            fileCount++
                            totalSize += Files.size(file)
                        }
                }
        } catch (e: IOException) {
            logger.error(e) { "Failed to calculate folder stats for: $folder" }
        }

        val attributes = Files.readAttributes(folder, BasicFileAttributes::class.java)

        return FolderInfoDTO(
            name = folder.fileName.toString(),
            path = relativePath,
            fileCount = fileCount,
            totalSize = totalSize,
            totalSizeFormatted = FileUtils.formatFileSize(totalSize),
            lastModified = attributes.lastModifiedTime().toInstant(),
            children = children
        )
    }

    override fun searchFiles(query: String, folderPath: String?): SearchResultDTO {
        logger.info { "Searching for: $query in folder: $folderPath" }
        val searchDir = resolvePath(folderPath)
        val results = mutableListOf<MediaInfoDTO>()
        val searchQuery = query.lowercase()

        try {
            Files.walk(searchDir)
                .use { stream ->
                    stream.filter { Files.isRegularFile(it) }
                        .forEach { file ->
                            val filename = file.fileName.toString().lowercase()
                            if (filename.contains(searchQuery)) {
                                results.add(buildMediaInfoDTO(file))
                            }
                        }
                }
        } catch (e: IOException) {
            logger.error(e) { "Failed to search files" }
            throw FileStorageException("Failed to search files", e)
        }

        logger.info { "Found ${results.size} results for query: $query" }
        return SearchResultDTO(
            query = query,
            results = results,
            count = results.size
        )
    }

    override fun batchOperation(request: BatchOperationDTO): BatchOperationResponseDTO {
        logger.info { "Performing batch operation: ${request.operation} on ${request.files.size} files" }

        val success = mutableListOf<String>()
        val failed = mutableListOf<Pair<String, String>>()

        request.files.forEach { filePath ->
            try {
                when (request.operation.lowercase()) {
                    "delete" -> {
                        deleteFile(filePath)
                        success.add(filePath)
                    }

                    "move" -> {
                        if (request.destination != null) {
                            moveFile(filePath, request.destination)
                            success.add(filePath)
                        } else {
                            failed.add(filePath to "Destination not specified")
                        }
                    }

                    "copy" -> {
                        if (request.destination != null) {
                            copyFile(filePath, request.destination)
                            success.add(filePath)
                        } else {
                            failed.add(filePath to "Destination not specified")
                        }
                    }

                    else -> failed.add(filePath to "Unknown operation: ${request.operation}")
                }
            } catch (e: Exception) {
                logger.error(e) { "Batch operation failed for: $filePath" }
                failed.add(filePath to (e.message ?: "Unknown error"))
            }
        }

        return BatchOperationResponseDTO(
            success = success,
            failed = failed,
            totalSuccess = success.size,
            totalFailed = failed.size
        )
    }

    override fun getSystemInfo(): FileSystemInfoDTO {
        val storageFile = storagePath.toFile()
        val totalSpace = storageFile.totalSpace
        val freeSpace = storageFile.freeSpace
        val usedSpace = totalSpace - freeSpace
        val usagePercentage = if (totalSpace > 0) ((usedSpace.toDouble() / totalSpace) * 100).toInt() else 0

        return FileSystemInfoDTO(
            totalSpace = totalSpace,
            totalSpaceFormatted = FileUtils.formatFileSize(totalSpace),
            freeSpace = freeSpace,
            freeSpaceFormatted = FileUtils.formatFileSize(freeSpace),
            usedSpace = usedSpace,
            usedSpaceFormatted = FileUtils.formatFileSize(usedSpace),
            usagePercentage = usagePercentage
        )
    }

    override fun getFilePreview(path: String): Path {
        logger.info { "Getting preview for: $path" }
        val targetFile = resolvePath(path)

        if (!Files.exists(targetFile) || Files.isDirectory(targetFile)) {
            throw FileNotFoundException("File not found: $path")
        }

        return targetFile
    }

    override fun getThumbnail(path: String): Path? {
        logger.info { "Getting thumbnail for: $path" }
        val targetFile = resolvePath(path)

        if (!Files.exists(targetFile) || Files.isDirectory(targetFile)) {
            throw FileNotFoundException("File not found: $path")
        }

        // For now, return null (no thumbnail generation)
        // Thumbnail generation can be added later with Thumbnailator
        return null
    }

    private fun buildMediaInfoDTO(file: Path): MediaInfoDTO {
        val relativePath = storagePath.relativize(file).toString()
        val filename = file.fileName.toString()
        val size = Files.size(file)
        val contentType = tika.detect(file)
        val attributes = Files.readAttributes(file, BasicFileAttributes::class.java)

        val parentFolder = file.parent?.let { storagePath.relativize(it).toString() } ?: ""

        return MediaInfoDTO(
            filename = filename,
            title = filename,
            path = relativePath,
            parentFolder = parentFolder,
            folderPath = parentFolder,
            size = size,
            sizeFormatted = FileUtils.formatFileSize(size),
            contentType = contentType,
            lastModified = attributes.lastModifiedTime().toInstant(),
            duration = null,
            bitrate = null,
            resolution = null,
            videoCodec = null,
            audioCodec = null,
            hasAudio = false,
            streamUrl = "/api/stream?path=${relativePath.replace("\\", "/")}",
            downloadUrl = "/api/files/${relativePath.replace("\\", "/")}"
        )
    }
}