package org.vng.filetransferserver.config

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Bean
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Primary

private val logger = KotlinLogging.logger {}

@Configuration
class StorageConfig {

    @Value("\${file.storage.path:/tmp/file-storage}")
    lateinit var storagePath: String

    @Bean
    @Primary
    fun storageDirectory(): Path {
        val path = Paths.get(storagePath).toAbsolutePath().normalize()
        if (!Files.exists(path)) {
            Files.createDirectories(path)
            logger.info { "Created storage directory: $path" }
        }
        logger.info { "Storage directory configured: $path" }
        return path
    }

    @Bean
    fun tempDirectory(): Path {
        val tempPath = Paths.get(storagePath, "temp")
        if (!Files.exists(tempPath)) {
            Files.createDirectories(tempPath)
        }
        return tempPath
    }

    @PostConstruct
    fun initStorage() {
        val path = Paths.get(storagePath)
        require(!Files.exists(path) || Files.isDirectory(path)) {
            "Storage path exists but is not a directory"
        }
        if (!Files.exists(path)) {
            Files.createDirectories(path)
        }
    }
}