package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.api.dto.DeliveryLoginRequest
import dev.yidafu.aqua.api.dto.DeliveryLoginResponse

/**
 * Service interface for delivery worker authentication
 */
interface DeliveryAuthService {
  /**
   * Login with WeChat code
   * @return LoginResponse containing token and whether phone binding is needed
   */
  fun login(request: DeliveryLoginRequest): DeliveryLoginResponse

  /**
   * Bind phone number to delivery worker
   * @return LoginResponse with token after successful binding
   */
  fun bindPhone(
    openId: String,
    phoneNumber: String,
  ): DeliveryLoginResponse

  /**
   * Check authentication status with JWT token
   * @param token JWT token from Authorization header
   * @return LoginResponse with user info if token is valid
   */
  fun checkAuthStatus(token: String): DeliveryLoginResponse
}
