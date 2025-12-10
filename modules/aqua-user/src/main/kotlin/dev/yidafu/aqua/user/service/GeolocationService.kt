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

package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.user.domain.exception.AquaException
import dev.yidafu.aqua.user.domain.model.AddressModel
import dev.yidafu.aqua.user.domain.model.RegionModel
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class GeolocationService(
    private val regionRepository: RegionRepository,
    private val externalGeocodingService: ExternalGeocodingService
) {

    /**
     * 地址转坐标 - 地理编码
     */
    fun geocode(address: AddressModel): Pair<Double, Double>? {
        val fullAddress = buildFullAddress(address)
        return try {
            externalGeocodingService.geocode(fullAddress)
        } catch (e: Exception) {
            // 记录错误但不抛出异常，允许业务继续
            null
        }
    }

    /**
     * 坐标转地址 - 逆地理编码
     */
    fun reverseGeocode(longitude: Double, latitude: Double): GeocodingResult? {
        return try {
            externalGeocodingService.reverseGeocode(
                longitude,
                latitude
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 验证并标准化地址
     */
    fun validateAndNormalizeAddress(address: AddressModel): AddressModel {
        // 验证行政区划代码
        val province = address.provinceCode?.let { code ->
            regionRepository.findByCode(code)
                ?.takeIf { it.level == 1 }
                ?: throw AquaException("无效的省份代码: $code")
        }

        val city = address.cityCode?.let { code ->
            regionRepository.findByCode(code)
                ?.takeIf { it.level == 2 }
                ?: throw AquaException("无效的城市代码: $code")
        }

        val district = address.districtCode?.let { code ->
            regionRepository.findByCode(code)
                ?.takeIf { it.level == 3 }
                ?: throw AquaException("无效的区县代码: $code")
        }

        // 验证层级关系
        city?.let { city ->
            if (city.parentCode != province?.code) {
                throw AquaException("城市与省份不匹配")
            }
        }

        district?.let { district ->
            if (district.parentCode != city?.code) {
                throw AquaException("区县与城市不匹配")
            }
        }

        // 标准化地址名称
        province?.let { address.province = it.name }
        city?.let { address.city = it.name }
        district?.let { address.district = it.name }

        // 标准化详细地址
        address.detailAddress = normalizeDetailAddress(address.detailAddress)

        return address
    }

    /**
     * 根据坐标匹配行政区划
     */
    @Cacheable(value = ["region_by_coords"], key = "#longitude.toString() + '_' + #latitude.toString()")
    fun findRegionByCoordinates(longitude: Double, latitude: Double): RegionModel? {
        val result = externalGeocodingService.reverseGeocode(
            longitude,
            latitude
        )

        return result?.provinceCode?.let { provinceCode ->
            regionRepository.findByCode(provinceCode)
        }
    }

    /**
     * 计算两个坐标之间的距离（单位：公里）
     */
    fun calculateDistance(
        lng1: Double, lat1: Double,
        lng2: Double, lat2: Double
    ): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLng = Math.toRadians(lng2 - lng1)

        val sinDeltaLatHalf = kotlin.math.sin(deltaLat / 2)
      val sinDeltaLngHalf = kotlin.math.sin(deltaLng / 2)
      val a = sinDeltaLatHalf * sinDeltaLatHalf +
                kotlin.math.cos(lat1Rad) * kotlin.math.cos(lat2Rad) *
                sinDeltaLngHalf * sinDeltaLngHalf
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return 6371 * c // 地球半径（公里）
    }

    /**
     * 检查坐标是否在服务区域内
     */
    fun isInServiceArea(longitude: Double, latitude: Double): Boolean {
        // 这里可以根据业务需求定义服务区域
        // 例如：只在特定省份/城市提供服务
        val region = findRegionByCoordinates(longitude, latitude)
        return region != null
    }

    /**
     * 构建完整地址字符串
     */
    private fun buildFullAddress(address: AddressModel): String {
        return buildString {
            append(address.province)
            append(address.city)
            append(address.district)
            append(address.detailAddress)
        }
    }

    /**
     * 标准化详细地址
     */
    private fun normalizeDetailAddress(detailAddress: String): String {
        return detailAddress
            .trim()
            .replace(Regex("\\s+"), " ") // 合并多个空格
            .replace(Regex("[号\\s]*\\d+[号\\s]*"), "号") // 标准化门牌号格式
            .replace(Regex("[号楼\\s]*\\d+[号楼\\s]*"), "号楼") // 标准化楼号格式
            .replace(Regex("[单元\\s]*\\d+[单元\\s]*"), "单元") // 标准化单元格式
            .replace(Regex("[室\\s]*\\d+[室\\s]*"), "室") // 标准化房间格式
    }

    /**
     * 验证坐标有效性
     */
    fun validateCoordinates(longitude: Double, latitude: Double): Boolean {
        return longitude >= -180.0 && longitude <= 180.0 &&
               latitude >= -90.0 && latitude <= 90.0
    }

    /**
     * 坐标格式化
     */
    fun formatCoordinates(longitude: Double, latitude: Double): Pair<Double, Double> {
        // 格式化到7位小数
        return Pair(
            String.format("%.7f", longitude).toDouble(),
            String.format("%.7f", latitude).toDouble()
        )
    }
}

/**
 * 地理编码结果
 */
data class GeocodingResult(
    val province: String,
    val provinceCode: String?,
    val city: String,
    val cityCode: String?,
    val district: String,
    val districtCode: String?,
    val street: String?,
    val formattedAddress: String,
    val longitude: Double,
    val latitude: Double,
    val confidence: Double // 置信度 0-1
)

/**
 * 外部地理编码服务接口
 */
interface ExternalGeocodingService {
    /**
     * 地理编码：地址转坐标
     */
    fun geocode(address: String): Pair<Double, Double>?

    /**
     * 逆地理编码：坐标转地址
     */
    fun reverseGeocode(longitude: Double, latitude: Double): GeocodingResult?
}
