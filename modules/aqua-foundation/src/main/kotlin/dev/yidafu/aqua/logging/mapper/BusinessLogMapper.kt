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

import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import dev.yidafu.aqua.common.graphql.fromValue
import dev.yidafu.aqua.common.graphql.generated.BizLogLevel
import dev.yidafu.aqua.common.graphql.generated.BusinessLogVo

/**
 * 业务日志领域模型 → GraphQL 类型
 */
fun BusinessLogModel.toGraphQL(): BusinessLogVo =
  BusinessLogVo(
    correlationId = correlationId,
    createdAt = createdAt,
    id = id ?: 0L,
    level = BizLogLevel.fromValue(level),
    loggerName = loggerName,
    message = message,
    operation = operation,
    stackTrace = stackTrace,
    userId = userId,
    username = username,
  )
