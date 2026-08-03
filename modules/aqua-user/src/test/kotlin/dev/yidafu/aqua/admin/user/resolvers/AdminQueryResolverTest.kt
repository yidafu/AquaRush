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

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdminQueryResolverTest {
  private lateinit var adminRepository: AdminRepository
  private lateinit var regionRepository: RegionRepository
  private lateinit var resolver: AdminQueryResolver

  private fun createSampleAdmin(): AdminModel =
    AdminModel(
      id = 1L,
      username = "admin",
      passwordHash = "hashed_password",
      phone = "13900139000",
      role = AdminRoleModel.ADMIN,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun createSampleRegion(): RegionModel =
    RegionModel(
      id = 1L,
      name = "广东省",
      code = "440000",
      parentCode = null,
      level = 1,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    adminRepository = mockk(relaxed = true)
    regionRepository = mockk(relaxed = true)
    resolver = AdminQueryResolver(adminRepository, regionRepository)
  }

  @Test
  fun `admins should return all admins`() {
    // Given
    val admins = listOf(createSampleAdmin())
    every { adminRepository.findAllAdmins() } returns admins

    // When
    val result = resolver.admins()

    // Then
    assertEquals(1, result.size)
    verify { adminRepository.findAllAdmins() }
  }

  @Test
  fun `admins should return empty list when no admins`() {
    // Given
    every { adminRepository.findAllAdmins() } returns emptyList()

    // When
    val result = resolver.admins()

    // Then
    assertTrue(result.isEmpty())
  }

  @Test
  fun `admin should return admin when found`() {
    // Given
    every { adminRepository.findAdminById(1L) } returns createSampleAdmin()

    // When
    val result = resolver.admin(1L)

    // Then
    assertNotNull(result)
    assertEquals(1L, result?.id)
  }

  @Test
  fun `admin should return null when not found`() {
    // Given
    every { adminRepository.findAdminById(999L) } returns null

    // When
    val result = resolver.admin(999L)

    // Then
    assertNull(result)
  }

  @Test
  fun `allRegions should return all regions`() {
    // Given
    val regions = listOf(createSampleRegion())
    every { regionRepository.findAll() } returns regions

    // When
    val result = resolver.allRegions()

    // Then
    assertEquals(1, result.size)
    verify { regionRepository.findAll() }
  }

  @Test
  fun `allRegions should return empty list on error`() {
    // Given
    every { regionRepository.findAll() } throws RuntimeException("Database error")

    // When
    val result = resolver.allRegions()

    // Then
    assertTrue(result.isEmpty())
  }
}
