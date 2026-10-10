/**
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

package dev.yidafu.aqua.user.service.impl

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.service.LoginLockService
import dev.yidafu.aqua.user.service.WeChatAuthException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import java.time.LocalDateTime

/**
 * 补充 AdminAuthServiceTest 未覆盖的失败/异常分支。
 */
class AdminAuthServiceExtraTest :
  FunSpec({
    val adminRepository: AdminRepository = mockk(relaxed = true)
    val jwtTokenService: JwtTokenService = mockk(relaxed = true)
    val authenticationManager: AuthenticationManager = mockk(relaxed = true)
    val authenticationConfiguration: AuthenticationConfiguration = mockk(relaxed = true)
    val loginLockService: LoginLockService = mockk(relaxed = true)
    val bizLogger: BizLogger = mockk(relaxed = true)
    val service =
      AdminAuthServiceImpl(
        adminRepository,
        jwtTokenService,
        authenticationConfiguration,
        loginLockService,
        bizLogger,
      )

    fun admin(): AdminModel =
      AdminModel(
        id = 1L,
        username = "admin",
        passwordHash = "hash",
        realName = "管理员",
        phone = "13900139000",
        role = AdminRoleModel.ADMIN,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun mockAuthManager() {
      every { authenticationConfiguration.authenticationManager } returns authenticationManager
    }

    test("authenticate should throw account disabled exception") {
      mockAuthManager()
      every { authenticationManager.authenticate(any()) } throws DisabledException("disabled")
      val exception = shouldThrow<WeChatAuthException> { service.authenticate("admin", "pass") }
      exception.message!!.contains("账号已停用") shouldBe true
      verify { bizLogger.logLogin("0", "admin", "PASSWORD", false, "LOGIN_FAILED", any()) }
    }
    test("authenticate should throw generic authentication failure") {
      mockAuthManager()
      every { authenticationManager.authenticate(any()) } throws
        org.springframework.security.authentication
          .BadCredentialsException("bad credentials")
      val exception = shouldThrow<WeChatAuthException> { service.authenticate("admin", "pass") }
      exception.message!!.contains("Authentication failed") shouldBe true
      // 登录失败锁定:密码错误计入失败次数
      verify { loginLockService.recordFailure("admin") }
    }
    test("authenticate should throw when admin not found after authentication") {
      mockAuthManager()
      every { authenticationManager.authenticate(any()) } returns UsernamePasswordAuthenticationToken("admin", "pass")
      every { adminRepository.findByUsername("admin") } returns null
      val exception = shouldThrow<WeChatAuthException> { service.authenticate("admin", "pass") }
      exception.message!!.contains("Invalid username or password") shouldBe true
    }
    test("authenticate should update last login and save admin") {
      mockAuthManager()
      val target = admin()
      every { authenticationManager.authenticate(any()) } returns UsernamePasswordAuthenticationToken("admin", "pass")
      every { adminRepository.findByUsername("admin") } returns target
      every { adminRepository.save(any()) } returns target
      every { jwtTokenService.generateAccessToken(any()) } returns "access"
      every { jwtTokenService.generateRefreshToken(any()) } returns "refresh"

      val response = service.authenticate("admin", "pass")

      response.accessToken shouldBe "access"
      response.tokenType shouldBe "Bearer"
      response.expiresIn shouldBe 86400
      verify { adminRepository.save(target) }
      verify { bizLogger.logLogin("1", "admin", "PASSWORD", true, "ADMIN_LOGIN", any()) }
    }
  })
