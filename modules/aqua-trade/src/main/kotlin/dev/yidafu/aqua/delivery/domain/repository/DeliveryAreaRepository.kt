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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import java.util.Optional

/**
 * 配送区域仓储(JPA→Exposed 迁移 P4):剥离 JpaRepository,仅保留实际被调用的方法;
 * 查询过滤 is_deleted(对齐 @SoftDelete),save 为按 id upsert。
 */
interface DeliveryAreaRepository {
  /** 按省/市/区精确定位区域(地址校验用,过滤软删)。 */
  fun findByProvinceAndCityAndDistrict(
    province: String,
    city: String,
    district: String,
  ): DeliveryAreaModel?

  /** 启用中的区域列表(过滤软删)。 */
  fun findByEnabledTrue(): List<DeliveryAreaModel>

  fun findById(id: Long): Optional<DeliveryAreaModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: DeliveryAreaModel): DeliveryAreaModel
}
