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

package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.common.domain.model.RegionModel
import dev.yidafu.aqua.user.domain.exception.AquaException
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

/**
 * 补充 RegionServiceTest 未覆盖的分支。
 */
class RegionServiceExtraTest :
  FunSpec({
    var regionRepository: RegionRepository = mockk()
    var service: RegionService = mockk()

    fun region(
      id: Long,
      name: String,
      code: String,
      level: Int,
      parentCode: String?,
    ): RegionModel =
      RegionModel(
        id = id,
        name = name,
        code = code,
        parentCode = parentCode,
        level = level,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      regionRepository = mockk(relaxed = true)
      service = RegionService(regionRepository)
    }
    test("createRegion for root level should use parent code zero") {
      every { regionRepository.existsByCode("440000") } returns false
      every { regionRepository.existsByNameAndLevelAndParentCode("广东省", 1, "0") } returns false
      every { regionRepository.save(any()) } answers { firstArg() }

      val result = service.createRegion(CreateRegionInput("广东省", "440000", 1, null))

      result.parentCode shouldBe "0"
    }
    test("createRegion should throw when non-root has no parent code") {
      every { regionRepository.existsByCode("440300") } returns false
      val exception =
        shouldThrow<AquaException> {
          service.createRegion(CreateRegionInput("深圳市", "440300", 2, null))
        }
      exception.message!!.contains("父级代码") shouldBe true
    }
    test("createRegion should throw when parent level is incorrect") {
      every { regionRepository.existsByCode("440300") } returns false
      every { regionRepository.findByCode("440000") } returns region(1L, "广东省", "440000", 2, null)
      val exception =
        shouldThrow<AquaException> {
          service.createRegion(CreateRegionInput("深圳市", "440300", 2, "440000"))
        }
      exception.message!!.contains("父级地区层级不正确") shouldBe true
    }
    test("updateRegion should throw when parent level is incorrect") {
      val city = region(2L, "深圳市", "440300", 2, "440000")
      every { regionRepository.findByCode("440300") } returns city
      every { regionRepository.findByCode("999999") } returns region(9L, "其他", "999999", 2, null)
      val exception =
        shouldThrow<AquaException> {
          service.updateRegion("440300", UpdateRegionInput(null, "999999"))
        }
      exception.message!!.contains("父级地区层级不正确") shouldBe true
    }
    test("updateRegion should throw for root parent code change") {
      val province = region(1L, "广东省", "440000", 1, "0")
      every { regionRepository.findByCode("440000") } returns province
      val exception =
        shouldThrow<AquaException> {
          service.updateRegion("440000", UpdateRegionInput(null, "999999"))
        }
      exception.message!!.contains("父代码必须为 '0'") shouldBe true
    }
    test("updateRegion should throw when new parent not found") {
      val city = region(2L, "深圳市", "440300", 2, "440000")
      every { regionRepository.findByCode("440300") } returns city
      every { regionRepository.findByCode("999999") } returns null
      val exception =
        shouldThrow<AquaException> {
          service.updateRegion("440300", UpdateRegionInput(null, "999999"))
        }
      exception.message!!.contains("父级地区不存在") shouldBe true
    }
    test("updateRegion should throw when region has children") {
      val city = region(2L, "深圳市", "440300", 2, "440000")
      every { regionRepository.findByCode("440300") } returns city
      every { regionRepository.findByCode("440100") } returns region(1L, "广州市", "440100", 1, null)
      every { regionRepository.findByParentCodeOrderByCode("440300") } returns
        listOf(region(3L, "南山区", "440305", 3, "440300"))
      val exception =
        shouldThrow<AquaException> {
          service.updateRegion("440300", UpdateRegionInput(null, "440100"))
        }
      exception.message!!.contains("存在子地区") shouldBe true
    }
    test("updateRegion should throw on duplicate name under new parent") {
      val city = region(2L, "深圳市", "440300", 2, "440000")
      every { regionRepository.findByCode("440300") } returns city
      every { regionRepository.findByCode("440100") } returns region(1L, "广州市", "440100", 1, null)
      every { regionRepository.findByParentCodeOrderByCode("440300") } returns emptyList()
      every { regionRepository.existsByNameAndLevelAndParentCode("深圳市", 2, "440100") } returns true
      val exception =
        shouldThrow<AquaException> {
          service.updateRegion("440300", UpdateRegionInput(null, "440100"))
        }
      exception.message!!.contains("同名地区") shouldBe true
    }
    test("updateRegion should return original region when no changes") {
      val city = region(2L, "深圳市", "440300", 2, "440000")
      every { regionRepository.findByCode("440300") } returns city
      val result = service.updateRegion("440300", UpdateRegionInput(null, null))
      result shouldBe city
    }
    test("updateRegion should throw on duplicate name under same parent") {
      val city = region(2L, "深圳市", "440300", 2, "440000")
      every { regionRepository.findByCode("440300") } returns city
      every { regionRepository.existsByNameAndLevelAndParentCode("新深圳市", 2, "440000") } returns true
      val exception =
        shouldThrow<AquaException> {
          service.updateRegion("440300", UpdateRegionInput("新深圳市", null))
        }
      exception.message!!.contains("同名地区") shouldBe true
    }
    test("deleteRegion should throw when region not found") {
      every { regionRepository.findByCode("999999") } returns null
      val exception = shouldThrow<AquaException> { service.deleteRegion("999999") }
      exception.message!!.contains("地区不存在") shouldBe true
    }
  })
