package org.vng.filetransferserver.config

import jakarta.annotation.PostConstruct
import org.vng.filetransferserver.exception.StorageInitializationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
@PropertySource("classpath:application.yml")
class StorageConfig (
    private val environment: Environment
){

    private val logger = LoggerFactory.getLogger(StorageConfig::class.java)

    @Value("\${file.storage.path:/tmp/File-Transfer-Storage}")
    private lateinit var storagePath: String

    @Value("\${file.max-size:1073741824}") // Default 1GB
    private var maxFileSize: Long = 1073741824

    @Value("\${file.allowed-extensions:*}")
    private lateinit var allowedExtensions: String

    fun getStoragePath(): Path = Paths.get(storagePath)

    fun getMaxFileSize(): Long = maxFileSize

    fun getAllowedExtensions(): List<String> {
        return if (allowedExtensions == "*") {
            listOf("*")
        } else {
            allowedExtensions.split(",").map { it.trim().lowercase() }
        }
    }

    @PostConstruct
    fun init() {
        try {
            // Resolve storage path with priority: Environment Variable > Spring Property > Default
            val envPath = System.getenv("FILE_STORAGE_PATH")
            val propertyPath = environment.getProperty("file.storage.path")

            storagePath = when {
                !envPath.isNullOrBlank() -> {
                    logger.info("Using storage path from environment variable: $envPath")
                    envPath
                }
                !propertyPath.isNullOrBlank() -> {
                    logger.info("Using storage path from application.yml: $propertyPath")
                    propertyPath
                }
                else -> {
                    val defaultPath = System.getProperty("user.home") + "/file-transfer-storage"
                    logger.info("Using default storage path: $defaultPath")
                    defaultPath
                }
            }

            // Convert Windows path separators if needed
            storagePath = storagePath.replace("\\", "/")

            val storageDir = getStoragePath()
            if (!Files.exists(storageDir)) {
                logger.info("Creating storage directory: $storageDir")
                Files.createDirectories(storageDir)
                logger.info("Storage directory created successfully")
            } else {
                logger.info("Storage directory already exists: $storageDir")
            }

            // Validate write permissions
            if (!Files.isWritable(storageDir)) {
                throw StorageInitializationException("Storage directory is not writable: $storageDir")
            }

            logger.info("File storage initialized with path: $storageDir")
            logger.info("Max file size: ${maxFileSize / (1024 * 1024)} MB")
            logger.info("Allowed extensions: ${getAllowedExtensions()}")

        } catch (e: Exception) {
            logger.error("Failed to initialize storage: ${e.message}", e)
            throw StorageInitializationException("Failed to initialize storage: ${e.message}", e)
        }
    }
}