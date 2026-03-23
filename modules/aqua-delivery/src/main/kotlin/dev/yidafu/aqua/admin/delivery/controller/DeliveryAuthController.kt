package dev.yidafu.aqua.admin.delivery.controller

import dev.yidafu.aqua.api.dto.DeliveryLoginResponse
import dev.yidafu.aqua.api.query.DeliveryLoginRequest
import dev.yidafu.aqua.api.service.delivery.DeliveryAuthService
import dev.yidafu.aqua.common.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for delivery worker authentication
 */
@RestController
@RequestMapping("/api/auth/delivery")
class DeliveryAuthController(
  private val deliveryAuthService: DeliveryAuthService,
) {
  /**
   * Login with WeChat code
   * Returns token if already bound, otherwise returns needBindPhone=true
   */
  @PostMapping("/login")
  fun login(
    @RequestBody @Valid request: DeliveryLoginRequest,
  ): ResponseEntity<ApiResponse<DeliveryLoginResponse>> {
    val response = deliveryAuthService.login(request)
    return ResponseEntity.ok(ApiResponse.success(response))
  }

  /**
   * Check authentication status with JWT token
   * Returns user info if token is valid
   */
  @GetMapping("/check")
  fun checkAuthStatus(
    @RequestHeader("Authorization") authHeader: String,
  ): ResponseEntity<ApiResponse<DeliveryLoginResponse>> {
    val response = deliveryAuthService.checkAuthStatus(authHeader)
    return ResponseEntity.ok(ApiResponse.success(response))
  }

  /**
   * Bind phone number to delivery worker
   * OpenID is extracted from JWT token (pending token)
   */
  @PostMapping("/bind-phone")
  fun bindPhone(
    @RequestBody @Valid request: BindPhoneRequest,
    @RequestHeader("Authorization") authHeader: String,
  ): ResponseEntity<ApiResponse<DeliveryLoginResponse>> {
    val response = deliveryAuthService.bindPhone(authHeader, request.phoneNumber)
    return ResponseEntity.ok(ApiResponse.success(response))
  }
}

/**
 * Request DTO for binding phone number
 * OpenID is extracted from JWT token
 */
data class BindPhoneRequest(
  @NotBlank(message = "手机号不能为空")
  val phoneNumber: String,
)
