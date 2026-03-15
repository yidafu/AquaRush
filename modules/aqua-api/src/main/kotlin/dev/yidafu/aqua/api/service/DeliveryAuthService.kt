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
   * Bind phone number to delivery worker using JWT token
   * OpenID is extracted from the JWT token
   * @param token JWT token from Authorization header (pending token)
   * @param phoneNumber Phone number to bind
   * @return LoginResponse with token after successful binding
   */
  fun bindPhone(
    token: String,
    phoneNumber: String,
  ): DeliveryLoginResponse

  /**
   * Check authentication status with JWT token
   * @param token JWT token from Authorization header
   * @return LoginResponse with user info if token is valid
   */
  fun checkAuthStatus(token: String): DeliveryLoginResponse
}
