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

package dev.yidafu.aqua.payment.domain.repository

import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import org.springframework.data.domain.Page
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * 退款记录仓储(JPA→Exposed 迁移 P4):剥离 JpaRepository,仅保留实际被调用的方法
 * (findByPaymentId/findByRefundId/findByOutRefundNo 无调用方,已废除);
 * 查询过滤 is_deleted;含聚合/分页方法。
 */
interface PaymentRefundRepository {
  /** 按 id 单行查询:命中且未软删返回实体,不存在或已软删返回 [Optional.empty]。 */
  fun findById(id: Long): Optional<PaymentRefundModel>

  /**
   * upsert 语义:无 id 或 id 不存在时插入(雪花 id 由 [dev.yidafu.aqua.common.id.SnowflakeIdGenerator] 回填),
   * 否则按 id 更新业务列(更新时 updated_at = now)。
   */
  fun save(entity: PaymentRefundModel): PaymentRefundModel

  /**
   * 日期窗口内已退款金额合计(分)
   */
  fun sumRefundedAmountByProcessedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long

  /**
   * 日期窗口内按日聚合的已退款金额(分,键为退款成功时间所在日期)
   */
  fun sumRefundedAmountGroupByDay(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Map<LocalDate, Long>

  /**
   * 退款申请分页查询(R-PAY-02 后台退款管理页):
   * status 为空=全部状态;日期按创建时间过滤(空=不限);按创建时间倒序(id 破平防跨页重复);content+count 同事务。
   */
  fun findRefundRequests(
    status: RefundStatus?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    page: Int,
    size: Int,
  ): Page<PaymentRefundModel>

  /**
   * 日期窗口内已退款(COMPLETED)记录分页查询(微信资金对账-退款对账用):
   * 口径同聚合查询 = status=COMPLETED 按退款成功时间(refundedAt/processed_at)归属闭区间窗口;
   * 按退款成功时间倒序(id 破平防跨页重复),与微信退款账单按退款成功日的归属对齐;content+count 同事务。
   */
  fun findRefundedByDateRange(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    page: Int,
    size: Int,
  ): Page<PaymentRefundModel>
}
