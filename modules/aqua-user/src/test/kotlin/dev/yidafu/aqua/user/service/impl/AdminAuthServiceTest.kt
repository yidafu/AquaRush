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
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import java.time.LocalDateTime

/**
 * 管理员登录响应(PRD/07-user.md §F-USR-020):LoginResponse.userInfo 需携带头像 URL,
 * 后台顶栏据此展示当前管理员头像;未设置时前端按用户名自动生成 identicon。
 */
class AdminAuthServiceTest {
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

  private fun admin(avatarUrl: String?): AdminModel =
    AdminModel(
      id = 1L,
      username = "admin",
      passwordHash = "hash",
      realName = "管理员",
      phone = "13900139000",
      role = AdminRoleModel.SUPER_ADMIN,
      avatarUrl = avatarUrl,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun mockLoginSuccess(target: AdminModel) {
    every { authenticationConfiguration.authenticationManager } returns authenticationManager
    every { authenticationManager.authenticate(any()) } returns
      UsernamePasswordAuthenticationToken(target.username, "pass")
    every { adminRepository.findByUsername(target.username) } returns target
    // relaxed mock 对泛型返回(save 返回 S)会生成 Object 导致 ClassCastException,需显式 stub
    every { adminRepository.save(any()) } returns target
    every { jwtTokenService.generateAccessToken(any()) } returns "access"
    every { jwtTokenService.generateRefreshToken(any()) } returns "refresh"
  }

  @Test
  fun `login response includes admin avatar when set`() {
    val avatarUrl = "http://cdn.example.com/admin-avatar.jpg"
    mockLoginSuccess(admin(avatarUrl))

    val response = service.authenticate("admin", "pass")

    assertEquals(avatarUrl, response.userInfo.avatar)
    assertEquals("admin", response.userInfo.username)
  }

  @Test
  fun `login response avatar is null when unset`() {
    mockLoginSuccess(admin(null))

    val response = service.authenticate("admin", "pass")

    assertNull(response.userInfo.avatar)
  }
}
