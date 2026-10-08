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

import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.user.domain.repository.UserRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

class UserQueryApiServiceImplTest :
  FunSpec({
    var userRepository: UserRepository = mockk()
    var service: UserQueryApiServiceImpl = mockk()

    fun user(
      id: Long,
      createdAt: LocalDateTime = LocalDateTime.now(),
    ): UserModel =
      UserModel(
        id = id,
        wechatOpenId = "openid_$id",
        nickname = "user$id",
        email = "e$id@e.com",
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        createdAt = createdAt,
        updatedAt = createdAt,
      )

    beforeTest {
      userRepository = mockk(relaxed = true)
      service = UserQueryApiServiceImpl(userRepository)
    }
    test("findAll should delegate to repository") {
      every { userRepository.findAll() } returns listOf(user(1L), user(2L))
      service.findAll().size shouldBe 2
      verify { userRepository.findAll() }
    }
    test("countAll should delegate to count") {
      every { userRepository.count() } returns 5L
      service.countAll() shouldBe 5L
      verify(exactly = 1) { userRepository.count() }
    }
    test("findById should return user when present") {
      every { userRepository.findById(1L) } returns Optional.of(user(1L))
      service.findById(1L).shouldNotBeNull()
    }
    test("findById should return null when absent") {
      every { userRepository.findById(99L) } returns Optional.empty()
      service.findById(99L).shouldBeNull()
    }
    test("findByIds should delegate") {
      every { userRepository.findAllById(listOf(1L, 2L)) } returns listOf(user(1L), user(2L))
      service.findByIds(listOf(1L, 2L)).size shouldBe 2
    }
    test("findByPhone should delegate") {
      every { userRepository.findByPhone("138") } returns user(1L)
      service.findByPhone("138")?.id shouldBe 1L
    }
    test("existsById should delegate") {
      every { userRepository.existsById(1L) } returns true
      service.existsById(1L) shouldBe true
      every { userRepository.existsById(2L) } returns false
      service.existsById(2L) shouldBe false
    }
    test("findUsersCreatedAfter should delegate to derived query") {
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val created = LocalDateTime.of(2025, 6, 1, 0, 0)
      every { userRepository.findByCreatedAtGreaterThan(start) } returns listOf(user(1L, created))
      val result = service.findUsersCreatedAfter(start)
      result.map { it.id } shouldBe listOf(1L)
      verify(exactly = 1) { userRepository.findByCreatedAtGreaterThan(start) }
    }
    test("findUsersCreatedBetween should delegate to derived query") {
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val end = LocalDateTime.of(2025, 12, 31, 0, 0)
      val created = LocalDateTime.of(2025, 6, 1, 0, 0)
      every { userRepository.findByCreatedAtGreaterThanAndCreatedAtLessThan(start, end) } returns listOf(user(1L, created))
      val result = service.findUsersCreatedBetween(start, end)
      result.map { it.id } shouldBe listOf(1L)
      verify(exactly = 1) { userRepository.findByCreatedAtGreaterThanAndCreatedAtLessThan(start, end) }
    }
    test("countUsersCreatedBetween should delegate to conditional count") {
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val end = LocalDateTime.of(2025, 12, 31, 0, 0)
      every { userRepository.countByCreatedAtGreaterThanAndCreatedAtLessThan(start, end) } returns 3L
      service.countUsersCreatedBetween(start, end) shouldBe 3L
      verify(exactly = 1) { userRepository.countByCreatedAtGreaterThanAndCreatedAtLessThan(start, end) }
    }
    test("findByCustomerGroupId should delegate") {
      every { userRepository.findByCustomerGroupId(3L) } returns listOf(user(1L))
      service.findByCustomerGroupId(3L).size shouldBe 1
    }
    test("countUsersByCustomerGroupId should delegate") {
      every { userRepository.countByCustomerGroupId(3L) } returns 7L
      service.countUsersByCustomerGroupId(3L) shouldBe 7L
    }
  })
