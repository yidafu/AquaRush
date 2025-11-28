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

import graphql.ErrorType
import graphql.GraphQLError
import graphql.language.SourceLocation
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component
import java.util.*

@Component
class GraphQLErrorHandler {
  fun handleException(throwable: Throwable): GraphQLError =
    object : GraphQLError {
      override fun getMessage(): String =
        when (throwable) {
          is AuthenticationException -> "Authentication failed: ${throwable.message}"
          is AccessDeniedException -> "Access denied: ${throwable.message}"
          is IllegalArgumentException -> "Invalid input: ${throwable.message}"
          else -> "Internal server error: ${throwable.message ?: "Unknown error"}"
        }

      override fun getLocations(): List<SourceLocation>? = emptyList()

      override fun getErrorType(): ErrorType? =
        when (throwable) {
          is AuthenticationException -> ErrorType.DataFetchingException
          is AccessDeniedException -> ErrorType.DataFetchingException
          is IllegalArgumentException -> ErrorType.DataFetchingException
          else -> ErrorType.DataFetchingException
        }

      override fun getExtensions(): Map<String, Any>? =
        when (throwable) {
          is AuthenticationException -> mapOf("code" to "AUTHENTICATION_ERROR")
          is AccessDeniedException -> mapOf("code" to "ACCESS_DENIED")
          is IllegalArgumentException -> mapOf("code" to "INVALID_INPUT")
          else -> mapOf("code" to "INTERNAL_ERROR")
        }
    }
}
