package org.vng.filetransferserver.exception

class FileNotFoundException(message: String) : RuntimeException(message)
class InvalidFileTypeException(message: String) : RuntimeException(message)
class FileSizeLimitExceededException(message: String) : RuntimeException(message)
class FileStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class DirectoryNotFoundException(message: String) : RuntimeException(message)
class PathTraversalException(message: String) : RuntimeException(message)