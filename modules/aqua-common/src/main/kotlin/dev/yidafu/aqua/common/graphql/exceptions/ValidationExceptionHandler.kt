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

package dev.yidafu.aqua.common.graphql.exceptions

import dev.yidafu.aqua.common.graphql.validation.ValidationException
import graphql.ErrorType
import graphql.GraphQLError
import graphql.language.SourceLocation
import graphql.schema.DataFetchingEnvironment
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.stereotype.Component

/**
 * 验证异常处理器，用于处理自定义验证异常并转换为GraphQL错误格式
 */
@Component
class ValidationExceptionHandler : DataFetcherExceptionResolverAdapter() {
  private val logger = LoggerFactory.getLogger(ValidationExceptionHandler::class.java)

  override fun resolveToSingleError(
    exception: Throwable,
    environment: DataFetchingEnvironment,
  ): GraphQLError? {
    when (exception) {
      is ValidationException -> {
        return ValidationError(
          errorMessage = exception.message ?: "验证失败",
        )
      }
      is ConstraintViolationException -> {
        logger.debug("ConstraintViolationException caught: {}", exception.message)
        return createValidationError(exception)
      }
      else -> return null
    }
  }

  private fun createValidationError(ex: ConstraintViolationException): GraphQLError {
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

    // Extract the first validation error for the main message
    val firstViolation = violations.firstOrNull()
    val mainMessage =
      if (firstViolation != null) {
        val fieldName = firstViolation.propertyPath.toString().split(".").lastOrNull()
        val message = firstViolation.message
        if (fieldName != null && message != null) {
          "$fieldName: $message"
        } else {
          message ?: "Validation failed"
        }
      } else {
        "Validation failed"
      }

    return ConstraintValidationError(
      errorMessage = mainMessage,
      validationErrors = validationErrors,
      fieldErrors = fieldErrors,
    )
  }

  /**
   * 自定义GraphQL错误类，用于表示验证错误
   */
  class ValidationError(
    private val errorMessage: String,
  ) : GraphQLError {
    override fun getMessage(): String = errorMessage

    override fun getErrorType(): ErrorType = ErrorType.ValidationError

    override fun getExtensions(): Map<String, Any> =
      mapOf(
        "code" to "VALIDATION_ERROR",
        "validationError" to true,
        "errorType" to "VALIDATION_ERROR",
      )

    override fun getLocations(): MutableList<SourceLocation>? = null
  }

  /**
   * ConstraintViolationException 专用的 GraphQL 错误类
   */
  class ConstraintValidationError(
    private val errorMessage: String,
    private val validationErrors: Map<String, String>,
    private val fieldErrors: List<Map<String, Any>>,
  ) : GraphQLError {
    override fun getMessage(): String = errorMessage

    override fun getErrorType(): ErrorType = ErrorType.ValidationError

    override fun getExtensions(): Map<String, Any> =
      mapOf(
        "code" to "VALIDATION_ERROR",
        "validationErrors" to validationErrors,
        "fieldErrors" to fieldErrors,
        "validationError" to true,
        "errorType" to "CONSTRAINT_VIOLATION_ERROR",
      )

    override fun getLocations(): MutableList<SourceLocation>? = null
  }
}
