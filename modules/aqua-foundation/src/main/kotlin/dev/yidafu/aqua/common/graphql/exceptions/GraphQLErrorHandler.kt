/**
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

package dev.yidafu.aqua.common.graphql.exceptions

import dev.yidafu.aqua.common.exception.BusinessException
import graphql.ErrorType
import graphql.GraphQLError
import graphql.language.SourceLocation
import graphql.schema.DataFetchingEnvironment
import jakarta.validation.ConstraintViolationException
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component

/**
 * 业务异常 → GraphQL 错误解析器。
 *
 * 注册为 DataFetcherExceptionResolver 后,BusinessException(如 NotFoundException「订单不存在」)
 * 会被转换为可读的 GraphQL 错误(message + extensions.code,如 NOT_FOUND),而不是被
 * Spring GraphQL 默认掩码为 `INTERNAL_ERROR for <executionId>`(classification: INTERNAL_ERROR)。
 *
 * 只解析 BusinessException;校验异常仍由 ValidationExceptionHandler 提供字段级错误,
 * 安全异常由 Spring Security 的 SecurityDataFetcherExceptionResolver 处理,其余未知异常维持默认掩码。
 */
@Component
class GraphQLErrorHandler : DataFetcherExceptionResolverAdapter() {
  override fun resolveToSingleError(
    exception: Throwable,
    environment: DataFetchingEnvironment,
  ): GraphQLError? =
    when (exception) {
      is BusinessException -> handleException(exception)
      else -> null
    }

  fun handleException(throwable: Throwable): GraphQLError =
    object : GraphQLError {
      override fun getMessage(): String =
        when (throwable) {
          is ConstraintViolationException -> extractValidationMessage(throwable)
          is BusinessException -> throwable.message
          is AuthenticationException -> "Authentication failed: ${throwable.message}"
          is AccessDeniedException -> "Access denied: ${throwable.message}"
          is IllegalArgumentException -> "Invalid input: ${throwable.message}"
          else -> "Internal server error: ${throwable.message ?: "Unknown error"}"
        }

      override fun getLocations(): List<SourceLocation>? = emptyList()

      override fun getErrorType(): ErrorType? =
        when (throwable) {
          is ConstraintViolationException -> ErrorType.ValidationError
          is BusinessException -> ErrorType.DataFetchingException
          is AuthenticationException -> ErrorType.DataFetchingException
          is AccessDeniedException -> ErrorType.DataFetchingException
          is IllegalArgumentException -> ErrorType.DataFetchingException
          else -> ErrorType.DataFetchingException
        }

      override fun getExtensions(): Map<String, Any>? =
        when (throwable) {
          is ConstraintViolationException -> extractValidationExtensions(throwable)
          is BusinessException -> mapOf("code" to throwable.code)
          is AuthenticationException -> mapOf("code" to "AUTHENTICATION_ERROR")
          is AccessDeniedException -> mapOf("code" to "ACCESS_DENIED")
          is IllegalArgumentException -> mapOf("code" to "INVALID_INPUT")
          else -> mapOf("code" to "INTERNAL_ERROR")
        }
    }

  private fun extractValidationMessage(ex: ConstraintViolationException): String {
    val violations = ex.constraintViolations
    if (violations.isNotEmpty()) {
      val firstViolation = violations.first()
      val message = firstViolation.message
      val propertyPath = firstViolation.propertyPath.toString()

      // Try to extract field name from property path (e.g., "createAddress.input.city" -> "city")
      val fieldName = propertyPath.split(".").lastOrNull()

      return if (fieldName != null && message != null) {
        "$fieldName: $message"
      } else {
        message ?: "Validation failed"
      }
    }
    return "Validation failed"
  }

  private fun extractValidationExtensions(ex: ConstraintViolationException): Map<String, Any> {
    val violations = ex.constraintViolations
    val validationErrors = mutableMapOf<String, String>()
    val fieldErrors = mutableListOf<Map<String, Any>>()

    violations.forEach { violation ->
      val propertyPath = violation.propertyPath.toString()
      val fieldName = propertyPath.split(".").lastOrNull() ?: "unknown"
      val message = violation.message ?: "Validation error"

      validationErrors[fieldName] = message

      fieldErrors.add(
        mapOf(
          "field" to fieldName,
          "message" to message,
          "invalidValue" to (violation.invalidValue?.toString() ?: "null"),
        ),
      )
    }

    return mapOf(
      "code" to "VALIDATION_ERROR",
      "validationErrors" to validationErrors,
      "fieldErrors" to fieldErrors,
    )
  }
}
