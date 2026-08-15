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

package dev.yidafu.aqua.user.service

import cn.binarywang.wx.miniapp.api.WxMaService
import cn.binarywang.wx.miniapp.api.WxMaUserService
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.chanjar.weixin.common.error.WxErrorException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WeChatAuthServiceTest {
  private lateinit var userRepository: UserRepository
  private lateinit var jwtTokenService: JwtTokenService
  private lateinit var wxMaService: WxMaService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: WeChatAuthService

  private fun session(openid: String = "openid_1"): WxMaJscode2SessionResult {
    val session = mockk<WxMaJscode2SessionResult>()
    every { session.openid } returns openid
    every { session.sessionKey } returns "session_key"
    every { session.unionid } returns null
    return session
  }

  private fun user(
    id: Long = 1L,
    openId: String = "openid_1",
  ): UserModel =
    UserModel(
      id = id,
      wechatOpenId = openId,
      nickname = "alice",
      phone = "13800138000",
      avatarUrl = null,
      email = "e@e.com",
      status = UserStatus.ACTIVE,
      role = UserRole.USER,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun principal(id: Long = 1L): UserPrincipal =
    UserPrincipal(
      id = id,
      _username = "openid_1",
      userType = "USER",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
    )

  @BeforeEach
  fun setUp() {
    userRepository = mockk(relaxed = true)
    jwtTokenService = mockk(relaxed = true)
    val userServiceApi: WxMaUserService = mockk(relaxed = true)
    wxMaService = mockk(relaxed = true)
    every { wxMaService.userService } returns userServiceApi
    bizLogger = mockk(relaxed = true)
    service =
      WeChatAuthService(
        userRepository,
        jwtTokenService,
        tools.jackson.module.kotlin
          .jacksonObjectMapper(),
        wxMaService,
        bizLogger,
      )
  }

  @Test
  fun `login should return tokens for existing user`() {
    every { wxMaService.userService.getSessionInfo("code") } returns session()
    every { userRepository.findByWechatOpenId("openid_1") } returns user()
    every { jwtTokenService.generateAccessToken(any()) } returns "access"
    every { jwtTokenService.generateRefreshToken(any()) } returns "refresh"

    val response = service.login("code")

    assertEquals("access", response.accessToken)
    assertEquals("refresh", response.refreshToken)
    assertEquals("Bearer", response.tokenType)
    assertEquals(1L, response.userInfo.id)
    verify { bizLogger.logLogin("1", "openid_1", "WECHAT", true, "USER_LOGIN", any()) }
  }

  @Test
  fun `login should create new user when absent`() {
    every { wxMaService.userService.getSessionInfo("code") } returns session("openid_new")
    every { userRepository.findByWechatOpenId("openid_new") } returns null
    every { userRepository.save(any()) } answers { firstArg<UserModel>().also { it.id = 5L } }

    val response = service.login("code")

    assertEquals(5L, response.userInfo.id)
    verify { bizLogger.logUserOperation(5L, "USER_CREATED", "SUCCESS", null, null, mapOf("loginMethod" to "WECHAT")) }
  }

  @Test
  fun `login should throw when wechat api fails`() {
    every { wxMaService.userService.getSessionInfo("code") } throws WxErrorException("api error")
    val exception = assertThrows<WeChatAuthException> { service.login("code") }
    assertTrue(exception.message!!.contains("WeChat API error"))
  }

  @Test
  fun `login should throw when unexpected exception occurs`() {
    every { wxMaService.userService.getSessionInfo("code") } throws RuntimeException("boom")
    val exception = assertThrows<WeChatAuthException> { service.login("code") }
    assertTrue(exception.message!!.contains("Login failed"))
  }

  @Test
  fun `refreshToken should issue new tokens`() {
    every { jwtTokenService.getUserPrincipalFromToken("refresh") } returns principal()
    every { userRepository.findById(1L) } returns Optional.of(user())
    every { jwtTokenService.generateAccessToken(any()) } returns "new-access"
    every { jwtTokenService.generateRefreshToken(any()) } returns "new-refresh"

    val response = service.refreshToken("refresh")

    assertEquals("new-access", response.accessToken)
    assertEquals("new-refresh", response.refreshToken)
  }

  @Test
  fun `refreshToken should throw when principal is null`() {
    every { jwtTokenService.getUserPrincipalFromToken("bad") } returns null
    assertThrows<WeChatAuthException> { service.refreshToken("bad") }
  }

  @Test
  fun `refreshToken should throw when user missing`() {
    every { jwtTokenService.getUserPrincipalFromToken("refresh") } returns principal()
    every { userRepository.findById(1L) } returns Optional.empty()
    assertThrows<WeChatAuthException> { service.refreshToken("refresh") }
  }

  @Test
  fun `getUserInfo should return user info`() {
    every { userRepository.findById(1L) } returns Optional.of(user())
    val info = service.getUserInfo(principal())
    assertEquals(1L, info.id)
    assertEquals("alice", info.nickname)
  }

  @Test
  fun `getUserInfo should throw when user missing`() {
    every { userRepository.findById(1L) } returns Optional.empty()
    assertThrows<WeChatAuthException> { service.getUserInfo(principal()) }
  }

  @Test
  fun `updateUserProfile should update fields`() {
    val existing = user()
    every { userRepository.findById(1L) } returns Optional.of(existing)
    every { userRepository.save(any()) } answers { firstArg() }

    val info = service.updateUserProfile(principal(), UpdateUserRequest(nickname = "bob", phone = "139", avatar = "http://a"))

    assertEquals("bob", info.nickname)
    assertEquals("139", info.phone)
    verify { bizLogger.logUserOperation(1L, "USER_UPDATED", "SUCCESS", 1L, "openid_1", any()) }
  }

  @Test
  fun `updateUserProfile should throw when user missing`() {
    every { userRepository.findById(1L) } returns Optional.empty()
    assertThrows<WeChatAuthException> { service.updateUserProfile(principal(), UpdateUserRequest()) }
  }

  @Test
  fun `updateUserProfile with null fields should keep existing`() {
    val existing = user()
    every { userRepository.findById(1L) } returns Optional.of(existing)
    every { userRepository.save(any()) } answers { firstArg() }
    val info = service.updateUserProfile(principal(), UpdateUserRequest())
    assertEquals("alice", info.nickname)
    assertEquals("13800138000", info.phone)
  }

  @Test
  fun `WeChatLoginResponse should expose fields`() {
    val response = WeChatLoginResponse("a", "r", 86400, "Bearer", UserInfo(1L, "openid", "n", null, null, "openid"))
    assertNotNull(response.userInfo)
    assertEquals(86400, response.expiresIn)
  }
}
