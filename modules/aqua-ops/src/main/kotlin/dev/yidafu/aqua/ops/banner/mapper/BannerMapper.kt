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

import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.graphql.generated.BannerLinkType
import dev.yidafu.aqua.common.graphql.generated.BannerVo
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType as DomainBannerLinkType

/**
 * 轮播图映射器:域实体 → GraphQL 类型
 */
fun BannerModel.toGraphQL(): BannerVo =
  BannerVo(
    createdAt = createdAt,
    enabled = enabled,
    endAt = endAt,
    id = id ?: 0L,
    imageUrl = imageUrl,
    linkTarget = linkTarget,
    linkType = linkType.toGraphQL(),
    sortOrder = sortOrder,
    startAt = startAt,
    subtitle = subtitle,
    title = title,
    updatedAt = updatedAt,
  )

/**
 * 轮播图跳转类型:领域枚举 ↔ GraphQL 枚举同名 valueOf 互转
 */
fun DomainBannerLinkType.toGraphQL(): BannerLinkType = BannerLinkType.valueOf(name)

fun BannerLinkType.toDomain(): DomainBannerLinkType = DomainBannerLinkType.valueOf(name)
