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

package dev.yidafu.aqua.payment.event

import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper

/**
 * 支付到期检查延迟消息消费者(R-ORD-04 支付超时优化):
 * 监听 payment-expiry-events 独立队列(仅 admin 服务注册,client 不消费不处理),
 * 到期消息触发 [PaymentService.handlePaymentTimeout] 自动取消待支付订单;
 * 幂等与互斥由数据库乐观锁 CAS 认领(claimExpired)保证,重复投递/重投均安全;
 * 处理失败抛出异常 → 事务回滚 + Artemis 重投;延迟消息丢失由兜底扫描覆盖。
 */
@Component
@AdminService
class PaymentExpiryCheckConsumer(
  private val paymentService: PaymentService,
) {
  private val logger = LoggerFactory.getLogger(PaymentExpiryCheckConsumer::class.java)
  private val objectMapper = jacksonObjectMapper()

  @JmsListener(destination = EventQueueConst.PAYMENT_EXPIRY_QUEUE_NAME)
  fun handlePaymentExpiryCheck(event: DomainEvent) {
    try {
      val eventData =
        objectMapper.readValue<Map<String, Any>>(
          event.payload,
          objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java),
        )
      val orderId = (eventData["orderId"] as Number).toLong()
      logger.debug("处理支付到期检查消息: 订单 {}", orderId)
      paymentService.handlePaymentTimeout(orderId)
    } catch (e: Exception) {
      logger.error("支付到期检查消息处理失败: eventId={}", event.id, e)
      throw e // 触发 Artemis 重投(消费端幂等,重投安全)
    }
  }
}
