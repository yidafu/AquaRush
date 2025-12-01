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

package dev.yidafu.aqua.user.service

import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.ObjectMapper
import dev.yidafu.aqua.api.dto.NotificationSettingsDTO
import dev.yidafu.aqua.api.dto.UserRole
import dev.yidafu.aqua.api.dto.UserStatus
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.model.User
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Service
class WeChatAuthService(
  private val userRepository: UserRepository,
  private val jwtTokenService: JwtTokenService,
  private val objectMapper: ObjectMapper,
) {
  private val logger = LoggerFactory.getLogger(WeChatAuthService::class.java)

  @Value($$"${aqua.wechat.app-id}")
  private lateinit var appId: String

  @Value($$"${aqua.wechat.app-secret}")
  private lateinit var appSecret: String

  private val restTemplate = RestTemplate()

  /**
   * Authenticate user with WeChat code and return JWT tokens
   */
  @Transactional
  fun login(code: String): WeChatLoginResponse {
    try {
      // Exchange code for OpenID and session key
      val wechatResponse = exchangeCodeForOpenId(code)

      if (wechatResponse.errcode != null && wechatResponse.errcode != 0) {
        throw WeChatAuthException("WeChat API error: ${wechatResponse.errmsg}")
      }

      // Find or create user
      val user = findOrCreateUser(wechatResponse.openid!!)

      // Create user principal
      val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
      val userPrincipal =
        UserPrincipal(
          id = user.id!!,
          _username = user.wechatOpenId,
          userType = "USER",
          _authorities = authorities,
        )

      // Generate JWT tokens
      val accessToken = jwtTokenService.generateAccessToken(userPrincipal)
      val refreshToken = jwtTokenService.generateRefreshToken(userPrincipal)

      logger.info("User logged in successfully: openid={}", wechatResponse.openid)

      return WeChatLoginResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = 86400, // 24 hours
        tokenType = "Bearer",
        userInfo =
          UserInfo(
            id = user.id!!,
            openid = user.wechatOpenId,
            nickname = user.nickname,
            avatar = user.avatarUrl,
            phone = user.phone,
          ),
      )
    } catch (e: Exception) {
      logger.error("WeChat login failed", e)
      throw WeChatAuthException("Login failed: ${e.message}")
    }
  }

  /**
   * Refresh access token using refresh token
   */
  fun refreshToken(refreshToken: String): WeChatTokenResponse {
    try {
      // Validate refresh token and extract user info
      val userPrincipal =
        jwtTokenService.getUserPrincipalFromToken(refreshToken)
          ?: throw WeChatAuthException("Invalid refresh token")

      // Verify user still exists
      val user =
        userRepository
          .findById(userPrincipal.id)
          .orElseThrow { WeChatAuthException("User not found") }

      // Generate new tokens
      val newAccessToken = jwtTokenService.generateAccessToken(userPrincipal)
      val newRefreshToken = jwtTokenService.generateRefreshToken(userPrincipal)

      return WeChatTokenResponse(
        accessToken = newAccessToken,
        refreshToken = newRefreshToken,
        expiresIn = 86400,
        tokenType = "Bearer",
      )
    } catch (e: Exception) {
      logger.error("Token refresh failed", e)
      throw WeChatAuthException("Token refresh failed: ${e.message}")
    }
  }

  /**
   * Exchange WeChat code for OpenID and session key
   */
  private fun exchangeCodeForOpenId(code: String): WeChatCode2SessionResponse {
    val url =
      "https://api.weixin.qq.com/sns/jscode2session" +
        "?appid=$appId" +
        "&secret=$appSecret" +
        "&js_code=$code" +
        "&grant_type=authorization_code"

    logger.debug("Calling WeChat code2session API")
    return restTemplate.getForObject(url, WeChatCode2SessionResponse::class.java)
      ?: throw WeChatAuthException("Failed to call WeChat API")
  }

  /**
   * Find existing user or create new one
   */
  private fun findOrCreateUser(openid: String): User =
    userRepository.findByWechatOpenId(openid)
      ?: User(
        wechatOpenId = openid,
        createdAt = LocalDateTime.now(),
        nickname = "",
        phone = "",
        avatarUrl = null, // Would be from user profile
        updatedAt = LocalDateTime.now(),
        email = "",
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        balance = BigDecimal(0),
        totalSpent = BigDecimal(0),
        lastLoginAt = LocalDateTime.now(),
      ).also { newUser ->
        userRepository.save(newUser)
        logger.info("Created new user with openid: {}", openid)
      }

  /**
   * Get user info by user principal
   */
  fun getUserInfo(userPrincipal: UserPrincipal): UserInfo {
    val user =
      userRepository
        .findById(userPrincipal.id)
        .orElseThrow { WeChatAuthException("User not found") }

    return UserInfo(
      id = user.id!!,
      openid = user.wechatOpenId,
      nickname = user.nickname,
      avatar = user.avatarUrl,
      phone = user.phone,
    )
  }

  /**
   * Update user profile
   */
  @Transactional
  fun updateUserProfile(
    userPrincipal: UserPrincipal,
    request: UpdateUserRequest,
  ): UserInfo {
    val user =
      userRepository
        .findById(userPrincipal.id)
        .orElseThrow { WeChatAuthException("User not found") }

    request.nickname?.let { user.nickname = it }
    request.phone?.let { user.phone = it }
    request.avatar?.let { user.avatarUrl = it }

    val updatedUser = userRepository.save(user)

    return UserInfo(
      id = updatedUser.id!!,
      openid = updatedUser.wechatOpenId,
      nickname = updatedUser.nickname,
      avatar = updatedUser.avatarUrl,
      phone = updatedUser.phone,
    )
  }
}

// Data classes for WeChat API responses and requests

data class WeChatCode2SessionResponse(
  @JsonProperty("openid") val openid: String? = null,
  @JsonProperty("session_key") val sessionKey: String? = null,
  @JsonProperty("unionid") val unionid: String? = null,
  @JsonProperty("errcode") val errcode: Int? = null,
  @JsonProperty("errmsg") val errmsg: String? = null,
)

data class WeChatLoginResponse(
  val accessToken: String,
  val refreshToken: String,
  val expiresIn: Long,
  val tokenType: String,
  val userInfo: UserInfo,
)

data class WeChatTokenResponse(
  val accessToken: String,
  val refreshToken: String,
  val expiresIn: Long,
  val tokenType: String,
)

data class UserInfo(
  val id: Long,
  val openid: String,
  val nickname: String?,
  val avatar: String?,
  val phone: String?,
)

data class UpdateUserRequest(
  val nickname: String? = null,
  val phone: String? = null,
  val avatar: String? = null,
)

class WeChatAuthException(
  message: String,
) : RuntimeException(message)
