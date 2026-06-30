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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RegionServiceTest {
  private lateinit var regionRepository: RegionRepository
  private lateinit var regionService: RegionService

  private fun createSampleProvince(): RegionModel {
    return RegionModel(
      id = 1L,
      name = "广东省",
      code = "440000",
      parentCode = null,
      level = 1,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )
  }

  private fun createSampleCity(): RegionModel {
    return RegionModel(
      id = 2L,
      name = "深圳市",
      code = "440300",
      parentCode = "440000",
      level = 2,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )
  }

  private fun createSampleDistrict(): RegionModel {
    return RegionModel(
      id = 3L,
      name = "南山区",
      code = "440305",
      parentCode = "440300",
      level = 3,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )
  }

  private fun createUpdatedRegion(original: RegionModel, name: String? = null): RegionModel {
    val updated = RegionModel(
      id = original.id,
      name = name ?: original.name,
      code = original.code,
      parentCode = original.parentCode,
      level = original.level,
      createdAt = original.createdAt,
      updatedAt = LocalDateTime.now(),
    )
    return updated
  }

  @BeforeEach
  fun setUp() {
    regionRepository = mockk(relaxed = true)
    regionService = RegionService(regionRepository)
  }

  @Test
  fun `createRegion should create new region successfully`() {
    // Given
    val sampleProvince = createSampleProvince()
    val input = CreateRegionInput(
      name = "东莞市",
      code = "441900",
      level = 2,
      parentCode = "440000",
    )
    every { regionRepository.existsByCode("441900") } returns false
    every { regionRepository.findByCode("440000") } returns sampleProvince
    every { regionRepository.existsByNameAndLevelAndParentCode("东莞市", 2, "440000") } returns false
    every { regionRepository.save(any()) } returns RegionModel(
      id = 1L,
      name = "东莞市",
      code = "441900",
      parentCode = "440000",
      level = 2,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

    // When
    val result = regionService.createRegion(input)

    // Then
    assertNotNull(result)
    verify { regionRepository.save(any()) }
  }

  @Test
  fun `createRegion should throw exception when code already exists`() {
    // Given
    val input = CreateRegionInput(
      name = "深圳市",
      code = "440300",
      level = 2,
      parentCode = "440000",
    )
    every { regionRepository.existsByCode("440300") } returns true

    // When & Then
    val exception = assertThrows<AquaException> { regionService.createRegion(input) }
    assertTrue(exception.message!!.contains("代码已存在"))
  }

  @Test
  fun `createRegion should throw exception when level is invalid`() {
    // Given
    val input = CreateRegionInput(
      name = "测试",
      code = "440000",
      level = 5,
      parentCode = null,
    )

    // When & Then
    val exception = assertThrows<AquaException> { regionService.createRegion(input) }
    assertTrue(exception.message!!.contains("层级必须在"))
  }

  @Test
  fun `createRegion should throw exception when parent region does not exist`() {
    // Given
    val input = CreateRegionInput(
      name = "测试",
      code = "441900",
      level = 2,
      parentCode = "440000",
    )
    every { regionRepository.existsByCode("441900") } returns false
    every { regionRepository.findByCode("440000") } returns null

    // When & Then
    val exception = assertThrows<AquaException> { regionService.createRegion(input) }
    assertTrue(exception.message!!.contains("父级地区不存在"))
  }

  @Test
  fun `createRegion should throw exception when name already exists under same parent`() {
    // Given
    val sampleProvince = createSampleProvince()
    val input = CreateRegionInput(
      name = "深圳市",
      code = "441900",
      level = 2,
      parentCode = "440000",
    )
    every { regionRepository.existsByCode("441900") } returns false
    every { regionRepository.findByCode("440000") } returns sampleProvince
    every { regionRepository.existsByNameAndLevelAndParentCode("深圳市", 2, "440000") } returns true

    // When & Then
    val exception = assertThrows<AquaException> { regionService.createRegion(input) }
    assertTrue(exception.message!!.contains("同名地区"))
  }

  @Test
  fun `updateRegion should update region name successfully`() {
    // Given
    val sampleCity = createSampleCity()
    every { regionRepository.findByCode("440300") } returns sampleCity
    every { regionRepository.existsByNameAndLevelAndParentCode(any(), any(), any()) } returns false
    every { regionRepository.save(any()) } returns createSampleCity()

    val input = UpdateRegionInput(
      name = "新深圳市",
      parentCode = null,
    )

    // When
    val result = regionService.updateRegion("440300", input)

    // Then
    assertNotNull(result)
  }

  @Test
  fun `updateRegion should throw exception when region not found`() {
    // Given
    every { regionRepository.findByCode("999999") } returns null

    val input = UpdateRegionInput(
      name = "测试",
      parentCode = null,
    )

    // When & Then
    val exception = assertThrows<AquaException> { regionService.updateRegion("999999", input) }
    assertTrue(exception.message!!.contains("地区不存在"))
  }

  @Test
  fun `deleteRegion should delete region successfully`() {
    // Given
    val sampleCity = createSampleCity()
    every { regionRepository.findByCode("440300") } returns sampleCity
    every { regionRepository.findByParentCodeOrderByCode("440300") } returns emptyList()

    // When
    val result = regionService.deleteRegion("440300")

    // Then
    assertTrue(result)
    verify { regionRepository.delete(sampleCity) }
  }

  @Test
  fun `deleteRegion should throw exception when has children`() {
    // Given
    val sampleProvince = createSampleProvince()
    val sampleCity = createSampleCity()
    every { regionRepository.findByCode("440000") } returns sampleProvince
    every { regionRepository.findByParentCodeOrderByCode("440000") } returns listOf(sampleCity)

    // When & Then
    val exception = assertThrows<AquaException> { regionService.deleteRegion("440000") }
    assertTrue(exception.message!!.contains("存在子地区"))
  }

  @Test
  fun `getRegions should return all regions when no parameters provided`() {
    // Given
    val allRegions = listOf(createSampleProvince(), createSampleCity(), createSampleDistrict())
    every { regionRepository.findAll() } returns allRegions

    // When
    val result = regionService.getRegions(null, null)

    // Then
    assertEquals(3, result.size)
    verify { regionRepository.findAll() }
  }

  @Test
  fun `getRegions should return regions by level`() {
    // Given
    every { regionRepository.findRootRegions(1) } returns listOf(createSampleProvince())

    // When
    val result = regionService.getRegions(1, null)

    // Then
    assertEquals(1, result.size)
    verify { regionRepository.findRootRegions(1) }
  }

  @Test
  fun `getRegions should return regions by parent code and level`() {
    // Given
    every { regionRepository.findByParentCodeAndLevel("440000", 2) } returns listOf(createSampleCity())

    // When
    val result = regionService.getRegions(2, "440000")

    // Then
    assertEquals(1, result.size)
    verify { regionRepository.findByParentCodeAndLevel("440000", 2) }
  }

  @Test
  fun `getRegionByCode should return region when found`() {
    // Given
    every { regionRepository.findByCode("440300") } returns createSampleCity()

    // When
    val result = regionService.getRegionByCode("440300")

    // Then
    assertNotNull(result)
    assertEquals("440300", result?.code)
  }

  @Test
  fun `getRegionByCode should return null when not found`() {
    // Given
    every { regionRepository.findByCode("999999") } returns null

    // When
    val result = regionService.getRegionByCode("999999")

    // Then
    assertEquals(null, result)
  }

  @Test
  fun `searchRegions should search by keyword and level`() {
    // Given
    every { regionRepository.findByNameContainingAndLevelOrderByCode("深圳", 2) } returns listOf(createSampleCity())

    // When
    val result = regionService.searchRegions("深圳", 2)

    // Then
    assertEquals(1, result.size)
    verify { regionRepository.findByNameContainingAndLevelOrderByCode("深圳", 2) }
  }

  @Test
  fun `searchRegions should search by keyword only`() {
    // Given
    every { regionRepository.findByNameContainingOrderByCode("深圳") } returns listOf(createSampleCity(), createSampleDistrict())

    // When
    val result = regionService.searchRegions("深圳", null)

    // Then
    assertEquals(2, result.size)
    verify { regionRepository.findByNameContainingOrderByCode("深圳") }
  }
}