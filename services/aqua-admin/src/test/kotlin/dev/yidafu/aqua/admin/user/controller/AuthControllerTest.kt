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

package dev.yidafu.aqua.admin.user.controller

import dev.yidafu.aqua.admin.user.dto.AdminLoginRequest
import dev.yidafu.aqua.api.dto.AdminUserInfo
import dev.yidafu.aqua.api.dto.LoginResponse
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.service.impl.AdminAuthServiceImpl
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class AuthControllerTest :
  FunSpec({
    var adminAuthService: AdminAuthServiceImpl = mockk()
    var bizLogger: BizLogger = mockk()
    var controller: AuthController = mockk()

    fun loginResponse(): LoginResponse =
      LoginResponse(
        accessToken = "access-token",
        refreshToken = "refresh-token",
        expiresIn = 7200L,
        tokenType = "Bearer",
        userInfo = AdminUserInfo(id = 1L, username = "admin", realName = "管理员", role = "ADMIN"),
      )

    beforeTest {
      adminAuthService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      controller = AuthController(adminAuthService, bizLogger)
    }
    afterTest {
      SecurityContextHolder.clearContext()
    }
    test("login should return token on success") {
      // Given
      every { adminAuthService.authenticate("admin", "password123") } returns loginResponse()

      // When
      val result = controller.login(AdminLoginRequest(username = "admin", password = "password123"))

      // Then
      result.statusCode shouldBe HttpStatus.OK
      result.body!!.success shouldBe true
      result.body!!.data!!.accessToken shouldBe "access-token"
    }
    test("login should return bad request on failure") {
      // Given
      every { adminAuthService.authenticate("admin", "wrong") } throws IllegalArgumentException("bad credentials")

      // When
      val result = controller.login(AdminLoginRequest(username = "admin", password = "wrong"))

      // Then
      result.statusCode shouldBe HttpStatus.BAD_REQUEST
      result.body!!.message!!.contains("Login failed") shouldBe true
    }
    test("logout should log audit entry for authenticated principal") {
      // Given
      SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
          UserPrincipal(1L, "admin", "ADMIN", emptyList()),
          null,
          emptyList(),
        )

      // When
      val result = controller.logout()

      // Then
      result.statusCode shouldBe HttpStatus.OK
      verify {
        bizLogger.logLogin(
          userId = "1",
          username = "admin",
          loginMethod = "PASSWORD",
          success = true,
          operation = "ADMIN_LOGOUT",
        )
      }
    }
    test("logout should skip audit when not authenticated") {
      // When
      val result = controller.logout()

      // Then
      result.statusCode shouldBe HttpStatus.OK
    }
  })
