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

package dev.yidafu.aqua.client.user.controller

import dev.yidafu.aqua.api.service.UserService
import dev.yidafu.aqua.client.user.controller.dto.RefreshTokenRequest
import dev.yidafu.aqua.client.user.controller.dto.UpdateProfileRequest
import dev.yidafu.aqua.client.user.controller.dto.WeChatLoginRequest
import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.service.UpdateUserRequest
import dev.yidafu.aqua.user.service.WeChatAuthService
import dev.yidafu.aqua.user.service.impl.AdminAuthServiceImpl
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
  private val weChatAuthService: WeChatAuthService,
  private val userService: UserService,
  private val adminAuthService: AdminAuthServiceImpl,
) {
  /**
   * WeChat mini-program login
   */
  @PostMapping("/wechat/login")
  fun wechatLogin(
    @RequestBody @Valid request: WeChatLoginRequest,
  ): ResponseEntity<ApiResponse<WeChatLoginResponse>> =
    try {
      val response = weChatAuthService.login(request.code)
      ResponseEntity.ok(ApiResponse.success(response))
    } catch (e: Exception) {
      ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Login failed"))
    }

  /**
   * Refresh access token
   */
  @PostMapping("/refresh")
  fun refreshToken(
    @RequestBody @Valid request: RefreshTokenRequest,
  ): ResponseEntity<ApiResponse<WeChatTokenResponse>> =
    try {
      val response = weChatAuthService.refreshToken(request.refreshToken)
      ResponseEntity.ok(ApiResponse.success(response))
    } catch (e: Exception) {
      ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Token refresh failed"))
    }

  /**
   * Get current user info
   */
  @GetMapping("/me")
  fun getCurrentUser(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): ResponseEntity<ApiResponse<UserInfo>> =
    try {
      val userInfo = weChatAuthService.getUserInfo(userPrincipal)
      ResponseEntity.ok(ApiResponse.success(userInfo))
    } catch (e: Exception) {
      ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to get user info"))
    }

  /**
   * Update user profile
   */
  @PutMapping("/profile")
  fun updateProfile(
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
    @RequestBody @Valid request: UpdateProfileRequest,
  ): ResponseEntity<ApiResponse<UserInfo>> =
    try {
      val updateRequest =
        UpdateUserRequest(
          nickname = request.nickname,
          phone = request.phone,
          avatar = request.avatar,
        )
      val userInfo = weChatAuthService.updateUserProfile(userPrincipal, updateRequest)
      ResponseEntity.ok(ApiResponse.success(userInfo))
    } catch (e: Exception) {
      ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to update profile"))
    }
}

typealias UserInfo = dev.yidafu.aqua.user.service.UserInfo
typealias WeChatLoginResponse = dev.yidafu.aqua.user.service.WeChatLoginResponse
typealias WeChatTokenResponse = dev.yidafu.aqua.user.service.WeChatTokenResponse
