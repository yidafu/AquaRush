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

import dev.yidafu.aqua.api.dto.SubmitDepositRefundRequest
import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel

/**
 * 退押申请变更服务(F-PRO-010/R-PRO-04 状态机,实现位于 aqua-marketing;AGENTS.md 规则 3):
 * 提交(待处理)→ 空桶回收确认(已回收)→ 审核退还(已退还)/ 驳回(已驳回)。
 * 退还幂等以押金记录状态谓词为准(R-MON-03 防并发重复退还)。
 */
interface DepositRefundMutationService {
  /**
   * 用户提交退押申请:桶数 ≤ 在租押金桶数;应退金额按押金记录快照计算(服务端,R-MON-01);
   * 同规格存在处理中(PENDING/RECOVERED)申请时拦截。
   */
  fun submit(
    userId: Long,
    request: SubmitDepositRefundRequest,
  ): DepositRefundApplicationModel

  /**
   * 空桶回收确认:PENDING → RECOVERED(门店/送水工回收空桶后由管理员确认)。
   */
  fun confirmRecovered(
    applicationId: Long,
    adminId: Long,
  ): DepositRefundApplicationModel

  /**
   * 审核通过:RECOVERED → REFUNDED——执行微信退款(DEPOSIT_REFUND 独立账本,不计退款统计)
   * + 覆盖押金记录 HELD → REFUNDED(条件更新幂等);现金/历史无交易号单仅落账本(线下退款)。
   * 微信退款失败抛异常,申请保持 RECOVERED 可重试。
   */
  fun approve(
    applicationId: Long,
    adminId: Long,
  ): DepositRefundApplicationModel

  /**
   * 驳回:PENDING/RECOVERED → REJECTED(留原因)。
   */
  fun reject(
    applicationId: Long,
    adminId: Long,
    reason: String,
  ): DepositRefundApplicationModel
}
