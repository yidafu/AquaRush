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

package dev.yidafu.aqua.user.controller

import dev.yidafu.aqua.user.domain.model.User
import dev.yidafu.aqua.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user/users")
class UserController(
  private val userService: UserService,
) {
  @GetMapping("/{id}")
  fun getUserById(
    @PathVariable id: Long,
  ): ResponseEntity<User> {
    val user =
      userService.findById(id)
        ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(user)
  }

  @GetMapping("/me")
  fun getCurrentUser(
    // @RequestAttribute("userId") userId: Long,
    userId: Long = 1L, // TODO: Remove this temporary default value when auth is restored
  ): ResponseEntity<User> {

    val user =
      userService.findById(userId)
        ?: return ResponseEntity.notFound().build()
    return ResponseEntity.ok(user)
  }

  @PutMapping("/me")
  fun updateCurrentUser(
    // @RequestAttribute("userId") userId: Long,
    userId: Long = 1L, // TODO: Remove this temporary default value when auth is restored
    @RequestBody request: UpdateUserRequest,
  ): ResponseEntity<User> {
    val user =
      userService.updateUserInfo(
        userId,
        request.nickname,
        request.phone,
        request.avatarUrl,
      )
    return ResponseEntity.ok(user)
  }
}

data class UpdateUserRequest(
  val nickname: String?,
  val phone: String?,
  val avatarUrl: String?,
)
