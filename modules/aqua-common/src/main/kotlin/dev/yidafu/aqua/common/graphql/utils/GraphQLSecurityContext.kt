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

package dev.yidafu.aqua.common.graphql.utils

import dev.yidafu.aqua.common.security.UserPrincipal
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

object GraphQLSecurityContext {
  fun getCurrentUser(): UserPrincipal? {
    val authentication: Authentication? = SecurityContextHolder.getContext().authentication
    return when {
      authentication != null && authentication.isAuthenticated && authentication.principal is UserPrincipal -> {
        authentication.principal as UserPrincipal
      }
      else -> null
    }
  }

  fun getCurrentUserId(): Long? = getCurrentUser()?.id

  fun getCurrentUserRole(): String? = getCurrentUser()?.userType

  fun hasRole(role: String): Boolean = getCurrentUser()?.hasRole(role) ?: false

  fun hasAnyRole(roles: Collection<String>): Boolean = roles.any { hasRole(it) }

  fun isAuthenticated(): Boolean = getCurrentUser() != null
}
