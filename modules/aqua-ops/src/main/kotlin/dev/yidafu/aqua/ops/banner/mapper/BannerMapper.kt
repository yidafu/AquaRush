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

package dev.yidafu.aqua.ops.banner.mapper

import dev.yidafu.aqua.api.dto.BannerCreateRequest
import dev.yidafu.aqua.api.dto.BannerListRequest
import dev.yidafu.aqua.api.dto.BannerUpdateRequest
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.graphql.generated.Banner
import dev.yidafu.aqua.common.graphql.generated.BannerLinkType
import dev.yidafu.aqua.common.graphql.generated.BannerListInput
import dev.yidafu.aqua.common.graphql.generated.CreateBannerInput
import dev.yidafu.aqua.common.graphql.generated.UpdateBannerInput
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType as DomainBannerLinkType

/**
 * 轮播图映射器:域实体 → GraphQL 类型
 */
object BannerMapper : ObjectMappie<BannerModel, Banner>() {
  override fun map(from: BannerModel): Banner =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::linkType fromExpression { BannerLinkTypeMapper.map(from.linkType) }
    }
}

object BannerLinkTypeMapper : EnumMappie<DomainBannerLinkType, BannerLinkType>()

/**
 * 新建轮播图输入映射器:GraphQL 输入 → 服务请求
 */
object CreateBannerInputMapper : ObjectMappie<CreateBannerInput, BannerCreateRequest>() {
  override fun map(from: CreateBannerInput): BannerCreateRequest =
    mapping {
      to::linkType fromExpression { BannerLinkTypeInputMapper.map(from.linkType) }
      to::enabled fromValue (from.enabled ?: false)
    }
}

/**
 * 编辑轮播图输入映射器
 */
object UpdateBannerInputMapper : ObjectMappie<UpdateBannerInput, BannerUpdateRequest>() {
  override fun map(from: UpdateBannerInput): BannerUpdateRequest =
    mapping {
      to::linkType fromExpression { BannerLinkTypeInputMapper.map(from.linkType) }
      to::enabled fromValue (from.enabled ?: false)
    }
}

object BannerLinkTypeInputMapper : EnumMappie<BannerLinkType, DomainBannerLinkType>()

/**
 * 轮播图列表查询输入映射器
 */
object BannerListInputMapper : ObjectMappie<BannerListInput, BannerListRequest>() {
  override fun map(from: BannerListInput): BannerListRequest =
    mapping {
      to::page fromValue (from.page ?: 0)
      to::size fromValue (from.size ?: 20)
    }
}
