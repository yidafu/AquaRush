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

import dev.yidafu.aqua.api.service.UserService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UpdateProfileInput
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.repository.UserRepository
import dev.yidafu.aqua.user.service.WeChatAuthService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClientUserQueryResolverTest {
  private lateinit var userService: UserService
  private lateinit var weChatAuthService: WeChatAuthService
  private lateinit var userRepository: UserRepository
  private lateinit var resolver: ClientUserQueryResolver
  private lateinit var userPrincipal: UserPrincipal

  private fun createSampleUser(): UserModel =
    UserModel(
      id = 1L,
      wechatOpenId = "test_openid",
      nickname = "testuser",
      phone = "13800138000",
      status = UserStatus.ACTIVE,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    userService = mockk()
    weChatAuthService = mockk()
    userRepository = mockk(relaxed = true)
    resolver =
      ClientUserQueryResolver(
        userService = userService,
        weChatAuthService = weChatAuthService,
        userRepository = userRepository,
      )

    userPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "testuser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )
  }

  @Test
  fun `me should return current user`() {
    // Given
    every { userRepository.findById(1L) } returns java.util.Optional.of(createSampleUser())

    // When
    val result = resolver.me(userPrincipal)

    // Then
    assertNotNull(result)
    assertEquals(1L, result.id)
    verify { userRepository.findById(1L) }
  }

  @Test
  fun `me should throw exception when user not found`() {
    // Given
    every { userRepository.findById(1L) } returns java.util.Optional.empty()

    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> { resolver.me(userPrincipal) }
    assertTrue(exception.message!!.contains("User not found"))
  }

  @Test
  fun `user should return user when requesting own id`() {
    // Given
    every { userRepository.findById(1L) } returns java.util.Optional.of(createSampleUser())

    // When
    val result = resolver.user(1L, userPrincipal)

    // Then
    assertNotNull(result)
    assertEquals(1L, result?.id)
  }

  @Test
  fun `user should throw exception when requesting other user id`() {
    // Given
    val otherUserPrincipal =
      UserPrincipal(
        id = 2L,
        _username = "otheruser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )

    // When & Then
    val exception = assertFailsWith<IllegalArgumentException> { resolver.user(1L, otherUserPrincipal) }
    assertTrue(exception.message!!.contains("无权查看"))
  }

  @Test
  fun `updateProfile should update user profile`() {
    // Given
    val input = UpdateProfileInput(nickname = "新昵称", avatar = "http://example.com/avatar.jpg")
    val updatedUser =
      createSampleUser().also {
        it.nickname = "新昵称"
        it.avatarUrl = "http://example.com/avatar.jpg"
      }
    every { userService.updateUserInfo(1L, "新昵称", null, "http://example.com/avatar.jpg") } returns updatedUser

    // When
    val result = resolver.updateProfile(input, userPrincipal)

    // Then
    assertNotNull(result)
    verify { userService.updateUserInfo(1L, "新昵称", null, "http://example.com/avatar.jpg") }
  }

  @Test
  fun `updateProfile should throw exception when not authenticated`() {
    // Given
    val input = UpdateProfileInput(nickname = "新昵称", avatar = null)
    val nullPrincipal: UserPrincipal? = null

    // When & Then
    val exception = assertFailsWith<IllegalStateException> { resolver.updateProfile(input, nullPrincipal) }
    assertTrue(exception.message!!.contains("请先登录"))
  }

  @Test
  fun `getUserOrderStatistics should return default statistics`() {
    // When
    val result = resolver.getUserOrderStatistics(userPrincipal)

    // Then
    assertEquals(0L, result.totalOrders)
    assertEquals(0L, result.completedOrders)
    assertEquals(0L, result.cancelledOrders)
  }

  @Test
  fun `canUserReview should check review eligibility`() {
    // Given
    every { userRepository.canUserReview(1L, 100L) } returns true

    // When
    val result = resolver.canUserReview(100L, userPrincipal)

    // Then
    assertTrue(result)
    verify { userRepository.canUserReview(1L, 100L) }
  }

  @Test
  fun `getUserPreferences should return default preferences`() {
    // When
    val result = resolver.getUserPreferences(userPrincipal)

    // Then
    assertEquals("zh-CN", result.language)
    assertEquals("Asia/Shanghai", result.timezone)
    assertEquals("CNY", result.currency)
    assertTrue(result.notifications["push"]!!)
  }
}
