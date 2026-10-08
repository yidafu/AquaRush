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

package dev.yidafu.aqua.api.service.deposit

import dev.yidafu.aqua.common.domain.model.DepositRecordModel

/**
 * 历史桶押金记录变更服务接口(G06 押金模块,实现位于 aqua-marketing;CLAUDE.md 规则 4)
 * 2026-08 水桶费改造后押金语义废弃:新购桶走 barrel_purchases;
 * 本接口仅保留「按订单退还全部有效押金」供历史订单(orders.deposit_amount_cents>0)取消退款兼容。
 */
interface DepositMutationApiService {
  /**
   * 按订单退还全部有效押金(历史订单取消退款成功后联动;幂等)
   */
  fun markRefundedByOrder(
    orderId: Long,
    refundedBy: Long?,
    remark: String?,
  ): List<DepositRecordModel>
}
