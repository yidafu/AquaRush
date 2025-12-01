/*
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

import dev.yidafu.aqua.user.service.ExternalGeocodingService
import dev.yidafu.aqua.user.service.GeocodingResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForObject

@Primary
@Service
class GoogleGeocodingService(
    private val restTemplate: RestTemplate
) : ExternalGeocodingService {

    @Value($$"${geocoding.google.api-key:}")
    private val apiKey: String = ""

    @Value($$"${geocoding.google.base-url:https://maps.googleapis.com/maps/api}")
    private val baseUrl: String = "https://maps.googleapis.com/maps/api"

    override fun geocode(address: String): Pair<Double, Double>? {
        if (apiKey.isBlank()) {
            // 如果没有配置API Key，返回空（使用模拟服务）
            return MockGeocodingService().geocode(address)
        }

        return try {
            val url = "$baseUrl/geocode/json?address=${address.replace(" ", "+")}&key=$apiKey&language=zh-CN"
            val response = restTemplate.getForObject<GoogleGeocodeResponse>(url)

            response?.results?.firstOrNull()?.geometry?.location?.let { location ->
                Pair(location.lng, location.lat)
            }
        } catch (e: Exception) {
            println("Google Geocoding API error: ${e.message}")
            null
        }
    }

    override fun reverseGeocode(longitude: Double, latitude: Double): GeocodingResult? {
        if (apiKey.isBlank()) {
            // 如果没有配置API Key，返回空（使用模拟服务）
            return MockGeocodingService().reverseGeocode(longitude, latitude)
        }

        return try {
            val url = "$baseUrl/geocode/json?latlng=$latitude,$longitude&key=$apiKey&language=zh-CN"
            val response = restTemplate.getForObject<GoogleGeocodeResponse>(url)

            response?.results?.firstOrNull()?.let { result ->
                val (province, provinceCode) = extractAdministrativeArea(result.address_components, "administrative_area_level_1")
                val (city, cityCode) = extractAdministrativeArea(result.address_components, "administrative_area_level_2")
                val (district, districtCode) = extractAdministrativeArea(result.address_components, "sublocality_level_1")

                GeocodingResult(
                    province = province,
                    provinceCode = provinceCode,
                    city = city,
                    cityCode = cityCode,
                    district = district,
                    districtCode = districtCode,
                    street = result.formatted_address,
                    formattedAddress = result.formatted_address,
                    longitude = result.geometry.location.lng,
                    latitude = result.geometry.location.lat,
                    confidence = calculateConfidence(result)
                )
            }
        } catch (e: Exception) {
            println("Google Reverse Geocoding API error: ${e.message}")
            null
        }
    }

    private fun extractAdministrativeArea(components: List<GoogleAddressComponent>, type: String): Pair<String, String?> {
        val component = components.find { it.types.contains(type) }
        return Pair(component?.long_name ?: "", component?.short_name)
    }

    private fun calculateConfidence(result: GoogleResult): Double {
        return when {
            result.types.contains("street_address") -> 0.95
            result.types.contains("route") -> 0.85
            result.types.contains("sublocality") -> 0.75
            result.types.contains("locality") -> 0.65
            else -> 0.5
        }
    }
}

@Profile("test")
// Mock service for development/testing
@Service
class MockGeocodingService : ExternalGeocodingService {
    override fun geocode(address: String): Pair<Double, Double>? {
        // 返回北京的坐标作为示例
        return when {
            address.contains("北京") -> Pair(116.4074, 39.9042)
            address.contains("上海") -> Pair(121.4737, 31.2304)
            address.contains("广州") -> Pair(113.2644, 23.1291)
            address.contains("深圳") -> Pair(114.0579, 22.5431)
            else -> Pair(116.4074, 39.9042) // 默认北京
        }
    }

    override fun reverseGeocode(longitude: Double, latitude: Double): GeocodingResult? {
        return GeocodingResult(
            province = "北京市",
            provinceCode = "110000",
            city = "北京市",
            cityCode = "110100",
            district = "朝阳区",
            districtCode = "110105",
            street = "建国路88号",
            formattedAddress = "北京市朝阳区建国路88号",
            longitude = longitude,
            latitude = latitude,
            confidence = 0.8
        )
    }
}

// Data classes for Google API response
data class GoogleGeocodeResponse(
    val results: List<GoogleResult>,
    val status: String
)

data class GoogleResult(
    val address_components: List<GoogleAddressComponent>,
    val formatted_address: String,
    val geometry: GoogleGeometry,
    val types: List<String>
)

data class GoogleAddressComponent(
    val long_name: String,
    val short_name: String,
    val types: List<String>
)

data class GoogleGeometry(
    val location: GoogleLocation,
    val location_type: String
)

data class GoogleLocation(
    val lat: Double,
    val lng: Double
)
