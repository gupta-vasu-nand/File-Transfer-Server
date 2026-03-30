package org.vng.filetransferserver.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig

@ConfigurationProperties(prefix = "file.storage")
data class StorageProperties(
    var path: String = "/tmp/file-storage",
    var maxSize: Long = 1073741824 // 1GB default
) {
    fun getStoragePath(): Path = Paths.get(path).toAbsolutePath().normalize()
}