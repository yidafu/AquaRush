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

package dev.yidafu.aqua.logging.mapper

import dev.yidafu.aqua.api.dto.BusinessLogListRequest
import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import dev.yidafu.aqua.common.graphql.generated.BizLogLevel
import dev.yidafu.aqua.common.graphql.generated.BusinessLog
import dev.yidafu.aqua.common.graphql.generated.BusinessLogListInput
import tech.mappie.api.ObjectMappie

/**
 * 业务日志映射器:域实体 → GraphQL 类型
 */
object BusinessLogMapper : ObjectMappie<BusinessLogModel, BusinessLog>() {
  override fun map(from: BusinessLogModel): BusinessLog =
    mapping {
      to::id fromValue (from.id ?: 0L)
      to::level fromExpression { BizLogLevel.valueOf(from.level) }
    }
}

/**
 * 业务日志列表查询输入映射器:GraphQL 输入 → 服务请求
 */
object BusinessLogListInputMapper : ObjectMappie<BusinessLogListInput, BusinessLogListRequest>() {
  override fun map(from: BusinessLogListInput): BusinessLogListRequest =
    mapping {
      to::page fromValue (from.page ?: 0)
      to::size fromValue (from.size ?: 10)
      to::domain fromExpression { src -> src.domain?.name }
      to::level fromExpression { src -> src.level?.name }
    }
}
