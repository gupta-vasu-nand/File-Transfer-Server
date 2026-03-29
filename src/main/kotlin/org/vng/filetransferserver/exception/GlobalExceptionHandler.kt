package org.vng.filetransferserver.exception

import org.vng.filetransferserver.dto.ErrorResponseDTO
import mu.KotlinLogging
import org.apache.catalina.connector.ClientAbortException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.LocalDateTime
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException::class)
    fun handleFileNotFound(ex: FileNotFoundException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.warn { ex.message }
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request)
    }

    @ExceptionHandler(InvalidFileTypeException::class)
    fun handleInvalidFileType(ex: InvalidFileTypeException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.warn { ex.message }
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request)
    }

    @ExceptionHandler(FileSizeLimitExceededException::class)
    fun handleFileSizeLimit(ex: FileSizeLimitExceededException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.warn { "File size limit exceeded: ${ex.message}" }
        return buildErrorResponse(ex, HttpStatus.PAYLOAD_TOO_LARGE, request)
    }

    @ExceptionHandler(FileStorageException::class)
    fun handleFileStorage(ex: FileStorageException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.error(ex) { "File storage error" }
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
    }

    @ExceptionHandler(DirectoryOperationException::class)
    fun handleDirectoryOperation(ex: DirectoryOperationException, request: HttpServletRequest): ResponseEntity<ErrorResponseDTO> {
        logger.warn { ex.message }
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request)
    }

    // Handle client abort during streaming - don't return JSON error
    @ExceptionHandler(ClientAbortException::class)
    fun handleClientAbort(ex: ClientAbortException, response: HttpServletResponse) {
        logger.debug { "Client aborted connection: ${ex.message}" }
        // Don't write any response as client is already disconnected
        if (!response.isCommitted) {
            response.setStatus(499) // Client Closed Request
        }
    }

    // Handle async request not usable (client disconnected during streaming)
    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun handleAsyncRequestNotUsable(ex: AsyncRequestNotUsableException, response: HttpServletResponse) {
        logger.debug { "Async request not usable (client likely disconnected): ${ex.message}" }
        if (!response.isCommitted) {
            response.setStatus(499)
        }
    }

    // Handle favicon.ico not found - just log debug
    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(ex: NoResourceFoundException, response: HttpServletResponse) {
        if (ex.message?.contains("favicon.ico") == true) {
            logger.debug { "Favicon not found" }
            response.setStatus(HttpStatus.NOT_FOUND.value())
        } else {
            logger.warn { "Resource not found: ${ex.message}" }
            response.setStatus(HttpStatus.NOT_FOUND.value())
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception, request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<ErrorResponseDTO>? {
        // Don't try to return JSON for media streaming endpoints that expect binary content
        val path = request.requestURI
        if (path.contains("/api/stream") || path.contains("/api/preview") || path.contains("/api/thumbnail")) {
            logger.error(ex) { "Error during streaming for path: $path" }
            if (!response.isCommitted) {
                response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
            }
            return null
        }

        logger.error(ex) { "Unexpected error" }
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request)
    }

    private fun buildErrorResponse(
        ex: Exception,
        status: HttpStatus,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponseDTO> {
        val errorResponse = ErrorResponseDTO(
            timestamp = LocalDateTime.now(),
            status = status.value(),
            error = status.reasonPhrase,
            message = ex.message ?: "An error occurred",
            path = request.requestURI
        )
        return ResponseEntity.status(status).body(errorResponse)
    }
}