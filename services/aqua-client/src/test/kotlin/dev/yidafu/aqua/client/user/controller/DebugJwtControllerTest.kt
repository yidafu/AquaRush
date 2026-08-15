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

import dev.yidafu.aqua.client.user.dto.JwtGenerationRequest
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DebugJwtControllerTest {
  private lateinit var jwtTokenService: JwtTokenService
  private lateinit var userRepository: UserRepository
  private lateinit var controller: DebugJwtController

  private fun user(): UserModel =
    UserModel(
      id = 1L,
      wechatOpenId = "openid-1",
      nickname = "用户甲",
      phone = "13900139000",
      email = "a@b.com",
      status = UserStatus.ACTIVE,
      role = UserRole.USER,
      balanceCents = 1000L,
      totalSpentCents = 5000L,
      createdAt = LocalDateTime.of(2025, 1, 1, 0, 0),
      lastLoginAt = LocalDateTime.of(2025, 1, 2, 0, 0),
    )

  @BeforeEach
  fun setUp() {
    jwtTokenService = mockk(relaxed = true)
    userRepository = mockk(relaxed = true)
    controller = DebugJwtController(jwtTokenService, userRepository)
  }

  @Test
  fun `generateJwtByUserId should generate token for existing user`() {
    // Given
    every { userRepository.findById(1L) } returns Optional.of(user())
    every { jwtTokenService.generateAccessToken(any()) } returns "jwt-token-1"

    // When
    val result = controller.generateJwtByUserId(JwtGenerationRequest(userId = 1L))

    // Then
    assertTrue(result.body!!.success)
    val data = result.body!!.data!!
    assertEquals("jwt-token-1", data.token)
    assertEquals(1L, data.userId)
    assertEquals("openid-1", data.username)
    assertTrue(data.authorities.contains("ROLE_USER"))
  }

  @Test
  fun `generateJwtByUserId should return error for missing user`() {
    // Given
    every { userRepository.findById(999L) } returns Optional.empty()

    // When
    val result = controller.generateJwtByUserId(JwtGenerationRequest(userId = 999L))

    // Then
    assertFalse(result.body!!.success)
    assertTrue(result.body!!.message!!.contains("not found"))
  }

  @Test
  fun `getUserById should return user debug info`() {
    // Given
    every { userRepository.findById(1L) } returns Optional.of(user())

    // When
    val result = controller.getUserById(1L)

    // Then
    assertTrue(result.body!!.success)
    assertEquals("用户甲", result.body!!.data!!.nickname)
    assertEquals(1000L, result.body!!.data!!.balance)
  }

  @Test
  fun `getUserById should return error for missing user`() {
    // Given
    every { userRepository.findById(999L) } returns Optional.empty()

    // When
    val result = controller.getUserById(999L)

    // Then
    assertFalse(result.body!!.success)
  }

  @Test
  fun `listUsers should return all users`() {
    // Given
    every { userRepository.findAll() } returns listOf(user())

    // When
    val result = controller.listUsers()

    // Then
    assertTrue(result.body!!.success)
    assertEquals(1, result.body!!.data!!.size)
    assertEquals("openid-1", result.body!!.data!![0].wechatOpenId)
    verify { userRepository.findAll() }
  }

  @Test
  fun `listUsers should return error on failure`() {
    // Given
    every { userRepository.findAll() } throws IllegalStateException("db down")

    // When
    val result = controller.listUsers()

    // Then
    assertFalse(result.body!!.success)
  }
}
