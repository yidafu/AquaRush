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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.api.service.marketing.CustomerGroupQueryApiService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserListInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.UserStatus as GraphQLUserStatus

class UserQueryResolverTest :
  FunSpec({
    var userService: UserService = mockk()
    var customerGroupQueryApiService: CustomerGroupQueryApiService = mockk()
    var resolver: UserQueryResolver = mockk()

    fun createSampleUser(): UserModel =
      UserModel(
        id = 1L,
        wechatOpenId = "openid_1",
        nickname = "用户甲",
        phone = "13900139000",
        status = GraphQLUserStatus.ACTIVE,
        customerGroupId = 10L,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun mockCurrentUser(id: Long) {
      SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(
          UserPrincipal(id, "admin", "ADMIN", emptyList()),
          null,
          emptyList(),
        )
    }

    beforeTest {
      userService = mockk(relaxed = true)
      customerGroupQueryApiService = mockk(relaxed = true)
      resolver = UserQueryResolver(userService, customerGroupQueryApiService)
    }
    afterTest {
      SecurityContextHolder.clearContext()
    }
    test("me should return current user with group name") {
      // Given
      mockCurrentUser(1L)
      every { userService.findById(1L) } returns createSampleUser()
      every { customerGroupQueryApiService.findById(10L) } returns
        CustomerGroupModel(id = 10L, name = "老客户")

      // When
      val result = resolver.me()

      // Then
      result.shouldNotBeNull()
      result.customerGroupName shouldBe "老客户"
    }
    test("me should throw when not authenticated") {
      // When & Then
      shouldThrow<IllegalArgumentException> { resolver.me() }
    }
    test("user should map user with group name") {
      // Given
      every { userService.findById(1L) } returns createSampleUser()
      every { customerGroupQueryApiService.findById(10L) } returns CustomerGroupModel(id = 10L, name = "老客户")

      // When
      val result = resolver.user(1L)

      // Then
      result.shouldNotBeNull()
      result.nickname shouldBe "用户甲"
      result.customerGroupName shouldBe "老客户"
    }
    test("user should return null when missing") {
      // Given
      every { userService.findById(999L) } returns null

      // When
      val result = resolver.user(999L)

      // Then
      result.shouldBeNull()
    }
    test("users should search by customer group") {
      // Given
      every { userService.findUsersByCustomerGroupId(10L, any()) } returns
        PageImpl(listOf(createSampleUser()))
      val input = UserListInput(customerGroupId = 10L)

      // When
      val result = resolver.users(input)

      // Then
      result.list.size shouldBe 1
      verify { userService.findUsersByCustomerGroupId(10L, any()) }
    }
    test("users should search by keyword and status") {
      // Given
      every {
        userService.findUsersByKeywordAndStatus("张三", GraphQLUserStatus.ACTIVE, any())
      } returns PageImpl(listOf(createSampleUser()))
      val input = UserListInput(search = "张三", status = GraphQLUserStatus.ACTIVE)

      // When
      val result = resolver.users(input)

      // Then
      result.list.size shouldBe 1
      verify {
        userService.findUsersByKeywordAndStatus("张三", GraphQLUserStatus.ACTIVE, any())
      }
    }
    test("users should fall back to all users with defaults") {
      // Given
      every { userService.findAllUsers(any()) } returns PageImpl(listOf(createSampleUser()))

      // When
      val result = resolver.users(null)

      // Then
      result.list.size shouldBe 1
      verify { userService.findAllUsers(any()) }
    }
    test("users should parse descending sort direction") {
      // Given
      every { userService.findAllUsers(any()) } returns PageImpl(emptyList())
      val input = UserListInput(sort = "createdAt,desc")

      // When
      val result = resolver.users(input)

      // Then
      result.list.size shouldBe 0
    }
  })
