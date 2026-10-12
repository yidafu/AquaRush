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
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 支付结果主动查单兜底(官方《支付回调和查单实现指引》§5.2 定时轮询查单):
 * 支付结果回调丢失时,主动调用微信查单接口核实未到期的待支付记录,避免用户已付款
 * 而订单停在「待支付」直至超时取消(仅 admin 服务运行,client 不轮询);
 * 查询由 [PaymentService.queryPaymentStatus] 统一完成——查得 trade_state=SUCCESS 且
 * 金额一致时走与回调共用的幂等完成路径流转订单;假支付模式/未配置商户参数时
 * 该方法直接返回 UNKNOWN,不会发起微信查单;
 * 每轮以 id keyset 游标分批扫描 PENDING 且未到期的支付记录(禁全表,AGENTS.md 规则 10),
 * 处理中完成支付的行离开结果集也不会跳行。
 */
@Component
@AdminService
class PaymentStatusPollScheduler(
  private val paymentRepository: PaymentRepository,
  private val paymentService: PaymentService,
) {
  private val logger = LoggerFactory.getLogger(PaymentStatusPollScheduler::class.java)

  companion object {
    /** 单批游标行数(分页有界;此业务单店量级下待支付记录远小于此值) */
    private const val POLL_BATCH_SIZE = 100
  }

  @Scheduled(
    fixedDelayString = "\${aqua.payment.poll.scan-interval-ms:60000}",
    initialDelayString = "\${aqua.payment.poll.scan-initial-delay-ms:60000}",
  )
  fun pollPendingPayments() {
    val now = LocalDateTime.now()
    var lastId = 0L
    while (true) {
      val batch =
        paymentRepository.findPollableByStatusAfterId(
          status = PaymentStatus.PENDING,
          expiredAt = now,
          afterId = lastId,
          limit = POLL_BATCH_SIZE,
        )
      if (batch.isEmpty()) {
        break
      }
      batch.forEach { payment ->
        try {
          paymentService.queryPaymentStatus(payment.orderId)
        } catch (e: Exception) {
          logger.error("支付查单兜底失败: 支付ID={}, 订单ID={}", payment.id, payment.orderId, e)
        }
      }
      val nextId = batch.last().id
      if (nextId == null || batch.size < POLL_BATCH_SIZE) {
        break
      }
      lastId = nextId
    }
  }
}
