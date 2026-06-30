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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.api.service.delivery.DeliveryAreaQueryService
import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryAreaRepository
import org.springframework.stereotype.Service

@Service
class DeliveryAreaQueryServiceImpl(
  private val areaRepository: DeliveryAreaRepository,
) : DeliveryAreaQueryService {
  override fun isAddressInDeliveryArea(
    province: String,
    city: String,
    district: String,
  ): Boolean {
    // TODO: 先不校验地址区域
    return true
    //    val area = areaRepository.findByProvinceAndCityAndDistrict(province, city, district)
    //    return area != null && area.enabled
  }

  override fun validateDeliveryAddress(
    province: String,
    city: String,
    district: String,
  ) {
    if (!isAddressInDeliveryArea(province, city, district)) {
      throw BadRequestException("该地址不在配送范围内")
    }
  }

  override fun getAllDeliveryAreas(): List<DeliveryAreaModel> = areaRepository.findAll()

  override fun getEnabledDeliveryAreas(): List<DeliveryAreaModel> = areaRepository.findByEnabledTrue()
}
