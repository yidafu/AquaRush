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

import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.payment.config.PaymentExpiryProperties
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 支付超时兜底扫描(R-ORD-04 支付超时优化):
 * 延迟消息(MQ,payment-expiry-events)为主触发,本扫描为兜底安全网——仅 admin 服务运行
 * ([AdminService],client 不扫描),间隔默认 5 分钟(可配 `aqua.payment.expiry.scan-interval-ms`);
 * 分页游标分批扫描 PENDING 且已过期的支付记录(禁止全表拉取,AGENTS.md 规则 10),
 * 逐条触发 [PaymentService.handlePaymentTimeout];与 MQ 触发并发时由数据库乐观锁
 * CAS 认领(claimExpired)保证幂等与互斥,重复处理安全。
 */
@Component
@AdminService
class PaymentExpiryScheduler(
  private val paymentRepository: PaymentRepository,
  private val paymentService: PaymentService,
  private val properties: PaymentExpiryProperties,
) {
  private val logger = LoggerFactory.getLogger(PaymentExpiryScheduler::class.java)

  @Scheduled(
    fixedDelayString = "\${aqua.payment.expiry.scan-interval-ms:300000}",
    initialDelayString = "\${aqua.payment.expiry.scan-initial-delay-ms:30000}",
  )
  fun expirePendingPayments() {
    val now = LocalDateTime.now()
    var pageNumber = 0
    while (true) {
      val pageable = PageRequest.of(pageNumber, properties.scanBatchSize, Sort.by(Sort.Direction.ASC, "id"))
      val page = paymentRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now, pageable)
      page.content.forEach { payment ->
        try {
          paymentService.handlePaymentTimeout(payment.orderId)
        } catch (e: Exception) {
          logger.error("支付超时兜底处理失败: 订单 {}", payment.orderId, e)
        }
      }
      if (!page.hasNext()) {
        break
      }
      pageNumber++
    }
  }
}
