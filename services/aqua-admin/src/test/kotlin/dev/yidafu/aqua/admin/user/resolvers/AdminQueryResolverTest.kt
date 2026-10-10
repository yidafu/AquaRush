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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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
    test("admins should return paged admins") {
      // Given
      val admins = listOf(createSampleAdmin())
      val pageable = PageRequest.of(0, 20)
      every { adminRepository.findAllAdmins(null, null, pageable) } returns PageImpl(admins, pageable, 1)

      // When
      val result = resolver.admins(keyword = null, role = null, page = 0, size = 20)

      // Then
      result.content.size shouldBe 1
      result.totalElements shouldBe 1
      verify { adminRepository.findAllAdmins(null, null, pageable) }
    }
    test("admins should return empty page when no admins") {
      // Given
      val pageable = PageRequest.of(0, 20)
      every { adminRepository.findAllAdmins(null, null, pageable) } returns PageImpl(emptyList(), pageable, 0)

      // When
      val result = resolver.admins(keyword = null, role = null, page = 0, size = 20)

      // Then
      result.content.isEmpty() shouldBe true
      result.totalElements shouldBe 0
    }
    test("regions(parentCode=null) should return root provinces") {
      // Given
      val regions = listOf(createSampleRegion())
      every { regionRepository.findRootRegions(1) } returns regions

      // When
      val result = resolver.regions(parentCode = null)

      // Then
      result.size shouldBe 1
      verify { regionRepository.findRootRegions(1) }
    }
    test("regions(parentCode) should return children of the given parent") {
      // Given
      val regions = listOf(createSampleRegion())
      every { regionRepository.findByParentCodeOrderByCode("440000") } returns regions

      // When
      val result = resolver.regions(parentCode = "440000")

      // Then
      result.size shouldBe 1
      verify { regionRepository.findByParentCodeOrderByCode("440000") }
    }
    test("regions should return empty list on error") {
      // Given
      every { regionRepository.findRootRegions(1) } throws RuntimeException("Database error")

      // When
      val result = resolver.regions(parentCode = null)

      // Then
      result.isEmpty() shouldBe true
    }
  })
