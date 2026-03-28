package org.vng.filetransferserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FileTransferServerApplication

fun main(args: Array<String>) {
    runApplication<FileTransferServerApplication>(*args)
}
