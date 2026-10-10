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

package dev.yidafu.aqua.api.service.barrel

import dev.yidafu.aqua.api.dto.PurchaseBarrelRequest
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel

/**
 * 水桶购买单变更服务(G06 水桶费模块,实现位于 aqua-marketing;AGENTS.md 规则 3)
 */
interface BarrelPurchaseMutationService {
  /**
   * 购桶(校验价目 ENABLED + 每用户总限购一次;创建 PENDING 购桶单与明细快照,
   * orderNo 作为支付 out_trade_no;重复发起时先取消旧 PENDING 单)
   */
  fun purchaseBarrel(
    userId: Long,
    request: PurchaseBarrelRequest,
  ): BarrelPurchaseModel

  /**
   * 支付成功入账(幂等):PENDING→PAID 原子流转并记录交易号/支付时间;
   * 重复回调 no-op。
   */
  fun handleBarrelPurchasePaid(
    purchaseId: Long,
    transactionId: String,
  ): BarrelPurchaseModel

  /** 管理员手动退还(校验 PAID;置 REFUNDED + 退还人/备注/时间;幂等) */
  fun markRefunded(
    purchaseId: Long,
    refundedBy: Long,
    remark: String?,
  ): BarrelPurchaseModel
}
