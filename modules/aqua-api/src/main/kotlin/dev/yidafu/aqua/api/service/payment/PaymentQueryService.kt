package dev.yidafu.aqua.api.service.payment

import dev.yidafu.aqua.common.domain.model.PaymentModel
import java.time.LocalDateTime

interface PaymentQueryService {
  fun getByDateRange(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel>
}
