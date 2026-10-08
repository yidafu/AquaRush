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

package dev.yidafu.aqua.ops.storeprofile.mapper

import dev.yidafu.aqua.api.dto.SaveStoreProfileRequest
import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import dev.yidafu.aqua.common.graphql.generated.SaveStoreProfileInput
import dev.yidafu.aqua.common.graphql.generated.StoreProfile
import tech.mappie.api.ObjectMappie
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * 店铺信息映射器:域实体 → GraphQL 类型
 * 营业时间跨 GraphQL 边界以 "HH:mm" 字符串传输(无 LocalTime scalar)。
 */
object StoreProfileMapper : ObjectMappie<StoreProfileModel, StoreProfile>() {
  private val HH_MM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

  override fun map(from: StoreProfileModel): StoreProfile =
    mapping {
      to::businessHoursStart fromExpression { from.businessHoursStart?.format(HH_MM_FORMATTER) }
      to::businessHoursEnd fromExpression { from.businessHoursEnd?.format(HH_MM_FORMATTER) }
    }
}

/**
 * 保存店铺信息输入映射器:GraphQL 输入 → 服务请求
 */
object SaveStoreProfileInputMapper : ObjectMappie<SaveStoreProfileInput, SaveStoreProfileRequest>() {
  private val HH_MM_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

  override fun map(from: SaveStoreProfileInput): SaveStoreProfileRequest =
    mapping {
      to::businessHoursStart fromExpression { from.businessHoursStart?.let { LocalTime.parse(it, HH_MM_FORMATTER) } }
      to::businessHoursEnd fromExpression { from.businessHoursEnd?.let { LocalTime.parse(it, HH_MM_FORMATTER) } }
    }
}
