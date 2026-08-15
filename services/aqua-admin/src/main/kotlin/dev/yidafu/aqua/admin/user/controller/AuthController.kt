package dev.yidafu.aqua.admin.user.controller

import dev.yidafu.aqua.admin.user.dto.AdminLoginRequest
import dev.yidafu.aqua.api.dto.LoginResponse
import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.service.impl.AdminAuthServiceImpl
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
  private val adminAuthService: AdminAuthServiceImpl,
  private val bizLogger: BizLogger,
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
   * Logout
   *
   * JWT 无状态认证:登出即客户端清除凭证;服务端记录登出业务日志(F-LOG-003,USER 域)。
   * G15 落地:后台登出调用本接口(不再仅清 localStorage)。
   */
  @PostMapping("/logout")
  fun logout(): ResponseEntity<ApiResponse<Unit>> {
    val principal = SecurityContextHolder.getContext().authentication?.principal
    if (principal is UserPrincipal) {
      bizLogger.logLogin(
        userId = principal.id.toString(),
        username = principal.username,
        loginMethod = "PASSWORD",
        success = true,
        operation = "ADMIN_LOGOUT",
      )
    }
    return ResponseEntity.ok(ApiResponse.success(Unit))
  }
}
