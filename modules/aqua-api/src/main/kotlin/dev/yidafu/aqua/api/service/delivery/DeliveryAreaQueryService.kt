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

package dev.yidafu.aqua.api.service.delivery

import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel

/**
 * 配送区域查询服务接口
 */
interface DeliveryAreaQueryService {
  /**
   * 检查地址是否在配送区域内
   */
  fun isAddressInDeliveryArea(
    province: String,
    city: String,
    district: String,
  ): Boolean

  /**
   * 验证配送地址
   */
  fun validateDeliveryAddress(
    province: String,
    city: String,
    district: String,
  )

  /**
   * 获取所有配送区域
   */
  fun getAllDeliveryAreas(): List<DeliveryAreaModel>

  /**
   * 获取已启用的配送区域
   */
  fun getEnabledDeliveryAreas(): List<DeliveryAreaModel>
}
