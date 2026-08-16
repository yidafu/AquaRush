package dev.yidafu.aqua.api.service.payment

import dev.yidafu.aqua.common.domain.model.PaymentModel
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 支付/退款查询服务(跨模块,实现位于 aqua-trade;CLAUDE.md 规则 4)
 */
interface PaymentQueryService {
  fun getByDateRange(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel>

  /**
   * 日期窗口内已退款金额合计(分)。
   * 口径 = payment_refunds.status=COMPLETED 按退款成功时间(processed_at)归属窗口
   * (PRD 08-statistics.md §F-STA-002 退款/净营收维度)。
   */
  fun sumRefundedAmountCents(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long

  /**
   * 日期窗口内按日聚合的已退款金额(分,status=COMPLETED,键为退款成功时间所在日期)。
   * 周/月趋势由服务层按日滚动聚合(与订单趋势同桶口径)。
   */
  fun getDailyRefundedAmountCents(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Map<LocalDate, Long>
}
