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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class AdminQueryResolverTest :
  FunSpec({
    var adminRepository: AdminRepository = mockk()
    var regionRepository: RegionRepository = mockk()
    var resolver: AdminQueryResolver = mockk()

    fun createSampleAdmin(): AdminModel =
      AdminModel(
        id = 1L,
        username = "admin",
        passwordHash = "hashed_password",
        phone = "13900139000",
        role = AdminRoleModel.ADMIN,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun createSampleRegion(): RegionModel =
      RegionModel(
        id = 1L,
        name = "广东省",
        code = "440000",
        parentCode = null,
        level = 1,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      adminRepository = mockk(relaxed = true)
      regionRepository = mockk(relaxed = true)
      resolver = AdminQueryResolver(adminRepository, regionRepository)
    }
    test("admins should return all admins") {
      // Given
      val admins = listOf(createSampleAdmin())
      every { adminRepository.findAllAdmins() } returns admins

      // When
      val result = resolver.admins()

      // Then
      result.size shouldBe 1
      verify { adminRepository.findAllAdmins() }
    }
    test("admins should return empty list when no admins") {
      // Given
      every { adminRepository.findAllAdmins() } returns emptyList()

      // When
      val result = resolver.admins()

      // Then
      result.isEmpty() shouldBe true
    }
    test("allRegions should return all regions") {
      // Given
      val regions = listOf(createSampleRegion())
      every { regionRepository.findAll() } returns regions

      // When
      val result = resolver.allRegions()

      // Then
      result.size shouldBe 1
      verify { regionRepository.findAll() }
    }
    test("allRegions should return empty list on error") {
      // Given
      every { regionRepository.findAll() } throws RuntimeException("Database error")

      // When
      val result = resolver.allRegions()

      // Then
      result.isEmpty() shouldBe true
    }
  })
