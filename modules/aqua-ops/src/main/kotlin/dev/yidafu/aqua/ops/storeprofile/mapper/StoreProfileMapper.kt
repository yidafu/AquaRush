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
import dev.yidafu.aqua.common.graphql.generated.StoreProfileVo
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HH_MM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * 店铺信息映射器:域实体 → GraphQL 类型
 * 营业时间跨 GraphQL 边界以 "HH:mm" 字符串传输(无 LocalTime scalar)。
 */
fun StoreProfileModel.toGraphQL(): StoreProfileVo =
  StoreProfileVo(
    aboutUs = aboutUs,
    businessHoursEnd = businessHoursEnd?.format(HH_MM_FORMATTER),
    businessHoursStart = businessHoursStart?.format(HH_MM_FORMATTER),
    logoUrl = logoUrl,
    serviceHotline = serviceHotline,
    shopIntro = shopIntro,
    shopName = shopName,
    updatedAt = updatedAt,
  )

/**
 * 保存店铺信息输入映射器:GraphQL 输入 → 服务请求
 *
 * 保留该中间 DTO 的原因:businessHoursStart/End 跨边界类型不同
 * (GraphQL "HH:mm" 字符串 ↔ 领域 LocalTime),超出「仅枚举换型 + 默认值回填」的删除规则。
 */
fun SaveStoreProfileInput.toRequest(): SaveStoreProfileRequest =
  SaveStoreProfileRequest(
    aboutUs = aboutUs,
    businessHoursEnd = businessHoursEnd?.let { LocalTime.parse(it, HH_MM_FORMATTER) },
    businessHoursStart = businessHoursStart?.let { LocalTime.parse(it, HH_MM_FORMATTER) },
    logoUrl = logoUrl,
    serviceHotline = serviceHotline,
    shopIntro = shopIntro,
    shopName = shopName,
  )
