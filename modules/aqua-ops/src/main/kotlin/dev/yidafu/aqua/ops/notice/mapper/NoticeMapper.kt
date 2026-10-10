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

package dev.yidafu.aqua.ops.notice.mapper

import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.graphql.generated.NoticeStatus
import dev.yidafu.aqua.common.graphql.generated.NoticeType
import dev.yidafu.aqua.common.graphql.generated.NoticeVo
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus as DomainNoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType as DomainNoticeType

/**
 * 公告映射器:域实体 → GraphQL 类型
 */
fun NoticeModel.toGraphQL(): NoticeVo =
  NoticeVo(
    content = content,
    createdAt = createdAt,
    endAt = endAt,
    id = id ?: 0L,
    pinned = pinned,
    publishedAt = publishedAt,
    startAt = startAt,
    status = status.toGraphQL(),
    title = title,
    type = type.toGraphQL(),
    updatedAt = updatedAt,
  )

/**
 * 公告类型:领域枚举 ↔ GraphQL 枚举同名枚举 when 穷举互转
 */
fun DomainNoticeType.toGraphQL(): NoticeType =
  when (this) {
    DomainNoticeType.PLATFORM_NOTICE -> NoticeType.PLATFORM_NOTICE
    DomainNoticeType.WATER_OUTAGE -> NoticeType.WATER_OUTAGE
    DomainNoticeType.ACTIVITY_PREVIEW -> NoticeType.ACTIVITY_PREVIEW
  }

fun NoticeType.toDomain(): DomainNoticeType =
  when (this) {
    NoticeType.ACTIVITY_PREVIEW -> DomainNoticeType.ACTIVITY_PREVIEW
    NoticeType.PLATFORM_NOTICE -> DomainNoticeType.PLATFORM_NOTICE
    NoticeType.WATER_OUTAGE -> DomainNoticeType.WATER_OUTAGE
  }

/**
 * 公告状态:领域枚举 ↔ GraphQL 枚举同名枚举 when 穷举互转
 */
fun DomainNoticeStatus.toGraphQL(): NoticeStatus =
  when (this) {
    DomainNoticeStatus.DRAFT -> NoticeStatus.DRAFT
    DomainNoticeStatus.PUBLISHED -> NoticeStatus.PUBLISHED
    DomainNoticeStatus.STOPPED -> NoticeStatus.STOPPED
  }

fun NoticeStatus.toDomain(): DomainNoticeStatus =
  when (this) {
    NoticeStatus.DRAFT -> DomainNoticeStatus.DRAFT
    NoticeStatus.PUBLISHED -> DomainNoticeStatus.PUBLISHED
    NoticeStatus.STOPPED -> DomainNoticeStatus.STOPPED
  }
