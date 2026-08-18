package dev.yidafu.aqua.api.service.payment

import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import org.springframework.data.domain.Page
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
   * 退款申请分页查询(R-PAY-02 后台退款管理页):status 为空=全部状态;
   * 日期按创建时间过滤(空=不限);按创建时间倒序。
   */
  fun getRefundRequests(
    status: RefundStatus?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    page: Int,
    size: Int,
  ): Page<PaymentRefundModel>

  /**
   * 退款申请详情(R-PAY-02 后台退款管理页)
   */
  fun getRefundRequest(id: Long): PaymentRefundModel?

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
