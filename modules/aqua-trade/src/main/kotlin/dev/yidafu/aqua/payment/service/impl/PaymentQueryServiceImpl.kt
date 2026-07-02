package dev.yidafu.aqua.payment.service.impl

import dev.yidafu.aqua.api.service.payment.PaymentQueryService
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PaymentQueryServiceImpl(
  private val paymentRepository: PaymentRepository,
) : PaymentQueryService {
  override fun getByDateRange(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel> = paymentRepository.findByCreatedAtBetween(startDate, endDate)
}
