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
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.stereotype.Component

/**
 * 验证异常处理器，用于处理自定义验证异常并转换为GraphQL错误格式
 */
@Component
class ValidationExceptionHandler : DataFetcherExceptionResolverAdapter() {

  override fun resolveToSingleError(
    exception: Throwable,
    environment: DataFetchingEnvironment
  ): GraphQLError? {
    if (exception is ValidationException) {
      return ValidationError(
        errorMessage = exception.message ?: "验证失败"
      )
    }
    return null
  }

  /**
   * 自定义GraphQL错误类，用于表示验证错误
   */
  class ValidationError(
    private val errorMessage: String
  ) : GraphQLError {

    override fun getMessage(): String = errorMessage

    override fun getErrorType(): ErrorType = ErrorType.ValidationError

    override fun getExtensions(): Map<String, Any> = mapOf(
      "validationError" to true,
      "errorType" to "VALIDATION_ERROR"
    )

    override fun getLocations(): MutableList<SourceLocation>? = null
  }
}