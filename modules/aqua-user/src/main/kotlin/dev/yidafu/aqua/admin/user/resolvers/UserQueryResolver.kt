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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.common.graphql.utils.GraphQLSecurityContext
import dev.yidafu.aqua.user.domain.model.User
import dev.yidafu.aqua.user.service.UserService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class UserQueryResolver(
  private val userService: UserService,
) {
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun me(): User? {
    val currentUserId =
      GraphQLSecurityContext.getCurrentUserId()
        ?: throw IllegalArgumentException("User not authenticated")
    return userService.findById(currentUserId)
  }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
  fun user(
    @Argument id: Long,
  ): User? = userService.findById(id)
}
