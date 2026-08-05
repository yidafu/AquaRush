package dev.yidafu.aqua.admin.user.controller

import dev.yidafu.aqua.admin.user.dto.AdminLoginRequest
import dev.yidafu.aqua.api.dto.LoginResponse
import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.user.service.impl.AdminAuthServiceImpl
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
  private val adminAuthService: AdminAuthServiceImpl,
) {
  /**
   * Traditional form login for admin users
   */
  @PostMapping("/login")
  fun login(
    @RequestBody @Valid request: AdminLoginRequest,
  ): ResponseEntity<ApiResponse<LoginResponse>> =
    try {
      val loginResponse = adminAuthService.authenticate(request.username, request.password)
      ResponseEntity.ok(ApiResponse.success(loginResponse))
    } catch (e: Exception) {
      ResponseEntity.badRequest().body(ApiResponse.error("Login failed: ${e.message}"))
    }

  /**
   * Logout (client-side token removal)
   */
  @PostMapping("/logout")
  fun logout(): ResponseEntity<ApiResponse<Unit>> = ResponseEntity.ok(ApiResponse.success(Unit))
}
