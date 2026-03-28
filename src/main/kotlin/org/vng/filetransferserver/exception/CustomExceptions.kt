package org.vng.filetransferserver.exception

class FileStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class FileNotFoundException(message: String) : RuntimeException(message)
class InvalidFileTypeException(message: String) : RuntimeException(message)
class FileSizeLimitExceededException(message: String) : RuntimeException(message)
class StorageInitializationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)