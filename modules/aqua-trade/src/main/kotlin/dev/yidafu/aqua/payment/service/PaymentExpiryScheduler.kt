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

package dev.yidafu.aqua.payment.service

import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 支付超时扫描(R-ORD-04:待支付订单超过支付时限默认 30 分钟自动取消并恢复库存):
 * 周期扫描 PENDING 且已过期的支付记录,触发 [PaymentService.handlePaymentTimeout];
 * 超时处理后按订单终态同步支付记录(取消→EXPIRED,已支付→SUCCESS),幂等可重入。
 */
@Component
class PaymentExpiryScheduler(
  private val paymentRepository: PaymentRepository,
  private val paymentService: PaymentService,
  private val orderQueryService: OrderQueryService,
) {
  private val logger = LoggerFactory.getLogger(PaymentExpiryScheduler::class.java)

  @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
  fun expirePendingPayments() {
    val now = LocalDateTime.now()
    val expiredPayments =
      paymentRepository
        .findByStatus(PaymentStatus.PENDING)
        .filter { payment -> payment.expiredAt != null && payment.expiredAt!!.isBefore(now) }

    expiredPayments.forEach { payment ->
      try {
        paymentService.handlePaymentTimeout(payment.orderId)
        val order = orderQueryService.getOrderById(payment.orderId)
        when (order.status) {
          // 超时取消:支付记录置 EXPIRED
          OrderModelStatus.CANCELLED -> {
            payment.status = PaymentStatus.EXPIRED
            paymentRepository.save(payment)
            logger.info("支付超时处理完成: 订单 {} 已自动取消", payment.orderId)
          }
          // 回调与超时竞态且回调胜出:支付记录同步为 SUCCESS
          OrderModelStatus.PENDING_DELIVERY -> {
            if (payment.status != PaymentStatus.SUCCESS) {
              payment.status = PaymentStatus.SUCCESS
              payment.transactionId = order.paymentTransactionId
              payment.paidAt = order.paymentTime
              paymentRepository.save(payment)
            }
          }
          else -> logger.debug("订单 {} 状态为 {},跳过支付记录同步", payment.orderId, order.status)
        }
      } catch (e: Exception) {
        logger.error("支付超时处理失败: 订单 {}", payment.orderId, e)
      }
    }
  }
}
