package dev.yidafu.aqua.admin.user.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.user.service.AdminAuthService
import dev.yidafu.aqua.user.service.dto.LoginResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/auth")
class AuthController(
  private val adminAuthService: AdminAuthService,
) {

  /**
   * Traditional form login for admin users
   */
  @PostMapping("/login")
  fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
    return try {
      val loginResponse = adminAuthService.authenticate(request.username, request.password)
      ResponseEntity.ok(ApiResponse.success(loginResponse))
    } catch (e: Exception) {
      ResponseEntity.badRequest().body(ApiResponse.error("Login failed: ${e.message}"))
    }
  }

  /**
   * Logout (client-side token removal)
   */
  @PostMapping("/logout")
  fun logout(): ResponseEntity<ApiResponse<Unit>> = ResponseEntity.ok(ApiResponse.success(Unit))
}


data class LoginRequest(
  @field:NotBlank(message = "Username is required")
  val username: String,
  @field:NotBlank(message = "Password is required")
  val password: String,
)
