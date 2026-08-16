package dev.yidafu.aqua.payment.service.impl

import dev.yidafu.aqua.api.service.payment.PaymentQueryService
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PaymentQueryServiceImpl(
  private val paymentRepository: PaymentRepository,
  private val paymentRefundRepository: PaymentRefundRepository,
) : PaymentQueryService {
  override fun getByDateRange(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel> = paymentRepository.findByCreatedAtBetween(startDate, endDate)

  override fun sumRefundedAmountCents(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long = paymentRefundRepository.sumRefundedAmountByProcessedAtBetween(startDate, endDate)

  override fun getDailyRefundedAmountCents(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Map<LocalDate, Long> = paymentRefundRepository.sumRefundedAmountGroupByDay(startDate, endDate)
}
