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

import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.graphql.generated.User
import dev.yidafu.aqua.common.graphql.generated.UserListInput
import dev.yidafu.aqua.common.graphql.generated.UserPage
import dev.yidafu.aqua.common.graphql.util.toPageInfo
import dev.yidafu.aqua.common.graphql.utils.GraphQLSecurityContext
import dev.yidafu.aqua.user.mapper.UserMapper
import dev.yidafu.aqua.user.mapper.UserStatusMapper
import dev.yidafu.aqua.user.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@AdminService
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
    return userService.findById(currentUserId)?.let { UserMapper.map(it) }
  }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
  fun user(
    @Argument id: Long,
  ): User? = userService.findById(id)?.let { UserMapper.map(it) }

  @QueryMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun users(@Argument input: UserListInput?): UserPage {
    // Provide default values if input is null
    val sort = input?.sort ?: "createdAt,desc"
    val page = input?.page ?: 0
    val size = input?.size ?: 20
    val search = input?.search
    val status = input?.status

    // Parse sort parameter (format: "field,direction")
    val sortParams = sort.split(",")
    val sortField = sortParams.getOrNull(0) ?: "createdAt"
    val sortDirection = if (sortParams.getOrNull(1)?.equals("desc", ignoreCase = true) == true) {
      Sort.Direction.DESC
    } else {
      Sort.Direction.ASC
    }

    val pageable: Pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField))

    val userPage = when {
      search != null && status != null -> {
        val domainStatus = UserStatusMapper.toDomainStatus(status)
        userService.findUsersByKeywordAndStatus(search, domainStatus, pageable)
      }
      search != null -> {
        userService.findUsersByKeyword(search, pageable)
      }
      status != null -> {
        val domainStatus = UserStatusMapper.toDomainStatus(status)
        userService.findUsersByStatus(domainStatus, pageable)
      }
      else -> {
        userService.findAllUsers(pageable)
      }
    }

    val (userList, pageInfo) = userPage.toPageInfo{ UserMapper.map(it) }
    return UserPage(
        list = userList,
        pageInfo = pageInfo
    )
  }
}
