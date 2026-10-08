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

import dev.yidafu.aqua.user.service.GeocodingResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.client.RestTemplate

class GoogleGeocodingServiceTest :
  FunSpec({
    var restTemplate: RestTemplate = mockk()
    var service: GoogleGeocodingService = mockk()

    fun setApiKey(key: String) {
      val field = GoogleGeocodingService::class.java.getDeclaredField("apiKey")
      field.isAccessible = true
      field.set(service, key)
    }

    fun response(
      location: Pair<Double, Double>,
      types: List<String> = listOf("street_address"),
    ): GoogleGeocodeResponse {
      val component = GoogleAddressComponent("广东省", "440000", listOf("administrative_area_level_1"))
      val result =
        GoogleResult(
          address_components = listOf(component),
          formatted_address = "广东省深圳市",
          geometry = GoogleGeometry(GoogleLocation(location.second, location.first), "ROOFTOP"),
          types = types,
        )
      return GoogleGeocodeResponse(listOf(result), "OK")
    }

    // ========== Mock fallback (no API key) ==========

    // ========== HTTP branch (with API key) ==========

    beforeTest {
      restTemplate = mockk(relaxed = true)
      service = GoogleGeocodingService(restTemplate)
    }
    test("geocode without api key should use mock service for beijing") {
      val result = service.geocode("北京市朝阳区建国路88号")
      result.shouldNotBeNull()
      result!!.first shouldBe 116.4074
      result.second shouldBe 39.9042
    }
    test("geocode without api key should use mock service for shanghai") {
      val result = service.geocode("上海市浦东新区")
      result!!.first shouldBe 121.4737
    }
    test("geocode without api key should default to beijing") {
      val result = service.geocode("未知地点")
      result!!.first shouldBe 116.4074
    }
    test("reverseGeocode without api key should use mock service") {
      val result = service.reverseGeocode(116.4074, 39.9042)
      result.shouldNotBeNull()
      result!!.provinceCode shouldBe "110000"
      result.province shouldBe "北京市"
    }
    test("geocode with api key should parse response") {
      setApiKey("test-key")
      every { restTemplate.getForObject(any(), GoogleGeocodeResponse::class.java, *anyVararg()) } returns response(Pair(114.05, 22.54))
      val result = service.geocode("广东省深圳市")
      result.shouldNotBeNull()
      result!!.first shouldBe 114.05
      result.second shouldBe 22.54
    }
    test("geocode with api key should return null when no results") {
      setApiKey("test-key")
      every { restTemplate.getForObject(any(), GoogleGeocodeResponse::class.java, *anyVararg()) } returns
        GoogleGeocodeResponse(emptyList(), "ZERO_RESULTS")
      service.geocode("广东省深圳市").shouldBeNull()
    }
    test("geocode with api key should return null on error") {
      setApiKey("test-key")
      every { restTemplate.getForObject(any(), GoogleGeocodeResponse::class.java, *anyVararg()) } throws RuntimeException("http error")
      service.geocode("广东省深圳市").shouldBeNull()
    }
    test("reverseGeocode with api key should parse administrative areas") {
      setApiKey("test-key")
      val component = GoogleAddressComponent("广东省", "440000", listOf("administrative_area_level_1"))
      val result =
        GoogleResult(
          address_components = listOf(component),
          formatted_address = "广东省深圳市",
          geometry = GoogleGeometry(GoogleLocation(22.54, 114.05), "ROOFTOP"),
          types = listOf("street_address"),
        )
      every { restTemplate.getForObject(any(), GoogleGeocodeResponse::class.java, *anyVararg()) } returns
        GoogleGeocodeResponse(listOf(result), "OK")
      val geocoded = service.reverseGeocode(114.05, 22.54)
      geocoded.shouldNotBeNull()
      geocoded!!.province shouldBe "广东省"
      geocoded.provinceCode shouldBe "440000"
      geocoded.confidence shouldBe 0.95
    }
    test("reverseGeocode with api key should compute confidence by type") {
      setApiKey("test-key")
      val cases =
        mapOf(
          listOf("route") to 0.85,
          listOf("sublocality") to 0.75,
          listOf("locality") to 0.65,
          listOf("neighborhood") to 0.5,
        )
      for ((types, expected) in cases) {
        every { restTemplate.getForObject(any(), GoogleGeocodeResponse::class.java, *anyVararg()) } returns
          response(Pair(114.05, 22.54), types)
        val geocoded = service.reverseGeocode(114.05, 22.54)
        geocoded!!.confidence shouldBe expected
      }
    }
    test("reverseGeocode with api key should return null when no results") {
      setApiKey("test-key")
      every { restTemplate.getForObject(any(), GoogleGeocodeResponse::class.java, *anyVararg()) } returns
        GoogleGeocodeResponse(emptyList(), "ZERO_RESULTS")
      service.reverseGeocode(114.05, 22.54).shouldBeNull()
    }
    test("reverseGeocode with api key should return null on error") {
      setApiKey("test-key")
      every { restTemplate.getForObject(any(), GoogleGeocodeResponse::class.java, *anyVararg()) } throws RuntimeException("http error")
      service.reverseGeocode(114.05, 22.54).shouldBeNull()
    }
    test("mock geocoding service should handle all cities") {
      val mock = MockGeocodingService()
      mock.geocode("北京").shouldNotBeNull()
      mock.geocode("上海").shouldNotBeNull()
      mock.geocode("广州").shouldNotBeNull()
      mock.geocode("深圳").shouldNotBeNull()
      mock.reverseGeocode(116.4, 39.9).shouldNotBeNull()
      val result: GeocodingResult? = mock.reverseGeocode(0.0, 0.0)
      result!!.longitude shouldBe 0.0
    }
  })
