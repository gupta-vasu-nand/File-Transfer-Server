package org.vng.filetransferserver.exception

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import java.time.LocalDateTime
import jakarta.servlet.http.HttpServletRequest
import org.vng.filetransferserver.dto.ErrorResponseDTO

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFound(ex: FileNotFoundException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error { "File not found: ${ex.message}" }
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request)
    }

    @ExceptionHandler(DirectoryNotFoundException::class)
    fun handleDirectoryNotFound(ex: DirectoryNotFoundException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error { "Directory not found: ${ex.message}" }
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request)
    }

    @ExceptionHandler(InvalidFileTypeException::class)
    fun handleInvalidFileType(ex: InvalidFileTypeException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.warn { "Invalid file type: ${ex.message}" }
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request)
    }

    @ExceptionHandler(FileSizeLimitExceededException::class)
    fun handleFileSizeLimitExceeded(ex: FileSizeLimitExceededException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.warn { "File size limit exceeded: ${ex.message}" }
        return buildErrorResponse(ex, HttpStatus.PAYLOAD_TOO_LARGE, request)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(ex: MaxUploadSizeExceededException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.warn { "Max upload size exceeded: ${ex.message}" }
        val errorMessage = "File size exceeds the maximum allowed limit (${ex.maxUploadSize ?: "1GB"})"
        return buildErrorResponse(
            FileSizeLimitExceededException(errorMessage),
            HttpStatus.PAYLOAD_TOO_LARGE,
            request
        )
    }

    @ExceptionHandler(PathTraversalException::class)
    fun handlePathTraversal(ex: PathTraversalException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.warn { "Path traversal attempt: ${ex.message}" }
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN, request)
    }

    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorage(ex: FileStorageException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error { "File storage error: ${ex.message}" }
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error { "Unexpected error: ${ex.message}" }
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
    }

    private fun buildErrorResponse(
        ex: Throwable,
        status: HttpStatus,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponseDTO> {
        val errorResponse = ErrorResponseDTO(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase ?: "Error",
            message = ex.message ?: "An error occurred",
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(errorResponse)
    }
}