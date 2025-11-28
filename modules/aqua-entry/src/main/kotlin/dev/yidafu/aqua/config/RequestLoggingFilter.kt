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

package dev.yidafu.aqua.config

import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import org.springframework.web.util.WebUtils
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1) // Run after CorrelationFilter but before everything else
class RequestLoggingFilter : OncePerRequestFilter() {
  private val logger = LoggerFactory.getLogger("dev.yidafu.aqua.security.request")
  private val errorLogger = LoggerFactory.getLogger("dev.yidafu.aqua.security.error")

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val startTime = System.currentTimeMillis()
    val correlationId = CorrelationIdHolder.getCorrelationId()
    val requestId = UUID.randomUUID().toString().substring(0, 8)

    // Log request entry
    logRequestEntry(request, correlationId ?: "", requestId, startTime)

    // Wrap request and response to enable multiple reads
    val wrappedRequest = if (isAsyncDispatch(request)) request else ContentCachingRequestWrapper(request)
    val wrappedResponse = ContentCachingResponseWrapper(response)

    var exception: Exception? = null

    try {
      filterChain.doFilter(wrappedRequest, wrappedResponse)
    } catch (ex: ServletException) {
      exception = ex
      logFilterException(ex, correlationId ?: "", requestId, "ServletException")
      throw ex
    } catch (ex: IOException) {
      exception = ex
      logFilterException(ex, correlationId ?: "", requestId, "IOException")
      throw ex
    } catch (ex: RuntimeException) {
      exception = ex
      logFilterException(ex, correlationId ?: "", requestId, "RuntimeException")
      throw ex
    } catch (ex: Exception) {
      exception = ex
      logFilterException(ex, correlationId ?: "", requestId, "UnexpectedException")
      throw ex
    } finally {
      // Log request completion
      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime

      // Copy response content before logging
      wrappedResponse.copyBodyToResponse()

      logRequestCompletion(
        request = wrappedRequest,
        response = wrappedResponse,
        correlationId = correlationId ?: "",
        requestId = requestId,
        duration = duration,
        exception = exception
      )
    }
  }

  private fun logRequestEntry(
    request: HttpServletRequest,
    correlationId: String,
    requestId: String,
    startTime: Long,
  ) {
    val clientIp = getClientIp(request)
    val userAgent = request.getHeader("User-Agent")
    val contentType = request.contentType
    val contentLength = request.contentLengthLong

    logger.info(
      "REQUEST_ENTRY - CorrelationId: {}, RequestId: {}, Method: {}, URI: {}, IP: {}, UserAgent: {}, ContentType: {}, ContentLength: {}, Timestamp: {}",
      correlationId,
      requestId,
      request.method,
      request.requestURI,
      clientIp,
      userAgent,
      contentType,
      contentLength,
      startTime
    )

    // Log all headers for debugging (only in debug mode)
    if (logger.isDebugEnabled) {
      val headers = request.headerNames.asSequence()
        .associateWith { headerName ->
          request.getHeaders(headerName)?.asSequence()?.toList() ?: emptyList()
        }

      logger.debug(
        "REQUEST_HEADERS - CorrelationId: {}, RequestId: {}, Headers: {}",
        correlationId,
        requestId,
        headers
      )
    }

    // Log query parameters
    val queryParams = request.parameterMap.map { (key, values) ->
      "$key=${values.joinToString(",")}"
    }.joinToString("&")

    if (queryParams.isNotEmpty()) {
      logger.info(
        "REQUEST_PARAMS - CorrelationId: {}, RequestId: {}, QueryParams: {}",
        correlationId,
        requestId,
        queryParams
      )
    }
  }

  private fun logRequestCompletion(
    request: HttpServletRequest,
    response: HttpServletResponse,
    correlationId: String,
    requestId: String,
    duration: Long,
    exception: Exception?,
  ) {
    val status = response.status
    val statusText = getStatusText(status)

    val logLevel = when {
      status >= 500 -> "ERROR"
      status >= 400 -> "WARN"
      duration > 5000 -> "WARN"
      else -> "INFO"
    }

    val message = buildString {
      append("REQUEST_COMPLETE - ")
      append("CorrelationId: $correlationId, ")
      append("RequestId: $requestId, ")
      append("Method: ${request.method}, ")
      append("URI: ${request.requestURI}, ")
      append("Status: $status $statusText, ")
      append("Duration: ${duration}ms, ")
      append("ContentType: ${response.contentType}")
    }

    when (logLevel) {
      "ERROR" -> {
        errorLogger.error(message, exception)
        // Log response body for 500 errors if available
        logResponseBodyForErrors(request, response, correlationId, requestId)
      }
      "WARN" -> logger.warn(message, exception)
      else -> logger.info(message)
    }

    // Performance warning
    if (duration > 5000) {
      logger.warn(
        "SLOW_REQUEST - CorrelationId: {}, RequestId: {}, Method: {}, URI: {}, Duration: {}ms",
        correlationId,
        requestId,
        request.method,
        request.requestURI,
        duration
      )
    }
  }

  private fun logFilterException(
    exception: Exception,
    correlationId: String,
    requestId: String,
    exceptionType: String,
  ) {
    errorLogger.error(
      "FILTER_EXCEPTION - CorrelationId: {}, RequestId: {}, ExceptionType: {}, Message: {}",
      correlationId,
      requestId,
      exceptionType,
      exception.message,
      exception
    )
  }

  private fun logResponseBodyForErrors(
    request: HttpServletRequest,
    response: HttpServletResponse,
    correlationId: String,
    requestId: String,
  ) {
    if (response is ContentCachingResponseWrapper) {
      val content = response.contentAsByteArray
      if (content.isNotEmpty()) {
        try {
          val responseBody = String(content, StandardCharsets.UTF_8)
          errorLogger.error(
            "ERROR_RESPONSE_BODY - CorrelationId: {}, RequestId: {}, Body: {}",
            correlationId,
            requestId,
            responseBody
          )
        } catch (e: Exception) {
          logger.warn(
            "Failed to log response body - CorrelationId: {}, RequestId: {}, Error: {}",
            correlationId,
            requestId,
            e.message
          )
        }
      }
    }
  }

  private fun getClientIp(request: HttpServletRequest): String {
    val xForwardedFor = request.getHeader("X-Forwarded-For")
    if (!xForwardedFor.isNullOrBlank()) {
      return xForwardedFor.split(",").first().trim()
    }

    val xRealIp = request.getHeader("X-Real-IP")
    if (!xRealIp.isNullOrBlank()) {
      return xRealIp
    }

    return request.remoteAddr ?: "unknown"
  }

  private fun getStatusText(status: Int): String {
    return when (status) {
      200 -> "OK"
      201 -> "Created"
      204 -> "No Content"
      400 -> "Bad Request"
      401 -> "Unauthorized"
      403 -> "Forbidden"
      404 -> "Not Found"
      405 -> "Method Not Allowed"
      415 -> "Unsupported Media Type"
      500 -> "Internal Server Error"
      502 -> "Bad Gateway"
      503 -> "Service Unavailable"
      else -> "Unknown"
    }
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    val uri = request.requestURI
    // Skip logging for static resources and health checks
    return uri.startsWith("/actuator/health") ||
           uri.startsWith("/actuator/info") ||
           uri.endsWith(".css") ||
           uri.endsWith(".js") ||
           uri.endsWith(".ico") ||
           uri.endsWith(".png") ||
           uri.endsWith(".jpg") ||
           uri.endsWith(".jpeg") ||
           uri.endsWith(".gif") ||
           uri.endsWith(".svg")
  }
}
