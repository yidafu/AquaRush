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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class GeolocationServiceTest :
  FunSpec({
    var regionRepository: RegionRepository = mockk()
    var externalGeocodingService: ExternalGeocodingService = mockk()
    var service: GeolocationService = mockk()

    fun region(
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

    fun address(
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

    beforeTest {
      regionRepository = mockk(relaxed = true)
      externalGeocodingService = mockk(relaxed = true)
      service = GeolocationService(regionRepository, externalGeocodingService)
    }
    test("geocode should return coordinates") {
      every { externalGeocodingService.geocode(any()) } returns Pair(114.05, 22.54)
      val result = service.geocode(address())
      result?.first shouldBe 114.05
      result?.second shouldBe 22.54
    }
    test("geocode should return null on external failure") {
      every { externalGeocodingService.geocode(any()) } throws RuntimeException("down")
      service.geocode(address()).shouldBeNull()
    }
    test("reverseGeocode should return result") {
      val result = GeocodingResult("广东省", "440000", "深圳市", "440300", "南山区", "440305", "街", "地址", 114.0, 22.0, 0.9)
      every { externalGeocodingService.reverseGeocode(114.0, 22.0) } returns result
      service.reverseGeocode(114.0, 22.0)?.province shouldBe "广东省"
    }
    test("reverseGeocode should return null on external failure") {
      every { externalGeocodingService.reverseGeocode(any(), any()) } throws RuntimeException("down")
      service.reverseGeocode(114.0, 22.0).shouldBeNull()
    }
    test("validateAndNormalizeAddress should normalize valid address") {
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
      every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "440300")

      val result = service.validateAndNormalizeAddress(address(detailAddress = "  科技园路 1 号  "))
      result.province shouldBe "广东省"
      result.city shouldBe "深圳市"
      result.district shouldBe "南山区"
      result.detailAddress.shouldNotBeNull()
    }
    test("validateAndNormalizeAddress should throw for invalid province") {
      every { regionRepository.findByCode("440000") } returns null
      val exception = shouldThrow<AquaException> { service.validateAndNormalizeAddress(address()) }
      exception.message!!.contains("无效的省份代码") shouldBe true
    }
    test("validateAndNormalizeAddress should throw for invalid city level") {
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 1)
      val exception = shouldThrow<AquaException> { service.validateAndNormalizeAddress(address()) }
      exception.message!!.contains("无效的城市代码") shouldBe true
    }
    test("validateAndNormalizeAddress should throw for invalid district level") {
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
      every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 1)
      val exception = shouldThrow<AquaException> { service.validateAndNormalizeAddress(address()) }
      exception.message!!.contains("无效的区县代码") shouldBe true
    }
    test("validateAndNormalizeAddress should throw for mismatched city parent") {
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "999999")
      every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "440300")
      val exception = shouldThrow<AquaException> { service.validateAndNormalizeAddress(address()) }
      exception.message!!.contains("城市与省份不匹配") shouldBe true
    }
    test("validateAndNormalizeAddress should throw for mismatched district parent") {
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
      every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "999999")
      val exception = shouldThrow<AquaException> { service.validateAndNormalizeAddress(address()) }
      exception.message!!.contains("区县与城市不匹配") shouldBe true
    }
    test("validateAndNormalizeAddress with empty codes should throw invalid province") {
      val exception =
        shouldThrow<AquaException> {
          service.validateAndNormalizeAddress(address(provinceCode = null, cityCode = null, districtCode = null))
        }
      exception.message!!.contains("无效的省份代码") shouldBe true
    }
    test("findRegionByCoordinates should return region when found") {
      val geocodeResult = GeocodingResult("广东省", "440000", "深圳市", "440300", "南山区", "440305", "街", "地址", 114.0, 22.0, 0.8)
      every { externalGeocodingService.reverseGeocode(114.0, 22.0) } returns geocodeResult
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      service.findRegionByCoordinates(114.0, 22.0)?.code shouldBe "440000"
    }
    test("findRegionByCoordinates should return null when reverse geocode is null") {
      every { externalGeocodingService.reverseGeocode(any(), any()) } returns null
      service.findRegionByCoordinates(114.0, 22.0).shouldBeNull()
    }
    test("findRegionByCoordinates should return null when province code missing") {
      val geocodeResult = GeocodingResult("广东省", null, "深圳市", "440300", "南山区", "440305", "街", "地址", 114.0, 22.0, 0.8)
      every { externalGeocodingService.reverseGeocode(any(), any()) } returns geocodeResult
      service.findRegionByCoordinates(114.0, 22.0).shouldBeNull()
    }
    test("calculateDistance should return zero for same point") {
      withClue(0.001) { service.calculateDistance(114.0, 22.5, 114.0, 22.5) shouldBe 0.0 }
    }
    test("calculateDistance should compute positive distance between cities") {
      // 深圳(114.0579, 22.5431) 到 北京(116.4074, 39.9042) 约 1900+ 公里
      val distance = service.calculateDistance(114.0579, 22.5431, 116.4074, 39.9042)
      (distance in 1800.0..2100.0) shouldBe true
    }
    test("isInServiceArea should return true when region exists") {
      every { externalGeocodingService.reverseGeocode(any(), any()) } returns
        GeocodingResult("广东省", "440000", "深圳市", "440300", "南山区", "440305", "街", "地址", 114.0, 22.0, 0.8)
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      service.isInServiceArea(114.0, 22.0) shouldBe true
    }
    test("isInServiceArea should return false when region missing") {
      every { externalGeocodingService.reverseGeocode(any(), any()) } returns null
      service.isInServiceArea(114.0, 22.0) shouldBe false
    }
    test("validateCoordinates should check bounds") {
      service.validateCoordinates(0.0, 0.0) shouldBe true
      service.validateCoordinates(-180.0, -90.0) shouldBe true
      service.validateCoordinates(180.0, 90.0) shouldBe true
      service.validateCoordinates(181.0, 0.0) shouldBe false
      service.validateCoordinates(0.0, 91.0) shouldBe false
    }
    test("formatCoordinates should round to 7 decimals") {
      val (lng, lat) = service.formatCoordinates(114.057912345, 22.54316789)
      lng shouldBe 114.0579123
      lat shouldBe 22.5431679
    }
    test("geocode should build full address from parts") {
      every { externalGeocodingService.geocode("广东省深圳市南山区科技园路1号") } returns Pair(1.0, 2.0)
      service.geocode(address())
      verify { externalGeocodingService.geocode("广东省深圳市南山区科技园路1号") }
    }
    test("normalizeDetailAddress should merge spaces") {
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
      every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "440300")
      val result = service.validateAndNormalizeAddress(address(detailAddress = "  A    B  ")).detailAddress
      result shouldBe "A B"
    }
    test("normalizeDetailAddress should normalize house number") {
      every { regionRepository.findByCode("440000") } returns region("440000", "广东省", 1)
      every { regionRepository.findByCode("440300") } returns region("440300", "深圳市", 2, "440000")
      every { regionRepository.findByCode("440305") } returns region("440305", "南山区", 3, "440300")
      val result = service.validateAndNormalizeAddress(address(detailAddress = "3号")).detailAddress
      result shouldBe "号"
    }
  })
