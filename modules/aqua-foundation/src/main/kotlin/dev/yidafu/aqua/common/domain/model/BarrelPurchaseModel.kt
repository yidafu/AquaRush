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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import java.time.LocalDateTime

/**
 * 水桶购买单(G06:一次性塑料水桶费,独立支付,与商品订单不捆绑):
 * 每用户总限购一次(存在 PAID 即拦截,PENDING 重复发起先取消旧单,REFUNDED 后可重新购买);
 * orderNo 作为微信支付 out_trade_no(BK 前缀);支付回调成功后按 PENDING→PAID 原子流转并记录交易号,幂等;
 * 退还由管理员手动执行(PENDING/PAID→REFUNDED)。
 */
open class BarrelPurchaseModel(
  var id: Long? = null,
  val userId: Long = 0,
  var orderNo: String = "",
  var totalAmountCents: Long = 0,
  var paymentStatus: BarrelPaymentStatus = BarrelPaymentStatus.PENDING,
  var transactionId: String? = null,
  var paidAt: LocalDateTime? = null,
  var refundedAt: LocalDateTime? = null,
  var refundedBy: Long? = null,
  var refundRemark: String? = null,
  var remark: String? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
