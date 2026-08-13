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

import dev.yidafu.aqua.api.dto.CreateNoticeRequest
import dev.yidafu.aqua.api.dto.NoticeListRequest
import dev.yidafu.aqua.api.dto.UpdateNoticeRequest
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus as DomainNoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType as DomainNoticeType
import dev.yidafu.aqua.common.graphql.generated.CreateNoticeInput
import dev.yidafu.aqua.common.graphql.generated.Notice
import dev.yidafu.aqua.common.graphql.generated.NoticeListInput
import dev.yidafu.aqua.common.graphql.generated.NoticeStatus
import dev.yidafu.aqua.common.graphql.generated.NoticeType
import dev.yidafu.aqua.common.graphql.generated.UpdateNoticeInput
import tech.mappie.api.EnumMappie
import tech.mappie.api.ObjectMappie

/**
 * 公告映射器:域实体 → GraphQL 类型
 */
object NoticeMapper : ObjectMappie<NoticeModel, Notice>() {
  override fun map(from: NoticeModel): Notice =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::type fromExpression { NoticeTypeMapper.map(from.type) }
      to::status fromExpression { NoticeStatusMapper.map(from.status) }
    }
}

object NoticeTypeMapper : EnumMappie<DomainNoticeType, NoticeType>()

object NoticeStatusMapper : EnumMappie<DomainNoticeStatus, NoticeStatus>()

/**
 * 新建公告输入映射器:GraphQL 输入 → 服务请求
 */
object CreateNoticeInputMapper : ObjectMappie<CreateNoticeInput, CreateNoticeRequest>() {
  override fun map(from: CreateNoticeInput): CreateNoticeRequest =
    mapping {
      to::type fromExpression { NoticeTypeInputMapper.map(from.type) }
      to::pinned fromValue (from.pinned ?: false)
    }
}

/**
 * 编辑公告输入映射器
 */
object UpdateNoticeInputMapper : ObjectMappie<UpdateNoticeInput, UpdateNoticeRequest>() {
  override fun map(from: UpdateNoticeInput): UpdateNoticeRequest =
    mapping {
      to::type fromExpression { NoticeTypeInputMapper.map(from.type) }
      to::pinned fromValue (from.pinned ?: false)
    }
}

object NoticeTypeInputMapper : EnumMappie<NoticeType, DomainNoticeType>()

/**
 * 公告列表查询输入映射器
 */
object NoticeListInputMapper : ObjectMappie<NoticeListInput, NoticeListRequest>() {
  override fun map(from: NoticeListInput): NoticeListRequest =
    mapping {
      to::page fromValue (from.page ?: 0)
      to::size fromValue (from.size ?: 20)
      to::type fromExpression { from.type?.let { NoticeTypeInputMapper.map(it) } }
      to::status fromExpression { from.status?.let { NoticeStatusInputMapper.map(it) } }
    }
}

object NoticeStatusInputMapper : EnumMappie<NoticeStatus, DomainNoticeStatus>()
