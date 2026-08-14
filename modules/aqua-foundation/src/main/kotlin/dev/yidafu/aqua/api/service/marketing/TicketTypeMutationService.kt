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

package dev.yidafu.aqua.api.service.marketing

import dev.yidafu.aqua.api.dto.TicketTypeCreateRequest
import dev.yidafu.aqua.api.dto.TicketTypeUpdateRequest
import dev.yidafu.aqua.common.domain.model.TicketTypeModel

/**
 * 桶水票票种变更服务(管理后台,PRD 10-marketing.md §F-MKT-007)
 * 票种/套餐配置变更不影响已购票(快照)。
 */
interface TicketTypeMutationService {
  fun createTicketType(
    adminId: Long,
    adminName: String,
    request: TicketTypeCreateRequest,
  ): TicketTypeModel

  fun updateTicketType(
    adminId: Long,
    adminName: String,
    id: Long,
    request: TicketTypeUpdateRequest,
  ): TicketTypeModel

  /**
   * 停用票种(用户端水票商城不再展示,不可再购)
   */
  fun disableTicketType(
    adminId: Long,
    adminName: String,
    id: Long,
  ): TicketTypeModel
}
