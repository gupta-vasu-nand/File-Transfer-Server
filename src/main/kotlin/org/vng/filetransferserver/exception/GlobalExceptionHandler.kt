package org.vng.filetransferserver.exception

import org.vng.filetransferserver.dto.ErrorResponseDTO
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.servlet.resource.NoResourceFoundException

@ControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFound(ex: FileNotFoundException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("File not found: ${ex.message}")
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.message ?: "File not found", request)
    }

    @ExceptionHandler(InvalidFileTypeException::class)
    fun handleInvalidFileType(ex: InvalidFileTypeException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("Invalid file type: ${ex.message}")
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid file type", request)
    }

    @ExceptionHandler(FileSizeLimitExceededException::class)
    fun handleFileSizeLimitExceeded(ex: FileSizeLimitExceededException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("File size limit exceeded: ${ex.message}")
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, ex.message ?: "File size exceeds the maximum allowed limit", request)
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceeded(ex: MaxUploadSizeExceededException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("Max upload size exceeded: ${ex.message}")
        val message = "File size exceeds the maximum upload limit (${ex.maxUploadSize / (1024 * 1024)}MB)"
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, message, request)
    }

    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorage(ex: FileStorageException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("File storage error: ${ex.message}", ex)
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: "File storage error", request)
    }

    @ExceptionHandler(StorageInitializationException::class)
    fun handleStorageInitialization(ex: StorageInitializationException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("Storage initialization error: ${ex.message}", ex)
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: "Storage initialization failed", request)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("Validation error: ${ex.message}")
        val errors = ex.bindingResult.allErrors.associate {
            when (it) {
                is FieldError -> it.field to (it.defaultMessage ?: "Invalid value")
                else -> it.objectName to (it.defaultMessage ?: "Invalid value")
            }
        }
        val message = "Validation failed: $errors"
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("Malformed JSON request: ${ex.message}")
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed request body", request)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(ex: NoResourceFoundException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("Resource not found: ${ex.message}")
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found", request)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("Illegal argument: ${ex.message}")
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.message ?: "Invalid request parameter", request)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error("Unexpected error: ${ex.message}", ex)
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later.",
            request
        )
    }

    private fun buildErrorResponse(
        status: HttpStatus,
        message: String,
        request: WebRequest
    ): ResponseEntity<ErrorResponseDTO> {
        val errorResponse = ErrorResponseDTO(
            timestamp = java.time.LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = message,
            path = request.getDescription(false).replace("uri=", "")
        )
        return ResponseEntity.status(status).body(errorResponse)
    }
}