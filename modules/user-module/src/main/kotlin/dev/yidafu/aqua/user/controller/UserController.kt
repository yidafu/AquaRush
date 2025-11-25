package dev.yidafu.aqua.user.controller

import dev.yidafu.aqua.user.domain.model.User
import dev.yidafu.aqua.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/user/users")
class UserController(
    private val userService: UserService
) {
    @GetMapping("/{id}")
    fun getUserById(@PathVariable id: UUID): ResponseEntity<User> {
        val user = userService.findById(id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }

    @GetMapping("/me")
    fun getCurrentUser(@RequestAttribute("userId") userId: UUID): ResponseEntity<User> {
        val user = userService.findById(userId)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(user)
    }

    @PutMapping("/me")
    fun updateCurrentUser(
        @RequestAttribute("userId") userId: UUID,
        @RequestBody request: UpdateUserRequest
    ): ResponseEntity<User> {
        val user = userService.updateUserInfo(
            userId,
            request.nickname,
            request.phone,
            request.avatarUrl
        )
        return ResponseEntity.ok(user)
    }
}

data class UpdateUserRequest(
    val nickname: String?,
    val phone: String?,
    val avatarUrl: String?
)
