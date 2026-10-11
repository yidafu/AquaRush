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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.admin.order.mapper.AdminOrderMapper
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.graphql.generated.OrderVo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.order.mapper.maskCustomerContactsForWorker

/**
 * 订单联系方式隐私(F-ORD-032/R-ORD-17)的调用方判定:
 * 管理员/超管不受限(AC-ORD-032-04),仅送水工角色按订单状态脱敏。
 * 主体缺失或类型不符时按送水工处理(fail-closed,最小可见)。
 */
fun UserPrincipal?.isWorkerViewer(): Boolean =
  when (this) {
    null -> true
    else -> !(hasRole("ADMIN") || hasRole("SUPER_ADMIN"))
  }

/**
 * 按调用方角色组装订单输出(F-ORD-032):
 * 送水工 → 按订单状态脱敏收货人电话与下单账号电话(服务端完成,完整号码不下发、
 * 无「查看完整号码」旁路 —— AC-ORD-032-05);管理员/超管 → 完整号码(AC-ORD-032-04)。
 */
fun AdminOrderMapper.mapForViewer(
  order: OrderModel,
  viewer: UserPrincipal?,
): OrderVo {
  val vo = map(order)
  return if (viewer.isWorkerViewer()) vo.maskCustomerContactsForWorker(order.status) else vo
}
