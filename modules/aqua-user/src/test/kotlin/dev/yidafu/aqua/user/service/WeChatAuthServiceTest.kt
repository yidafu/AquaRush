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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import me.chanjar.weixin.common.error.WxErrorException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import java.util.Optional

class WeChatAuthServiceTest :
  FunSpec({
    var userRepository: UserRepository = mockk()
    var jwtTokenService: JwtTokenService = mockk()
    var wxMaService: WxMaService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: WeChatAuthService = mockk()

    fun session(openid: String = "openid_1"): WxMaJscode2SessionResult {
      val session = mockk<WxMaJscode2SessionResult>()
      every { session.openid } returns openid
      every { session.sessionKey } returns "session_key"
      every { session.unionid } returns null
      return session
    }

    fun user(
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

    fun principal(id: Long = 1L): UserPrincipal =
      UserPrincipal(
        id = id,
        _username = "openid_1",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )

    beforeTest {
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
    test("login should return tokens for existing user") {
      every { wxMaService.userService.getSessionInfo("code") } returns session()
      every { userRepository.findByWechatOpenId("openid_1") } returns user()
      every { jwtTokenService.generateAccessToken(any()) } returns "access"
      every { jwtTokenService.generateRefreshToken(any()) } returns "refresh"

      val response = service.login("code")

      response.accessToken shouldBe "access"
      response.refreshToken shouldBe "refresh"
      response.tokenType shouldBe "Bearer"
      response.userInfo.id shouldBe 1L
      verify { bizLogger.logLogin("1", "openid_1", "WECHAT", true, "USER_LOGIN", any()) }
    }
    test("login should create new user when absent") {
      every { wxMaService.userService.getSessionInfo("code") } returns session("openid_new")
      every { userRepository.findByWechatOpenId("openid_new") } returns null
      every { userRepository.save(any()) } answers { firstArg<UserModel>().also { it.id = 5L } }

      val response = service.login("code")

      response.userInfo.id shouldBe 5L
      verify { bizLogger.logUserOperation(5L, "USER_CREATED", "SUCCESS", null, null, mapOf("loginMethod" to "WECHAT")) }
    }
    test("login should throw when wechat api fails") {
      every { wxMaService.userService.getSessionInfo("code") } throws WxErrorException("api error")
      val exception = shouldThrow<WeChatAuthException> { service.login("code") }
      exception.message!!.contains("WeChat API error") shouldBe true
    }
    test("login should throw when unexpected exception occurs") {
      every { wxMaService.userService.getSessionInfo("code") } throws RuntimeException("boom")
      val exception = shouldThrow<WeChatAuthException> { service.login("code") }
      exception.message!!.contains("Login failed") shouldBe true
    }
    test("refreshToken should issue new tokens") {
      every { jwtTokenService.getUserPrincipalFromToken("refresh") } returns principal()
      every { userRepository.findById(1L) } returns Optional.of(user())
      every { jwtTokenService.generateAccessToken(any()) } returns "new-access"
      every { jwtTokenService.generateRefreshToken(any()) } returns "new-refresh"

      val response = service.refreshToken("refresh")

      response.accessToken shouldBe "new-access"
      response.refreshToken shouldBe "new-refresh"
    }
    test("refreshToken should throw when principal is null") {
      every { jwtTokenService.getUserPrincipalFromToken("bad") } returns null
      shouldThrow<WeChatAuthException> { service.refreshToken("bad") }
    }
    test("refreshToken should throw when user missing") {
      every { jwtTokenService.getUserPrincipalFromToken("refresh") } returns principal()
      every { userRepository.findById(1L) } returns Optional.empty()
      shouldThrow<WeChatAuthException> { service.refreshToken("refresh") }
    }
    test("getUserInfo should return user info") {
      every { userRepository.findById(1L) } returns Optional.of(user())
      val info = service.getUserInfo(principal())
      info.id shouldBe 1L
      info.nickname shouldBe "alice"
    }
    test("getUserInfo should throw when user missing") {
      every { userRepository.findById(1L) } returns Optional.empty()
      shouldThrow<WeChatAuthException> { service.getUserInfo(principal()) }
    }
    test("updateUserProfile should update fields") {
      val existing = user()
      every { userRepository.findById(1L) } returns Optional.of(existing)
      every { userRepository.save(any()) } answers { firstArg() }

      val info = service.updateUserProfile(principal(), UpdateUserRequest(nickname = "bob", phone = "139", avatar = "http://a"))

      info.nickname shouldBe "bob"
      info.phone shouldBe "139"
      verify { bizLogger.logUserOperation(1L, "USER_UPDATED", "SUCCESS", 1L, "openid_1", any()) }
    }
    test("updateUserProfile should throw when user missing") {
      every { userRepository.findById(1L) } returns Optional.empty()
      shouldThrow<WeChatAuthException> { service.updateUserProfile(principal(), UpdateUserRequest()) }
    }
    test("updateUserProfile with null fields should keep existing") {
      val existing = user()
      every { userRepository.findById(1L) } returns Optional.of(existing)
      every { userRepository.save(any()) } answers { firstArg() }
      val info = service.updateUserProfile(principal(), UpdateUserRequest())
      info.nickname shouldBe "alice"
      info.phone shouldBe "13800138000"
    }
    test("WeChatLoginResponse should expose fields") {
      val response = WeChatLoginResponse("a", "r", 86400, "Bearer", UserInfo(1L, "openid", "n", null, null, "openid"))
      response.userInfo.shouldNotBeNull()
      response.expiresIn shouldBe 86400
    }
  })
