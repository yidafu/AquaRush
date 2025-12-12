/*
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

package dev.yidafu.aqua.notice.service

import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.notice.domain.model.MessageType
import dev.yidafu.aqua.notice.dto.WeChatMessageData
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class MessageEventListener(
  private val subscriptionService: SubscriptionService,
  private val weChatMessagePushService: WeChatMessagePushService,
  private val eventPublishService: SimplifiedEventPublishService,
) {
  private val logger = LoggerFactory.getLogger(MessageEventListener::class.java)
  private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  @Async
  @EventListener
  fun handleOrderCreated(order: OrderModel) {
    logger.info("Handling order created event for order ${order.id}")

    try {
      val enabled =
        subscriptionService.isNotificationEnabled(
          order.userId,
          MessageType.ORDER_CREATED,
        )

      if (enabled) {
        val templateData =
          mapOf(
            "thing1" to WeChatMessageData("订单号", order.id.toString()),
            "thing2" to WeChatMessageData("下单时间", order.createdAt.format(dateFormatter)),
            "amount3" to WeChatMessageData("订单金额", "¥${order.totalAmount}"),
          )

        // We need to get user's WeChat openId - this would typically come from the user service
        // For now, we'll use a placeholder implementation
        val openId = getUserOpenId(order.userId)

        if (openId != null) {
          weChatMessagePushService.sendMessage(
            userId = order.userId,
            openId = openId,
            messageType = MessageType.ORDER_CREATED,
            templateData = templateData.mapValues { it.value.value },
            page = "/pages/order/detail?id=${order.id}",
          )
        } else {
          logger.warn("User ${order.userId} does not have a WeChat openId")
        }
      }
    } catch (e: Exception) {
      logger.error("Failed to handle order created event for order ${order.id}", e)
      eventPublishService.publishDomainEvent(
        "notification_failed",
        order.id.toString(),
        mapOf<String, Any>(
          "userId" to order.userId,
          "messageType" to MessageType.ORDER_CREATED.templateId,
          "error" to (e.message ?: ""),
        ),
      )
    }
  }

  @Async
  @EventListener
  fun handleOrderPaid(payment: PaymentModel) {
    logger.info("Handling order paid event for payment ${payment.id}")

    try {
      val enabled =
        subscriptionService.isNotificationEnabled(
          payment.userId,
          MessageType.ORDER_PAID,
        )

      if (enabled) {
        val templateData =
          mapOf(
            "thing1" to WeChatMessageData("订单号", payment.orderId.toString()),
            "thing2" to WeChatMessageData("支付金额", "¥${payment.amount}"),
            "time3" to WeChatMessageData("支付时间", payment.paidAt?.format(dateFormatter) ?: ""),
          )

        val openId = getUserOpenId(payment.userId)

        if (openId != null) {
          weChatMessagePushService.sendMessage(
            userId = payment.userId,
            openId = openId,
            messageType = MessageType.ORDER_PAID,
            templateData = templateData.mapValues { it.value.value },
            page = "/pages/order/detail?id=${payment.orderId}",
          )
        } else {
          logger.warn("User ${payment.userId} does not have a WeChat openId")
        }
      }
    } catch (e: Exception) {
      logger.error("Failed to handle order paid event for payment ${payment.id}", e)
      eventPublishService.publishDomainEvent(
        "notification_failed",
        payment.id.toString(),
        mapOf<String, Any>(
          "userId" to payment.userId,
          "messageType" to MessageType.ORDER_PAID.templateId,
          "error" to (e.message ?: ""),
        ),
      )
    }
  }

  @Async
  @EventListener
  fun handleOrderCancelled(order: OrderModel) {
    logger.info("Handling order cancelled event for order ${order.id}")

    try {
      val enabled =
        subscriptionService.isNotificationEnabled(
          order.userId,
          MessageType.ORDER_CANCELLED,
        )

      if (enabled) {
        val templateData =
          mapOf(
            "thing1" to WeChatMessageData("订单号", order.id.toString()),
            "thing2" to WeChatMessageData("取消时间", order.updatedAt.format(dateFormatter)),
            "thing3" to WeChatMessageData("取消原因", "用户主动取消"),
          )

        val openId = getUserOpenId(order.userId)

        if (openId != null) {
          weChatMessagePushService.sendMessage(
            userId = order.userId,
            openId = openId,
            messageType = MessageType.ORDER_CANCELLED,
            templateData = templateData.mapValues { it.value.value },
            page = "/pages/order/detail?id=${order.id}",
          )
        } else {
          logger.warn("User ${order.userId} does not have a WeChat openId")
        }
      }
    } catch (e: Exception) {
      logger.error("Failed to handle order cancelled event for order ${order.id}", e)
      eventPublishService.publishDomainEvent(
        "notification_failed",
        order.id.toString(),
        mapOf<String, Any>(
          "userId" to order.userId,
          "messageType" to MessageType.ORDER_CANCELLED.templateId,
          "error" to (e.message ?: ""),
        ),
      )
    }
  }

  @Async
  @EventListener
  fun handleDeliveryStarted(deliveryData: Map<String, Any>) {
    logger.info("Handling delivery started event")

    val userId = deliveryData["userId"] as? Long ?: return
    val orderId = deliveryData["orderId"] as? Long ?: return
    try {
      val deliveryWorkerName = deliveryData["deliveryWorkerName"] as? String ?: "配送员"

      val enabled =
        subscriptionService.isNotificationEnabled(
          userId,
          MessageType.DELIVERY_STARTED,
        )

      if (enabled) {
        val templateData =
          mapOf(
            "thing1" to WeChatMessageData("订单号", orderId.toString()),
            "thing2" to WeChatMessageData("配送员", deliveryWorkerName),
            "time3" to WeChatMessageData("配送开始时间", LocalDateTime.now().format(dateFormatter)),
          )

        val openId = getUserOpenId(userId)

        if (openId != null) {
          weChatMessagePushService.sendMessage(
            userId = userId,
            openId = openId,
            messageType = MessageType.DELIVERY_STARTED,
            templateData = templateData.mapValues { it.value.value },
            page = "/pages/order/detail?id=$orderId",
          )
        } else {
          logger.warn("User $userId does not have a WeChat openId")
        }
      }
    } catch (e: Exception) {
      logger.error("Failed to handle delivery started event", e)
      eventPublishService.publishDomainEvent(
        "notification_failed",
        orderId.toString(),
        mapOf<String, Any>(
          "deliveryData" to deliveryData,
          "messageType" to MessageType.DELIVERY_STARTED.templateId,
          "error" to (e.message ?: ""),
        ),
      )
    }
  }

  @Async
  @EventListener
  fun handleOrderDelivered(deliveryData: Map<String, Any>) {
    logger.info("Handling order delivered event")

    val userId = deliveryData["userId"] as? Long ?: return
    val orderId = deliveryData["orderId"] as? Long ?: return
    val deliveryWorkerName = deliveryData["deliveryWorkerName"] as? String ?: "配送员"
    try {
      val enabled =
        subscriptionService.isNotificationEnabled(
          userId,
          MessageType.ORDER_DELIVERED,
        )

      if (enabled) {
        val templateData =
          mapOf(
            "thing1" to WeChatMessageData("订单号", orderId.toString()),
            "thing2" to WeChatMessageData("配送员", deliveryWorkerName),
            "time3" to WeChatMessageData("配送完成时间", LocalDateTime.now().format(dateFormatter)),
          )

        val openId = getUserOpenId(userId)

        if (openId != null) {
          weChatMessagePushService.sendMessage(
            userId = userId,
            openId = openId,
            messageType = MessageType.ORDER_DELIVERED,
            templateData = templateData.mapValues { it.value.value },
            page = "/pages/order/detail?id=$orderId",
          )
        } else {
          logger.warn("User $userId does not have a WeChat openId")
        }
      }
    } catch (e: Exception) {
      logger.error("Failed to handle order delivered event", e)
      eventPublishService.publishDomainEvent(
        "notification_failed",
        orderId.toString(),
        mapOf<String, Any>(
          "deliveryData" to deliveryData,
          "messageType" to MessageType.ORDER_DELIVERED.templateId,
          "error" to (e.message ?: ""),
        ),
      )
    }
  }

  private fun getUserOpenId(userId: Long): String? {
    // This is a placeholder implementation
    // In a real application, you would call the user service to get the WeChat openId
    // or store it in the user table
    logger.debug("Getting WeChat openId for user $userId")
    return null // Placeholder - should be implemented based on your user management system
  }
}
