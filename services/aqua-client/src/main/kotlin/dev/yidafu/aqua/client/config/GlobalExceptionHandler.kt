/*
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.config

import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.LocalDateTime
import java.util.*

@RestControllerAdvice
class GlobalExceptionHandler {
  private val logger = LoggerFactory.getLogger("dev.yidafu.aqua.exception")
  private val errorLogger = LoggerFactory.getLogger("dev.yidafu.aqua.error")
  private val auditLogger = LoggerFactory.getLogger("dev.yidafu.aqua.audit")

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleIllegalArgument(
    ex: IllegalArgumentException,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    logger.warn(
      "BAD_REQUEST_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, Message: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      ex.message,
      ex
    )

    val error =
      ErrorResponse(
        code = 400,
        message = ex.message ?: "Invalid argument",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
  }

  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException::class)
  fun handleValidationException(
    ex: org.springframework.web.bind.MethodArgumentNotValidException,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    val validationErrors = ex.bindingResult.fieldErrors.map {
      "${it.field}: ${it.defaultMessage ?: "Invalid value"}"
    }.joinToString("; ")

    logger.warn(
      "VALIDATION_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, ValidationErrors: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      validationErrors,
      ex
    )

    val error =
      ErrorResponse(
        code = 400,
        message = "Validation failed: $validationErrors",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException::class)
  fun handleMethodNotSupported(
    ex: org.springframework.web.HttpRequestMethodNotSupportedException,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    logger.warn(
      "METHOD_NOT_SUPPORTED_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, SupportedMethods: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      ex.supportedMethods?.joinToString(", ") ?: "none",
      ex
    )

    val error =
      ErrorResponse(
        code = 405,
        message = "Method ${request.method} not supported. Supported methods: ${ex.supportedMethods?.joinToString(", ") ?: "none"}",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error)
  }

  @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException::class)
  fun handleMissingParameter(
    ex: org.springframework.web.bind.MissingServletRequestParameterException,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    logger.warn(
      "MISSING_PARAMETER_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, Parameter: {}, ParameterType: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      ex.parameterName,
      ex.parameterType,
      ex
    )

    val error =
      ErrorResponse(
        code = 400,
        message = "Required parameter '${ex.parameterName}' of type ${ex.parameterType} is missing",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
  }

  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException::class)
  fun handleMessageNotReadable(
    ex: org.springframework.http.converter.HttpMessageNotReadableException,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    logger.warn(
      "MESSAGE_NOT_READABLE_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, ContentType: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      request.contentType,
      ex
    )

    val error =
      ErrorResponse(
        code = 400,
        message = "Request body is malformed or unreadable: ${ex.message}",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
  fun handleAccessDeniedException(
    ex: org.springframework.security.access.AccessDeniedException,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    logger.warn(
      "ACCESS_DENIED_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, User: {}, Reason: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      request.userPrincipal?.name ?: "anonymous",
      ex.message
    )

    auditLogger.error(
      "ACCESS_DENIED - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, User: {}, UserAgent: {}, RemoteAddr: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      request.userPrincipal?.name ?: "anonymous",
      request.getHeader("User-Agent"),
      request.remoteAddr
    )

    val error =
      ErrorResponse(
        code = 403,
        message = "权限不足，无法访问: ${request.method} ${request.requestURI}",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
  }

  @ExceptionHandler(SecurityException::class)
  fun handleSecurityException(
    ex: SecurityException,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    logger.warn(
      "SECURITY_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, Error: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      ex.message
    )

    auditLogger.error(
      "SECURITY_VIOLATION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, UserAgent: {}, RemoteAddr: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      request.getHeader("User-Agent"),
      request.remoteAddr
    )

    val error =
      ErrorResponse(
        code = 403,
        message = ex.message ?: "安全验证失败",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
  }

  @ExceptionHandler(org.springframework.dao.DataAccessException::class)
  fun handleDataAccessException(
    ex: org.springframework.dao.DataAccessException,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    errorLogger.error(
      "DATABASE_ACCESS_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, DatabaseError: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      ex.message,
      ex
    )

    auditLogger.error(
      "DATABASE_ERROR - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, ErrorType: {}, Message: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      ex.javaClass.simpleName,
      ex.message
    )

    val error =
      ErrorResponse(
        code = 500,
        message = "Database operation failed. Error ID: $errorId",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
  }

  @ExceptionHandler(Exception::class)
  fun handleGenericException(
    ex: Exception,
    request: HttpServletRequest,
  ): ResponseEntity<ErrorResponse> {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: generateCorrelationId()
    val errorId = UUID.randomUUID().toString().substring(0, 8)

    // Detailed error logging for 500 errors
    errorLogger.error(
      "UNEXPECTED_EXCEPTION - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, ExceptionType: {}, Message: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      ex.javaClass.simpleName,
      ex.message,
      ex
    )

    // Log additional request context for debugging
    logRequestContext(request, correlationId, errorId)

    // Log the full stack trace
    errorLogger.error(
      "EXCEPTION_STACK_TRACE - CorrelationId: {}, ErrorId: {}, StackTrace: {}",
      correlationId,
      errorId,
      getStackTraceAsString(ex)
    )

    auditLogger.error(
      "SYSTEM_ERROR - CorrelationId: {}, ErrorId: {}, Method: {}, URI: {}, ExceptionType: {}, Message: {}",
      correlationId,
      errorId,
      request.method,
      request.requestURI,
      ex.javaClass.simpleName,
      ex.message
    )

    val error =
      ErrorResponse(
        code = 500,
        message = "Internal server error occurred. Error ID: $errorId",
        timestamp = LocalDateTime.now(),
        errorId = errorId,
        correlationId = correlationId
      )
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
  }

  private fun logRequestContext(request: HttpServletRequest, correlationId: String, errorId: String) {
    try {
      val userAgent = request.getHeader("User-Agent")
      val contentType = request.contentType
      val contentLength = request.contentLengthLong
      val remoteAddr = request.remoteAddr
      val requestParams = request.parameterMap.map { (key, values) ->
        "$key=${values.joinToString(",")}"
      }.joinToString("&")

      errorLogger.error(
        "REQUEST_CONTEXT - CorrelationId: {}, ErrorId: {}, RemoteAddr: {}, UserAgent: {}, ContentType: {}, ContentLength: {}, Parameters: {}",
        correlationId,
        errorId,
        remoteAddr,
        userAgent,
        contentType,
        contentLength,
        if (requestParams.length > 500) requestParams.substring(0, 500) + "..." else requestParams
      )

      // Log headers for debugging (sensitive data will be masked)
      val headers = request.headerNames.asSequence()
        .filter { headerName ->
          !headerName.equals("Authorization", ignoreCase = true) &&
          !headerName.equals("Cookie", ignoreCase = true) &&
          !headerName.contains("password", ignoreCase = true) &&
          !headerName.contains("secret", ignoreCase = true)
        }
        .associateWith { headerName ->
          request.getHeaders(headerName)?.asSequence()?.toList() ?: emptyList()
        }

      errorLogger.error(
        "REQUEST_HEADERS - CorrelationId: {}, ErrorId: {}, Headers: {}",
        correlationId,
        errorId,
        headers
      )

    } catch (ex: Exception) {
      logger.warn(
        "Failed to log request context - CorrelationId: {}, ErrorId: {}, Error: {}",
        correlationId,
        errorId,
        ex.message
      )
    }
  }

  private fun generateCorrelationId(): String {
    return "GEN-${UUID.randomUUID().toString().substring(0, 8)}"
  }

  private fun getStackTraceAsString(exception: Exception): String {
    val sw = java.io.StringWriter()
    val pw = java.io.PrintWriter(sw)
    exception.printStackTrace(pw)
    return sw.toString()
  }
}

data class ErrorResponse(
  val code: Int,
  val message: String,
  val timestamp: LocalDateTime,
  val errorId: String? = null,
  val correlationId: String? = null
)
