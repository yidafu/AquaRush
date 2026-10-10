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

import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import dev.yidafu.aqua.user.mapper.toGraphQL
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.time.LocalDateTime

class RegionQueryResolverTest :
  FunSpec({
    var regionRepository: RegionRepository = mockk()
    var regionQueryResolver: RegionQueryResolver = mockk()
    var userPrincipal: UserPrincipal = mockk()
    val sampleProvince =
      RegionModel(
        id = 1L,
        name = "广东省",
        code = "440000",
        parentCode = null,
        level = 1,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val sampleCity =
      RegionModel(
        id = 2L,
        name = "深圳市",
        code = "440300",
        parentCode = "440000",
        level = 2,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val sampleDistrict =
      RegionModel(
        id = 3L,
        name = "南山区",
        code = "440305",
        parentCode = "440300",
        level = 3,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )
    val anotherCity =
      RegionModel(
        id = 4L,
        name = "广州市",
        code = "440100",
        parentCode = "440000",
        level = 2,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    // Test data

    beforeTest {
      regionRepository = mockk()
      regionQueryResolver =
        RegionQueryResolver(
          regionRepository = regionRepository,
        )

      userPrincipal =
        UserPrincipal(
          id = 123L,
          _username = "testuser",
          userType = "USER",
          _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
        )
    }
    test("should return root provinces when no parameters provided(规则 10:不再整表返回)") {
      // Given
      every { regionRepository.findRootRegions(1) } returns listOf(sampleProvince)

      // When
      val result = regionQueryResolver.regions(null, null, userPrincipal)

      // Then
      result.size shouldBe 1
      result shouldBe listOf(sampleProvince.toGraphQL())
      verify { regionRepository.findRootRegions(1) }
    }
    test("should return regions by level when only level provided") {
      // Given
      every { regionRepository.findRootRegions(1) } returns listOf(sampleProvince)

      // When
      val result = regionQueryResolver.regions(1, null, userPrincipal)

      // Then
      result.size shouldBe 1
      result[0] shouldBe sampleProvince.toGraphQL()
      verify { regionRepository.findRootRegions(1) }
    }
    test("should return root regions when level is 1") {
      // Given
      every { regionRepository.findRootRegions(1) } returns listOf(sampleProvince)

      // When
      val result = regionQueryResolver.regions(1, null, userPrincipal)

      // Then
      result.size shouldBe 1
      result[0] shouldBe sampleProvince.toGraphQL()
      verify { regionRepository.findRootRegions(1) }
    }
    test("should call findByLevel for levels other than 1") {
      // Given
      every { regionRepository.findByLevel(2) } returns listOf(sampleCity, anotherCity)

      // When
      val result = regionQueryResolver.regions(2, null, userPrincipal)

      // Then
      result.size shouldBe 2
      verify { regionRepository.findByLevel(2) }
    }
    test("should return regions by level and parent code when both provided") {
      // Given
      every { regionRepository.findByParentCodeAndLevel("440000", 2) } returns listOf(sampleCity, anotherCity)

      // When
      val result = regionQueryResolver.regions(2, "440000", userPrincipal)

      // Then
      result.size shouldBe 2
      result[0] shouldBe sampleCity.toGraphQL()
      result[1] shouldBe anotherCity.toGraphQL()
      verify { regionRepository.findByParentCodeAndLevel("440000", 2) }
    }
    test("should return region by code") {
      // Given
      every { regionRepository.findByCode("440305") } returns sampleDistrict

      // When
      val result = regionQueryResolver.region("440305", userPrincipal)

      // Then
      result.shouldNotBeNull()
      result shouldBe sampleDistrict.toGraphQL()
      verify { regionRepository.findByCode("440305") }
    }
    test("should return null when region code not found") {
      // Given
      every { regionRepository.findByCode("999999") } returns null

      // When
      val result = regionQueryResolver.region("999999", userPrincipal)

      // Then
      result.shouldBeNull()
      verify { regionRepository.findByCode("999999") }
    }
    test("should work with null user principal") {
      // Given
      every { regionRepository.findRootRegions(1) } returns listOf(sampleProvince)

      // When
      val result = regionQueryResolver.regions(null, null, null)

      // Then
      result.size shouldBe 1
      result[0] shouldBe sampleProvince.toGraphQL()
      verify { regionRepository.findRootRegions(1) }
    }
    test("should handle regions query with mixed parameters correctly") {
      // Given
      val cities = listOf(sampleCity, anotherCity)
      every { regionRepository.findByParentCodeAndLevel("440000", 2) } returns cities

      // When
      val result = regionQueryResolver.regions(2, "440000", null)

      // Then
      result.size shouldBe 2
      result shouldBe cities.map { it.toGraphQL() }
      verify { regionRepository.findByParentCodeAndLevel("440000", 2) }
    }
    test("should return empty list when no regions found for given criteria") {
      // Given
      every { regionRepository.findByLevel(99) } returns emptyList()

      // When
      val result = regionQueryResolver.regions(99, null, userPrincipal)

      // Then
      result.size shouldBe 0
      verify { regionRepository.findByLevel(99) }
    }
    test("should handle level 0 correctly") {
      // Given
      every { regionRepository.findByLevel(0) } returns listOf(sampleProvince, sampleCity)

      // When
      val result = regionQueryResolver.regions(0, null, userPrincipal)

      // Then
      result.size shouldBe 2
      verify { regionRepository.findByLevel(0) }
    }
  })
