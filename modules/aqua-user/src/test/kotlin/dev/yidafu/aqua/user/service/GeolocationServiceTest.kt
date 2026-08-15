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

import dev.yidafu.aqua.common.domain.model.AddressModel
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeolocationServiceTest {
  private lateinit var regionRepository: RegionRepository
  private lateinit var externalGeocodingService: ExternalGeocodingService
  private lateinit var service: GeolocationService

  private fun region(
    code: String,
    name: String,
    level: Int,
    parentCode: String? = null,
  ): RegionModel =
    RegionModel(
      id = code.hashCode().toLong(),
      name = name,
      code = code,
      parentCode = parentCode,
      level = level,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun address(
    provinceCode: String? = "440000",
    cityCode: String? = "440300",
    districtCode: String? = "440305",
    detailAddress: String = "科技园路1号",
  ): AddressModel =
    AddressModel(
      id = 1L,
      userId = 1L,
      receiverName = "张三",
      phone = "13800138000",
      province = "广东省",
      city = "深圳市",
      district = "南山区",
      provinceCode = provinceCode ?: "",
      cityCode = cityCode ?: "",
      districtCode = districtCode ?: "",
      detailAddress = detailAddress,
      isDefault = true,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    regionRepository = mockk(relaxed = true)
    externalGeocodingService = mockk(relaxed = true)
    service = GeolocationService(regionRepository, externalGeocodingService)
  }

  @Test
  fun `geocode should return coordinates`() {
    every { externalGeocodingService.geocode(any()) } returns Pair(114.05, 22.54)
    val result = service.geocode(address())
    assertEquals(114.05, result?.first)
    assertEquals(22.54, result?.second)
  }

  @Test
  fun `geocode should return null on external failure`() {
    every { externalGeocodingService.geocode(any()) } throws RuntimeException("down")
    assertNull(service.geocode(address()))
  }

  @Test
  fun `reverseGeocode should return result`() {
    val result = GeocodingResult("广东省", "440000", "深圳市", "440300", "南山区", "440305", "街", "地址", 114.0, 22.0, 0.9)
    every { externalGeocodingService.reverseGeocode(114.0, 22.0) } returns result
    assertEquals("广东省", service.reverseGeocode(114.0, 22.0)?.province)
  }

  @Test
  fun `reverseGeocode should return null on external failure`() {
    every { externalGeocodingService.reverseGeocode(any(), any()) } throws RuntimeException("down")
    assertNull(service.reverseGeocode(114.0, 22.0))
  }

  @Test
  fun `validateAndNormalizeAddress should normalize valid address`() {
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
    every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "440300")

    val result = service.validateAndNormalizeAddress(address(detailAddress = "  科技园路 1 号  "))
    assertEquals("广东省", result.province)
    assertEquals("深圳市", result.city)
    assertEquals("南山区", result.district)
    assertNotNull(result.detailAddress)
  }

  @Test
  fun `validateAndNormalizeAddress should throw for invalid province`() {
    every { regionRepository.findByCode("440000") } returns null
    val exception = assertThrows<AquaException> { service.validateAndNormalizeAddress(address()) }
    assertTrue(exception.message!!.contains("无效的省份代码"))
  }

  @Test
  fun `validateAndNormalizeAddress should throw for invalid city level`() {
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 1)
    val exception = assertThrows<AquaException> { service.validateAndNormalizeAddress(address()) }
    assertTrue(exception.message!!.contains("无效的城市代码"))
  }

  @Test
  fun `validateAndNormalizeAddress should throw for invalid district level`() {
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
    every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 1)
    val exception = assertThrows<AquaException> { service.validateAndNormalizeAddress(address()) }
    assertTrue(exception.message!!.contains("无效的区县代码"))
  }

  @Test
  fun `validateAndNormalizeAddress should throw for mismatched city parent`() {
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "999999")
    every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "440300")
    val exception = assertThrows<AquaException> { service.validateAndNormalizeAddress(address()) }
    assertTrue(exception.message!!.contains("城市与省份不匹配"))
  }

  @Test
  fun `validateAndNormalizeAddress should throw for mismatched district parent`() {
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
    every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "999999")
    val exception = assertThrows<AquaException> { service.validateAndNormalizeAddress(address()) }
    assertTrue(exception.message!!.contains("区县与城市不匹配"))
  }

  @Test
  fun `validateAndNormalizeAddress with empty codes should throw invalid province`() {
    val exception =
      assertThrows<AquaException> {
        service.validateAndNormalizeAddress(address(provinceCode = null, cityCode = null, districtCode = null))
      }
    assertTrue(exception.message!!.contains("无效的省份代码"))
  }

  @Test
  fun `findRegionByCoordinates should return region when found`() {
    val geocodeResult = GeocodingResult("广东省", "440000", "深圳市", "440300", "南山区", "440305", "街", "地址", 114.0, 22.0, 0.8)
    every { externalGeocodingService.reverseGeocode(114.0, 22.0) } returns geocodeResult
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    assertEquals("440000", service.findRegionByCoordinates(114.0, 22.0)?.code)
  }

  @Test
  fun `findRegionByCoordinates should return null when reverse geocode is null`() {
    every { externalGeocodingService.reverseGeocode(any(), any()) } returns null
    assertNull(service.findRegionByCoordinates(114.0, 22.0))
  }

  @Test
  fun `findRegionByCoordinates should return null when province code missing`() {
    val geocodeResult = GeocodingResult("广东省", null, "深圳市", "440300", "南山区", "440305", "街", "地址", 114.0, 22.0, 0.8)
    every { externalGeocodingService.reverseGeocode(any(), any()) } returns geocodeResult
    assertNull(service.findRegionByCoordinates(114.0, 22.0))
  }

  @Test
  fun `calculateDistance should return zero for same point`() {
    assertEquals(0.0, service.calculateDistance(114.0, 22.5, 114.0, 22.5), 0.001)
  }

  @Test
  fun `calculateDistance should compute positive distance between cities`() {
    // 深圳(114.0579, 22.5431) 到 北京(116.4074, 39.9042) 约 1900+ 公里
    val distance = service.calculateDistance(114.0579, 22.5431, 116.4074, 39.9042)
    assertTrue(distance in 1800.0..2100.0)
  }

  @Test
  fun `isInServiceArea should return true when region exists`() {
    every { externalGeocodingService.reverseGeocode(any(), any()) } returns
      GeocodingResult("广东省", "440000", "深圳市", "440300", "南山区", "440305", "街", "地址", 114.0, 22.0, 0.8)
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    assertTrue(service.isInServiceArea(114.0, 22.0))
  }

  @Test
  fun `isInServiceArea should return false when region missing`() {
    every { externalGeocodingService.reverseGeocode(any(), any()) } returns null
    assertFalse(service.isInServiceArea(114.0, 22.0))
  }

  @Test
  fun `validateCoordinates should check bounds`() {
    assertTrue(service.validateCoordinates(0.0, 0.0))
    assertTrue(service.validateCoordinates(-180.0, -90.0))
    assertTrue(service.validateCoordinates(180.0, 90.0))
    assertFalse(service.validateCoordinates(181.0, 0.0))
    assertFalse(service.validateCoordinates(0.0, 91.0))
  }

  @Test
  fun `formatCoordinates should round to 7 decimals`() {
    val (lng, lat) = service.formatCoordinates(114.057912345, 22.54316789)
    assertEquals(114.0579123, lng)
    assertEquals(22.5431679, lat)
  }

  @Test
  fun `geocode should build full address from parts`() {
    every { externalGeocodingService.geocode("广东省深圳市南山区科技园路1号") } returns Pair(1.0, 2.0)
    service.geocode(address())
    verify { externalGeocodingService.geocode("广东省深圳市南山区科技园路1号") }
  }

  @Test
  fun `normalizeDetailAddress should merge spaces`() {
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
    every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "440300")
    val result = service.validateAndNormalizeAddress(address(detailAddress = "  A    B  ")).detailAddress
    assertEquals("A B", result)
  }

  @Test
  fun `normalizeDetailAddress should normalize house number`() {
    every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
    every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
    every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "440300")
    val result = service.validateAndNormalizeAddress(address(detailAddress = "3号")).detailAddress
    assertEquals("号", result)
  }
}
