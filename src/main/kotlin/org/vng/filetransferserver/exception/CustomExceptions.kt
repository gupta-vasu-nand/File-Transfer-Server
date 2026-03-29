package org.vng.filetransferserver.exception

class FileNotFoundException(filename: String) :
    RuntimeException("File not found: $filename")

class InvalidFileTypeException(message: String) :
    RuntimeException(message)

class FileSizeLimitExceededException(maxSize: Long) :
    RuntimeException("File size exceeds limit of ${maxSize} bytes")

class FileStorageException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

class DirectoryOperationException(message: String) :
    RuntimeException(message)