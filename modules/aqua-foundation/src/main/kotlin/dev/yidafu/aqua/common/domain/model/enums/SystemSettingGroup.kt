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

package dev.yidafu.aqua.common.domain.model.enums

/**
 * 系统参数分组
 * 与 PRD 11-system-settings.md §F-SYS-001 保持一致(订单与履约/库存/安全/通知/数据维护),
 * 当前仅 订单与履约(F-SYS-002)、库存(F-SYS-003) 有种子参数,其余分组预留。
 */
enum class SystemSettingGroup(
  val label: String,
) {
  ORDER_AND_FULFILLMENT("订单与履约"),
  INVENTORY("库存"),
  SECURITY("安全"),
  NOTIFICATION("通知"),
  DATA_MAINTENANCE("数据维护"),
}
