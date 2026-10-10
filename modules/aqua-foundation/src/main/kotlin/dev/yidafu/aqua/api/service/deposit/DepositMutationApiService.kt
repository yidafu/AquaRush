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

import dev.yidafu.aqua.api.dto.DepositCalculation
import dev.yidafu.aqua.api.dto.EmptyBarrelRecoveryItem
import dev.yidafu.aqua.api.dto.OrderDepositItem
import dev.yidafu.aqua.common.domain.model.DepositRecordModel

/**
 * 水桶押金变更服务接口(R-MON-03 押金模块,实现位于 aqua-marketing;AGENTS.md 规则 3)
 * 覆盖随单收押链路(试算/建账/实收确认/取消清账)与订单退款联动(HELD→REFUNDED 幂等)。
 */
interface DepositMutationApiService {
  /**
   * 押金试算(不落库;预览与下单同口径):
   * 按本单各规格桶数 × 启用押金价目计算,同规格已有 HELD/PENDING 记录(在租只收一次)或
   * 未配置启用价目的规格不出行(R-MON-03/R-PRO-04)。
   */
  fun calcOrderDeposit(
    userId: Long,
    items: List<OrderDepositItem>,
  ): DepositCalculation

  /**
   * 随单建押金账(同事务):按试算结果落 deposit_records(PENDING),
   * 支付成功后经 [markHeldByOrder] 置 HELD。
   */
  fun createPendingDeposits(
    orderId: Long,
    userId: Long,
    calculation: DepositCalculation,
  ): List<DepositRecordModel>

  /**
   * 支付成功/代客下单确认:PENDING → HELD(实收确认;返回受影响行数)。
   */
  fun markHeldByOrder(orderId: Long): Int

  /**
   * 取消/支付超时:软删该订单全部 PENDING 押金记录(未实收清账;返回受影响行数)。
   */
  fun cancelPendingByOrder(orderId: Long): Int

  /**
   * 完成配送登记空桶回收(F-ORD-011,非强制):按规格落 barrel_recovery_records
   * (欠桶 = 已交付 − 已回收,R-PRO-04);返回登记行数。
   */
  fun registerBarrelRecovery(
    workerId: Long?,
    orderId: Long,
    items: List<EmptyBarrelRecoveryItem>,
  ): Int

  /**
   * 按订单退还全部有效押金(订单取消/退款成功后联动;状态谓词条件更新幂等)
   */
  fun markRefundedByOrder(
    orderId: Long,
    refundedBy: Long?,
    remark: String?,
  ): List<DepositRecordModel>
}
