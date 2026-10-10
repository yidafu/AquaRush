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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

class DebugJwtControllerTest :
  FunSpec({
    var jwtTokenService: JwtTokenService = mockk()
    var userRepository: UserRepository = mockk()
    var controller: DebugJwtController = mockk()

    fun user(): UserModel =
      UserModel(
        id = 1L,
        wechatOpenId = "openid-1",
        nickname = "用户甲",
        phone = "13900139000",
        email = "a@b.com",
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        totalSpentCents = 5000L,
        createdAt = LocalDateTime.of(2025, 1, 1, 0, 0),
        lastLoginAt = LocalDateTime.of(2025, 1, 2, 0, 0),
      )

    beforeTest {
      jwtTokenService = mockk(relaxed = true)
      userRepository = mockk(relaxed = true)
      controller = DebugJwtController(jwtTokenService, userRepository)
    }
    test("generateJwtByUserId should generate token for existing user") {
      // Given
      every { userRepository.findById(1L) } returns Optional.of(user())
      every { jwtTokenService.generateAccessToken(any()) } returns "jwt-token-1"

      // When
      val result = controller.generateJwtByUserId(JwtGenerationRequest(userId = 1L))

      // Then
      result.body!!.success shouldBe true
      val data = result.body!!.data!!
      data.token shouldBe "jwt-token-1"
      data.userId shouldBe 1L
      data.username shouldBe "openid-1"
      data.authorities.contains("ROLE_USER") shouldBe true
    }
    test("generateJwtByUserId should return error for missing user") {
      // Given
      every { userRepository.findById(999L) } returns Optional.empty()

      // When
      val result = controller.generateJwtByUserId(JwtGenerationRequest(userId = 999L))

      // Then
      result.body!!.success shouldBe false
      result.body!!.message!!.contains("not found") shouldBe true
    }
    test("getUserById should return user debug info") {
      // Given
      every { userRepository.findById(1L) } returns Optional.of(user())

      // When
      val result = controller.getUserById(1L)

      // Then
      result.body!!.success shouldBe true
      result.body!!.data!!.nickname shouldBe "用户甲"
    }
    test("getUserById should return error for missing user") {
      // Given
      every { userRepository.findById(999L) } returns Optional.empty()

      // When
      val result = controller.getUserById(999L)

      // Then
      result.body!!.success shouldBe false
    }
    test("listUsers should return all users") {
      // Given
      every { userRepository.findAll(any<Pageable>()) } returns PageImpl(listOf(user()))

      // When
      val result = controller.listUsers(page = 0, size = 20)

      // Then
      result.body!!.success shouldBe true
      result.body!!.data!!.size shouldBe 1
      result.body!!.data!![0].wechatOpenId shouldBe "openid-1"
      verify { userRepository.findAll(any<Pageable>()) }
    }
    test("listUsers should return error on failure") {
      // Given
      every { userRepository.findAll(any<Pageable>()) } throws IllegalStateException("db down")

      // When
      val result = controller.listUsers(page = 0, size = 20)

      // Then
      result.body!!.success shouldBe false
    }
  })
