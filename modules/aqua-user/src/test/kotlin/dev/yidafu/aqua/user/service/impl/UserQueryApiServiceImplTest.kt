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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserQueryApiServiceImplTest {
  private lateinit var userRepository: UserRepository
  private lateinit var service: UserQueryApiServiceImpl

  private fun user(
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

  @BeforeEach
  fun setUp() {
    userRepository = mockk(relaxed = true)
    service = UserQueryApiServiceImpl(userRepository)
  }

  @Test
  fun `findAll should delegate to repository`() {
    every { userRepository.findAll() } returns listOf(user(1L), user(2L))
    assertEquals(2, service.findAll().size)
    verify { userRepository.findAll() }
  }

  @Test
  fun `findById should return user when present`() {
    every { userRepository.findById(1L) } returns Optional.of(user(1L))
    assertNotNull(service.findById(1L))
  }

  @Test
  fun `findById should return null when absent`() {
    every { userRepository.findById(99L) } returns Optional.empty()
    assertNull(service.findById(99L))
  }

  @Test
  fun `findByIds should delegate`() {
    every { userRepository.findAllById(listOf(1L, 2L)) } returns listOf(user(1L), user(2L))
    assertEquals(2, service.findByIds(listOf(1L, 2L)).size)
  }

  @Test
  fun `findByPhone should delegate`() {
    every { userRepository.findByPhone("138") } returns user(1L)
    assertEquals(1L, service.findByPhone("138")?.id)
  }

  @Test
  fun `existsById should delegate`() {
    every { userRepository.existsById(1L) } returns true
    assertTrue(service.existsById(1L))
    every { userRepository.existsById(2L) } returns false
    assertFalse(service.existsById(2L))
  }

  @Test
  fun `findUsersCreatedAfter should filter users`() {
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    every { userRepository.findAll() } returns
      listOf(
        user(1L, LocalDateTime.of(2025, 6, 1, 0, 0)),
        user(2L, LocalDateTime.of(2024, 6, 1, 0, 0)),
      )
    val result = service.findUsersCreatedAfter(start)
    assertEquals(listOf(1L), result.map { it.id })
  }

  @Test
  fun `findUsersCreatedBetween should filter users`() {
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 12, 31, 0, 0)
    every { userRepository.findAll() } returns
      listOf(
        user(1L, LocalDateTime.of(2025, 6, 1, 0, 0)),
        user(2L, LocalDateTime.of(2026, 1, 1, 0, 0)),
        user(3L, LocalDateTime.of(2024, 1, 1, 0, 0)),
      )
    val result = service.findUsersCreatedBetween(start, end)
    assertEquals(listOf(1L), result.map { it.id })
  }

  @Test
  fun `countUsersCreatedBetween should delegate to count`() {
    every { userRepository.count() } returns 5L
    assertEquals(5L, service.countUsersCreatedBetween(LocalDateTime.now(), LocalDateTime.now()))
  }

  @Test
  fun `findByCustomerGroupId should delegate`() {
    every { userRepository.findByCustomerGroupId(3L) } returns listOf(user(1L))
    assertEquals(1, service.findByCustomerGroupId(3L).size)
  }

  @Test
  fun `countUsersByCustomerGroupId should delegate`() {
    every { userRepository.countByCustomerGroupId(3L) } returns 7L
    assertEquals(7L, service.countUsersByCustomerGroupId(3L))
  }
}
