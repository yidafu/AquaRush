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

package dev.yidafu.aqua.client.user.controller

import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.client.user.dto.RefreshTokenRequest
import dev.yidafu.aqua.client.user.dto.UpdateProfileRequest
import dev.yidafu.aqua.client.user.dto.WeChatLoginRequest
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.service.UpdateUserRequest
import dev.yidafu.aqua.user.service.UserInfo
import dev.yidafu.aqua.user.service.WeChatAuthService
import dev.yidafu.aqua.user.service.WeChatLoginResponse
import dev.yidafu.aqua.user.service.WeChatTokenResponse
import dev.yidafu.aqua.user.service.impl.AdminAuthServiceImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthControllerTest {
  private lateinit var weChatAuthService: WeChatAuthService
  private lateinit var userService: UserService
  private lateinit var adminAuthService: AdminAuthServiceImpl
  private lateinit var controller: AuthController

  private fun loginResponse(): WeChatLoginResponse =
    WeChatLoginResponse(
      accessToken = "access-token",
      refreshToken = "refresh-token",
      expiresIn = 7200L,
      tokenType = "Bearer",
      userInfo = UserInfo(id = 1L, openid = "openid-1", nickname = "用户甲", avatar = null, phone = null, wechatOpenId = "openid-1"),
    )

  private fun tokenResponse(): WeChatTokenResponse =
    WeChatTokenResponse(accessToken = "new-access", refreshToken = "new-refresh", expiresIn = 7200L, tokenType = "Bearer")

  private fun principal(): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "testuser",
      userType = "USER",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
    )

  @BeforeEach
  fun setUp() {
    weChatAuthService = mockk(relaxed = true)
    userService = mockk(relaxed = true)
    adminAuthService = mockk(relaxed = true)
    controller = AuthController(weChatAuthService, userService, adminAuthService)
  }

  @Test
  fun `wechatLogin should return token on success`() {
    // Given
    every { weChatAuthService.login("code-1") } returns loginResponse()

    // When
    val result = controller.wechatLogin(WeChatLoginRequest(code = "code-1"))

    // Then
    assertEquals(HttpStatus.OK, result.statusCode)
    assertTrue(result.body!!.success)
    assertEquals("access-token", result.body!!.data!!.accessToken)
  }

  @Test
  fun `wechatLogin should return bad request on failure`() {
    // Given
    every { weChatAuthService.login("bad-code") } throws IllegalStateException("invalid code")

    // When
    val result = controller.wechatLogin(WeChatLoginRequest(code = "bad-code"))

    // Then
    assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
    assertTrue(!result.body!!.success)
  }

  @Test
  fun `refreshToken should return new token`() {
    // Given
    every { weChatAuthService.refreshToken("refresh-token") } returns tokenResponse()

    // When
    val result = controller.refreshToken(RefreshTokenRequest(refreshToken = "refresh-token"))

    // Then
    assertEquals(HttpStatus.OK, result.statusCode)
    assertEquals("new-access", result.body!!.data!!.accessToken)
  }

  @Test
  fun `refreshToken should return bad request on failure`() {
    // Given
    every { weChatAuthService.refreshToken("expired") } throws IllegalStateException("expired")

    // When
    val result = controller.refreshToken(RefreshTokenRequest(refreshToken = "expired"))

    // Then
    assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
  }

  @Test
  fun `getCurrentUser should return user info`() {
    // Given
    every { weChatAuthService.getUserInfo(principal()) } returns
      UserInfo(id = 1L, openid = "openid-1", nickname = "用户甲", avatar = null, phone = null, wechatOpenId = "openid-1")

    // When
    val result = controller.getCurrentUser(principal())

    // Then
    assertEquals(HttpStatus.OK, result.statusCode)
    assertEquals("用户甲", result.body!!.data!!.nickname)
  }

  @Test
  fun `getCurrentUser should return bad request on failure`() {
    // Given
    every { weChatAuthService.getUserInfo(any()) } throws IllegalStateException("boom")

    // When
    val result = controller.getCurrentUser(principal())

    // Then
    assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
  }

  @Test
  fun `updateProfile should delegate update request`() {
    // Given
    every { weChatAuthService.updateUserProfile(any(), any()) } returns
      UserInfo(id = 1L, openid = "openid-1", nickname = "新昵称", avatar = null, phone = null, wechatOpenId = "openid-1")

    // When
    val result = controller.updateProfile(principal(), UpdateProfileRequest(nickname = "新昵称"))

    // Then
    assertEquals(HttpStatus.OK, result.statusCode)
    assertEquals("新昵称", result.body!!.data!!.nickname)
    verify { weChatAuthService.updateUserProfile(any(), any<UpdateUserRequest>()) }
  }

  @Test
  fun `updateProfile should return bad request on failure`() {
    // Given
    every { weChatAuthService.updateUserProfile(any(), any()) } throws IllegalStateException("boom")

    // When
    val result = controller.updateProfile(principal(), UpdateProfileRequest(nickname = "新昵称"))

    // Then
    assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
  }
}
