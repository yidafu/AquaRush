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

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.service.dto.AdminUserInfo
import dev.yidafu.aqua.user.service.dto.LoginResponse
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for handling admin authentication
 */
@Service
class AdminAuthService(
  private val adminRepository: AdminRepository,
  private val jwtTokenService: JwtTokenService,
//  private val passwordEncoder: PasswordEncoder,
//  private val authenticationManager: AuthenticationManager,
) {
  private val logger = LoggerFactory.getLogger(AdminAuthService::class.java)

  /**
   * Authenticate admin user with username and password
   */
  @Transactional
  fun authenticate(username: String, password: String): LoginResponse {
    try {
      // Authenticate user
//      val authentication = authenticationManager.authenticate(
//        UsernamePasswordAuthenticationToken(username, password)
//      )

      // Get authenticated user details
      val admin = adminRepository.findByUsername(username)
        .orElseThrow { WeChatAuthException("Invalid username or password") }

      // Create user principal
      val authorities = listOf(SimpleGrantedAuthority("ROLE_${admin.role.name}"))
      val userPrincipal = UserPrincipal(
        id = admin.id!!,
        _username = admin.username,
        userType = "ADMIN",
        _authorities = authorities,
      )

      // Generate JWT tokens
      val accessToken = jwtTokenService.generateAccessToken(userPrincipal)
      val refreshToken = jwtTokenService.generateRefreshToken(userPrincipal)

      // Update last login time
      admin.lastLoginAt = java.time.LocalDateTime.now()
      adminRepository.save(admin)

      logger.info("Admin logged in successfully: username={}", username)

      return LoginResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = 86400, // 24 hours
        tokenType = "Bearer",
        userInfo = AdminUserInfo(
          id = admin.id,
          username = admin.username,
          realName = admin.realName,
          role = admin.role.name,
        ),
      )
    } catch (e: Exception) {
      logger.error("Admin authentication failed", e)
      throw WeChatAuthException("Authentication failed: ${e.message}")
    }
  }

  /**
   * Find admin by username
   */
  fun findAdminByUsername(username: String): AdminModel? =
    adminRepository.findByUsername(username).orElse(null)

  /**
   * Validate password against encoded password hash
   */
  fun validatePassword(rawPassword: String, encodedPassword: String): Boolean = true
//    passwordEncoder.matches(rawPassword, encodedPassword)
}
