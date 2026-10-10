package dev.yidafu.aqua.user.service.impl

import dev.yidafu.aqua.api.dto.AdminUserInfo
import dev.yidafu.aqua.api.dto.LoginResponse
import dev.yidafu.aqua.api.service.admin.AdminAuthService
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.security.toPermissionAuthorities
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.service.LoginLockService
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
  private val loginLockService: LoginLockService,
  private val bizLogger: BizLogger,
) : AdminAuthService {
  private val logger = LoggerFactory.getLogger(AdminAuthServiceImpl::class.java)

  /**
   * Authenticate admin user with username and password
   * 登录失败锁定(PRD 11-system-settings.md §F-SYS-004,AC-SYS-004-02):
   * 锁定期内直接拒绝;密码错误达阈值后锁定;登录成功清零。
   */
  @Transactional
  override fun authenticate(
    username: String,
    password: String,
  ): LoginResponse {
    // 锁定检查置于 try 外:锁定拒绝不计入失败次数(不延长锁定)
    val lockedSeconds = loginLockService.lockedRemainingSeconds(username)
    if (lockedSeconds > 0) {
      val lockedMinutes = (lockedSeconds + 59) / 60
      logger.warn("Admin login rejected by lockout: username={}, remainingSeconds={}", username, lockedSeconds)
      bizLogger.logLogin(
        userId = "0",
        username = username,
        loginMethod = "PASSWORD",
        success = false,
        operation = "LOGIN_FAILED",
        additionalData = mapOf("error" to "ACCOUNT_LOCKED", "lockedSeconds" to lockedSeconds),
      )
      throw WeChatAuthException("账号已锁定,请 $lockedMinutes 分钟后重试")
    }

    var credentialsVerified = false
    try {
      val authenticationManager = authenticationConfiguration.authenticationManager
      // Authenticate user
      val authentication =
        authenticationManager.authenticate(
          UsernamePasswordAuthenticationToken(username, password),
        )
      credentialsVerified = true
      // 密码校验通过:清零失败计数
      loginLockService.clear(username)

      SecurityContextHolder.getContext().authentication = authentication

      // Get authenticated user details
      val admin =
        adminRepository
          .findByUsername(username)
          ?: throw WeChatAuthException("Invalid username or password")

      // Create user principal
      val authorities = admin.role.toPermissionAuthorities()
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
        operation = "ADMIN_LOGIN",
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
            avatar = admin.avatarUrl,
          ),
      )
    } catch (e: org.springframework.security.authentication.DisabledException) {
      // 停用账号登录(AC-USR-020-07,PRD/07-user.md §F-USR-020)
      logger.warn("Admin login failed, account disabled: username={}", username)
      bizLogger.logLogin(
        userId = "0",
        username = username,
        loginMethod = "PASSWORD",
        success = false,
        operation = "LOGIN_FAILED",
        additionalData = mapOf("error" to "ACCOUNT_DISABLED"),
      )
      throw WeChatAuthException("账号已停用，请联系管理员")
    } catch (e: Exception) {
      logger.error("Admin authentication failed", e)
      // 密码校验失败计数(达阈值锁定);认证通过后的后续步骤异常不计入
      if (!credentialsVerified) {
        loginLockService.recordFailure(username)
      }
      // 记录登录失败日志
      bizLogger.logLogin(
        userId = "0",
        username = username,
        loginMethod = "PASSWORD",
        success = false,
        operation = "LOGIN_FAILED",
        additionalData = mapOf("error" to (e.message ?: "unknown error")),
      )
      throw WeChatAuthException("Authentication failed: ${e.message}")
    }
  }
}
