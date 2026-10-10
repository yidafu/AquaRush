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

package dev.yidafu.aqua.order.event

import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.common.messaging.consumer.EventProcessor
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper

@Component
class OrderCancelledHandler(
  private val orderRepository: OrderRepository,
  private val paymentRepository: PaymentRepository,
  private val paymentRefundRepository: PaymentRefundRepository,
  private val orderOperationService: OrderOperationService,
) : EventProcessor {
  private val logger = LoggerFactory.getLogger(OrderCancelledHandler::class.java)
  private val objectMapper = jacksonObjectMapper()

  override fun getSupportedEventType(): DomainEventType = DomainEventType.ORDER_CANCELLED

  /**
   * 处理订单取消事件
   */
  @Transactional
  override fun handle(event: DomainEvent) {
    try {
      // 解析payload获取事件数据
      val eventData =
        objectMapper.readValue<Map<String, Any>>(
          event.payload,
          objectMapper.typeFactory.constructMapType(Map::class.java, String::class.java, Any::class.java),
        )

      val orderId = eventData["orderId"].toString().toLong()
      val order =
        orderRepository
          .findById(orderId)
          .orElseThrow { IllegalStateException("Order not found: $orderId") }

      val shouldRefund = eventData["shouldRefund"] as? Boolean ?: false
      val paymentTransactionId = eventData["paymentTransactionId"] as? String

      logger.info("Processing ORDER_CANCELLED event for order: ${order.orderNo}, shouldRefund: $shouldRefund")

      // 记录订单操作
      val cancelDescription = if (shouldRefund) "用户取消订单（需退款）" else "用户取消订单"
      orderOperationService.recordOperation(
        orderId = order.id!!,
        operationType = OrderOperationType.ORDER_CANCELLED,
        operatorType = OperatorType.USER,
        operatorId = order.userId,
        description = cancelDescription,
      )

      // 已支付取消:生成退款申请(R-PAY-02 审核制),微信退款在后台审核通过时执行
      if (shouldRefund && !paymentTransactionId.isNullOrEmpty()) {
        createRefundRequest(order, paymentTransactionId)
      }

      logger.info("Successfully processed ORDER_CANCELLED event for order: ${order.orderNo}")
    } catch (e: Exception) {
      logger.error("Failed to process ORDER_CANCELLED event: ${event.id}", e)
      throw e // 重新抛出异常以触发重试机制
    }
  }

  /**
   * 生成退款申请(R-PAY-02 审核制):取消已支付单不再直接执行微信退款,
   * 改为落 `payment_refunds`(PENDING) 待后台审核,审核通过([dev.yidafu.aqua.payment.service.RefundReviewService])
   * 才执行微信退款并回写本条记录;
   * 金额口径 = 商品+配送费(与审核执行一致,缺陷 #5;押金由审核时按订单快照单独退还);
   * 幂等:同一订单至多一条订单退款申请,Outbox 重投不重复生成。
   */
  private fun createRefundRequest(
    order: OrderModel,
    paymentTransactionId: String,
  ) {
    if (paymentRefundRepository.findByOrderIdAndRefundType(order.id!!, RefundType.ORDER_REFUND).isPresent) {
      logger.info("退款申请已存在,跳过生成: orderNo={}", order.orderNo)
      return
    }

    val refundAmount = order.amountCents + order.deliveryFeeCents
    if (refundAmount <= 0) {
      logger.warn("可退金额为 0,跳过退款申请生成: orderNo={}", order.orderNo)
      return
    }

    val payment =
      paymentRepository
        .findByTransactionId(paymentTransactionId)
        .orElse(null)
        ?: paymentRepository.findByOrderId(order.id!!).orElse(null)
    if (payment?.id == null) {
      // 无支付记录无法关联申请(payment_id 外键);退款需人工核对处理
      logger.error("退款申请未命中支付记录,跳过生成(需人工核对): transactionId={}, orderNo={}", paymentTransactionId, order.orderNo)
      return
    }

    paymentRefundRepository.save(
      PaymentRefundModel(
        paymentId = payment.id!!,
        orderId = order.id!!,
        userId = order.userId,
        refundAmount = refundAmount,
        refundReason = "订单取消退款 - 订单号: ${order.orderNo}",
        status = RefundStatus.PENDING,
        refundType = RefundType.ORDER_REFUND,
      ),
    )

    // 操作时间线:退款申请已生成,待后台审核
    orderOperationService.recordOperation(
      orderId = order.id!!,
      operationType = OrderOperationType.REFUND_INITIATED,
      operatorType = OperatorType.SYSTEM,
      operatorId = order.userId,
      description = "取消订单,已生成退款申请,待后台审核(R-PAY-02)",
      extraData = """{"refundAmountCents": $refundAmount}""",
    )

    logger.info("退款申请已生成(待审核): orderNo={}, amount={} 分", order.orderNo, refundAmount)
  }
}
