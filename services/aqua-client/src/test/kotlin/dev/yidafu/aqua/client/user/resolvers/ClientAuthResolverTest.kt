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

package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UpdateProfileInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

/**
 * 客户端认证 Resolver 测试(me 查询 / updateProfile 变更):
 * - me 返回当前登录用户信息;用户不存在抛 IllegalArgumentException
 * - updateProfile 将 input 昵称/手机号/头像透传 UserService,并返回更新后用户信息
 */
class ClientAuthResolverTest :
  FunSpec({
    var userService: UserService = mockk()
    var resolver: ClientAuthResolver = mockk()
    var userPrincipal: UserPrincipal = mockk()

    fun sampleUser(nickname: String = "用户甲"): UserModel =
      UserModel(
        id = 1L,
        wechatOpenId = "openid-1",
        nickname = nickname,
        createdAt = LocalDateTime.of(2026, 1, 1, 10, 0),
        updatedAt = LocalDateTime.of(2026, 1, 1, 10, 0),
      )

    beforeTest {
      userService = mockk(relaxed = true)
      resolver = ClientAuthResolver(userService)
      userPrincipal =
        UserPrincipal(
          id = 1L,
          _username = "openid-1",
          userType = "USER",
          _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
        )
    }

    test("me returns current user info mapped to GraphQL UserInfo") {
      every { userService.findById(1L) } returns sampleUser()

      val result = resolver.me(userPrincipal)

      result.id shouldBe 1L
      result.wechatOpenId shouldBe "openid-1"
      result.nickname shouldBe "用户甲"
      result.phone shouldBe null
      result.avatarUrl shouldBe null
      result.createdAt shouldBe LocalDateTime.of(2026, 1, 1, 10, 0)
      result.updatedAt shouldBe LocalDateTime.of(2026, 1, 1, 10, 0)
      verify { userService.findById(1L) }
    }

    test("me throws IllegalArgumentException when user not found") {
      every { userService.findById(1L) } returns null

      shouldThrow<IllegalArgumentException> { resolver.me(userPrincipal) }
    }

    test("updateProfile delegates all fields to UserService and returns updated info") {
      every { userService.updateUserInfo(1L, "新昵称", "13800138000", "http://a/avatar.png") } returns
        sampleUser(nickname = "新昵称")

      val input =
        UpdateProfileInput(
          nickname = "新昵称",
          phone = "13800138000",
          avatar = "http://a/avatar.png",
        )

      val result = resolver.updateProfile(input, userPrincipal)

      result.nickname shouldBe "新昵称"
      verify {
        userService.updateUserInfo(1L, "新昵称", "13800138000", "http://a/avatar.png")
      }
    }

    test("updateProfile passes null fields through") {
      every { userService.updateUserInfo(1L, null, null, null) } returns sampleUser()

      val result = resolver.updateProfile(UpdateProfileInput(), userPrincipal)

      result.nickname shouldBe "用户甲"
      verify { userService.updateUserInfo(1L, null, null, null) }
    }
  })
