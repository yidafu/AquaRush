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
import dev.yidafu.aqua.user.service.WeChatAuthException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import java.time.LocalDateTime
import kotlin.test.assertTrue

/**
 * 补充 AdminAuthServiceTest 未覆盖的失败/异常分支。
 */
class AdminAuthServiceExtraTest {
  private val adminRepository: AdminRepository = mockk(relaxed = true)
  private val jwtTokenService: JwtTokenService = mockk(relaxed = true)
  private val authenticationManager: AuthenticationManager = mockk(relaxed = true)
  private val authenticationConfiguration: AuthenticationConfiguration = mockk(relaxed = true)
  private val bizLogger: BizLogger = mockk(relaxed = true)

  private val service =
    AdminAuthServiceImpl(
      adminRepository,
      jwtTokenService,
      authenticationConfiguration,
      bizLogger,
    )

  private fun admin(): AdminModel =
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

  private fun mockAuthManager() {
    every { authenticationConfiguration.authenticationManager } returns authenticationManager
  }

  @Test
  fun `authenticate should throw account disabled exception`() {
    mockAuthManager()
    every { authenticationManager.authenticate(any()) } throws DisabledException("disabled")
    val exception = assertThrows<WeChatAuthException> { service.authenticate("admin", "pass") }
    assertTrue(exception.message!!.contains("账号已停用"))
    verify { bizLogger.logLogin("0", "admin", "PASSWORD", false, "LOGIN_FAILED", any()) }
  }

  @Test
  fun `authenticate should throw generic authentication failure`() {
    mockAuthManager()
    every { authenticationManager.authenticate(any()) } throws
      org.springframework.security.authentication
        .BadCredentialsException("bad credentials")
    val exception = assertThrows<WeChatAuthException> { service.authenticate("admin", "pass") }
    assertTrue(exception.message!!.contains("Authentication failed"))
  }

  @Test
  fun `authenticate should throw when admin not found after authentication`() {
    mockAuthManager()
    every { authenticationManager.authenticate(any()) } returns UsernamePasswordAuthenticationToken("admin", "pass")
    every { adminRepository.findByUsername("admin") } returns null
    val exception = assertThrows<WeChatAuthException> { service.authenticate("admin", "pass") }
    assertTrue(exception.message!!.contains("Invalid username or password"))
  }

  @Test
  fun `authenticate should update last login and save admin`() {
    mockAuthManager()
    val target = admin()
    every { authenticationManager.authenticate(any()) } returns UsernamePasswordAuthenticationToken("admin", "pass")
    every { adminRepository.findByUsername("admin") } returns target
    every { adminRepository.save(any()) } returns target
    every { jwtTokenService.generateAccessToken(any()) } returns "access"
    every { jwtTokenService.generateRefreshToken(any()) } returns "refresh"

    val response = service.authenticate("admin", "pass")

    assertEquals("access", response.accessToken)
    assertEquals("Bearer", response.tokenType)
    assertEquals(86400, response.expiresIn)
    verify { adminRepository.save(target) }
    verify { bizLogger.logLogin("1", "admin", "PASSWORD", true, "ADMIN_LOGIN", any()) }
  }
}
