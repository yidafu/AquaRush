package dev.yidafu.aqua.user.service.impl

import dev.yidafu.aqua.api.dto.AdminUserInfo
import dev.yidafu.aqua.api.dto.LoginResponse
import dev.yidafu.aqua.api.service.admin.AdminAuthService
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.ext.getAuthorities
import dev.yidafu.aqua.user.service.WeChatAuthException
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Service for handling admin authentication
 */
@Service
class AdminAuthServiceImpl(
  private val adminRepository: AdminRepository,
  private val jwtTokenService: JwtTokenService,
  private val authenticationConfiguration: AuthenticationConfiguration,
  private val bizLogger: BizLogger,
) : AdminAuthService {
  private val logger = LoggerFactory.getLogger(AdminAuthServiceImpl::class.java)

  /**
   * Authenticate admin user with username and password
   */
  @Transactional
  override fun authenticate(
    username: String,
    password: String,
  ): LoginResponse {
    try {
      val authenticationManager = authenticationConfiguration.authenticationManager
      // Authenticate user
      val authentication =
        authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken(username, password),
        )

      SecurityContextHolder.getContext().authentication = authentication

      // Get authenticated user details
      val admin =
        adminRepository
          .findByUsername(username)
          ?: throw WeChatAuthException("Invalid username or password")

      // Create user principal
      val authorities = admin.getAuthorities()
      val userPrincipal =
        UserPrincipal(
          id = admin.id!!,
          _username = admin.username,
          userType = admin.role.toString(),
          _authorities = authorities,
        )

      // Generate JWT tokens
      val accessToken = jwtTokenService.generateAccessToken(userPrincipal)
      val refreshToken = jwtTokenService.generateRefreshToken(userPrincipal)

      // Update last login time
      admin.lastLoginAt = LocalDateTime.now()
      adminRepository.save(admin)

      logger.info("Admin logged in successfully: username={}", username)

      // 记录管理员登录日志
      bizLogger.logLogin(
        userId = admin.id!!.toString(),
        username = username,
        loginMethod = "PASSWORD",
        success = true,
        additionalData = mapOf("role" to admin.role.name),
      )

      return LoginResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = 86400, // 24 hours
        tokenType = "Bearer",
        userInfo =
          AdminUserInfo(
            id = admin.id!!,
            username = admin.username,
            realName = admin.realName,
            role = admin.role.name,
          ),
      )
    } catch (e: Exception) {
      logger.error("Admin authentication failed", e)
      // 记录登录失败日志
      bizLogger.logLogin(
        userId = "0",
        username = username,
        loginMethod = "PASSWORD",
        success = false,
        additionalData = mapOf("error" to (e.message ?: "unknown error")),
      )
      throw WeChatAuthException("Authentication failed: ${e.message}")
    }
  }
}
