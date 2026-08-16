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

package dev.yidafu.aqua.notification.gateway

import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.messaging.consumer.EventProcessor
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.notification.dto.NotificationEventData
import dev.yidafu.aqua.notification.dto.NotificationTemplateScenes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 通知事件发布器:组装 NOTIFICATION_SEND 领域事件并发布到 notifications-events 队列
 * (PRD 13-notification.md:消息先推到 MQ,MQ 消费消息)。
 */
@Component
class NotificationEventPublisher(
  private val eventPublishService: SimplifiedEventPublishService,
) {
  private val objectMapper = jacksonObjectMapper()

  fun publish(data: NotificationEventData) {
    val map: Map<String, Any> = objectMapper.convertValue(data, object : TypeReference<Map<String, Any>>() {})
    eventPublishService.publishDomainEvent(
      eventType = DomainEventType.NOTIFICATION_SEND.value,
      aggregateId = data.relatedNo ?: "-",
      eventData = map,
    )
  }
}

/**
 * 各场景通知事件处理器基类(实现 EventProcessor,由 MessageConsumer 按事件类型分发)
 * 处理逻辑绝不抛异常(避免污染订单消费者触发整条消息重投);解析失败仅记录日志跳过。
 */
abstract class BaseNotificationEventHandler(
  protected val publisher: NotificationEventPublisher,
) : EventProcessor {
  protected val logger = LoggerFactory.getLogger(javaClass)
  protected val objectMapper = jacksonObjectMapper()

  override fun handle(event: DomainEvent) {
    try {
      val payload: Map<String, Any> =
        objectMapper.readValue(event.payload, object : TypeReference<Map<String, Any>>() {})
      handleEvent(event, payload)
    } catch (e: Exception) {
      logger.warn("通知事件处理失败, eventType={}, eventId={}: {}", event.eventType, event.id, e.message)
    }
  }

  protected abstract fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  )

  protected fun requireLong(
    data: Map<String, Any>,
    key: String,
  ): Long = data[key].toString().toLong()

  protected fun requireString(
    data: Map<String, Any>,
    key: String,
  ): String = data[key].toString()

  protected fun longOrNull(
    data: Map<String, Any>,
    key: String,
  ): Long? = data[key]?.toString()?.toLongOrNull()

  protected fun eventTimeMillis(data: Map<String, Any>): Long =
    longOrNull(data, "timestamp") ?: System.currentTimeMillis()

  /** 金额分 → 元字符串(展示用) */
  protected fun yuan(cents: Long): String = String.format("%.2f", cents / 100.0)

  protected fun formatTime(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
      .format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()))
}

/**
 * 订单创建通知(下单成功,用户端)
 */
@Component
class OrderCreatedNotificationHandler(
  publisher: NotificationEventPublisher,
) : BaseNotificationEventHandler(publisher) {
  override fun getSupportedEventType(): DomainEventType = DomainEventType.ORDER_CREATED

  override fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  ) {
    val orderId = requireLong(data, "orderId")
    val userId = requireLong(data, "userId")
    val amountCents = longOrNull(data, "amountCents") ?: 0L
    val eventTime = eventTimeMillis(data)
    publisher.publish(
      NotificationEventData(
        receiverType = "USER",
        userId = userId,
        messageType = "ORDER",
        scene = DomainEventType.ORDER_CREATED.value,
        title = "下单成功",
        summary = "您的订单($orderId)已提交,请尽快完成支付。",
        relatedNo = orderId.toString(),
        eventTime = eventTime,
        templateScene = NotificationTemplateScenes.ORDER_CREATED,
        page = "pages/order-detail/index?id=$orderId",
        templateFields =
          mapOf(
            "thing1" to "订单号:$orderId",
            "amount2" to "¥${yuan(amountCents)}",
            "time3" to formatTime(eventTime),
          ),
      ),
    )
  }
}

/**
 * 订单支付成功通知:用户端「支付成功」+ 送水工端「新订单可抢单」广播(F-NTF-003)
 */
@Component
class OrderPaidNotificationHandler(
  publisher: NotificationEventPublisher,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) : BaseNotificationEventHandler(publisher) {
  override fun getSupportedEventType(): DomainEventType = DomainEventType.ORDER_PAID

  override fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  ) {
    val orderId = requireLong(data, "orderId")
    val userId = requireLong(data, "userId")
    val amountCents = longOrNull(data, "amountCents") ?: 0L
    val eventTime = eventTimeMillis(data)

    // 用户端:支付成功
    publisher.publish(
      NotificationEventData(
        receiverType = "USER",
        userId = userId,
        messageType = "ORDER",
        scene = DomainEventType.ORDER_PAID.value,
        title = "支付成功",
        summary = "您的订单($orderId)已支付成功,等待配送。",
        relatedNo = orderId.toString(),
        eventTime = eventTime,
        templateScene = NotificationTemplateScenes.PAYMENT_RESULT,
        page = "pages/order-detail/index?id=$orderId",
        templateFields =
          mapOf(
            "thing1" to "订单号:$orderId",
            "amount2" to "¥${yuan(amountCents)}",
            "time3" to formatTime(eventTime),
          ),
      ),
    )

    // 送水工端:新订单(可抢单)广播,受 DELIVERY_PUSH_NEW_ORDER_ENABLED 开关控制
    val pushEnabled = systemConfigQueryApiService.getBooleanValue("delivery_push_new_order_enabled", true)
    if (!pushEnabled) {
      return
    }
    publisher.publish(
      NotificationEventData(
        receiverType = "WORKER",
        workerId = null,
        messageType = "ORDER",
        scene = "NEW_ORDER",
        title = "新订单待接单",
        summary = "新订单($orderId)待接单,金额 ¥${yuan(amountCents)}。",
        relatedNo = orderId.toString(),
        eventTime = eventTime,
        broadcast = true,
      ),
    )
  }
}

/**
 * 订单取消通知(用户端:取消结果/退款结果)
 */
@Component
class OrderCancelledNotificationHandler(
  publisher: NotificationEventPublisher,
) : BaseNotificationEventHandler(publisher) {
  override fun getSupportedEventType(): DomainEventType = DomainEventType.ORDER_CANCELLED

  override fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  ) {
    val orderId = requireLong(data, "orderId")
    val userId = requireLong(data, "userId")
    val reason = data["reason"]?.toString() ?: ""
    val shouldRefund = data["shouldRefund"]?.toString()?.toBooleanStrictOrNull() ?: false
    val eventTime = eventTimeMillis(data)
    val refundText = if (shouldRefund) "已发起退款,请留意到账。" else ""
    publisher.publish(
      NotificationEventData(
        receiverType = "USER",
        userId = userId,
        messageType = "ORDER",
        scene = DomainEventType.ORDER_CANCELLED.value,
        title = "订单已取消",
        summary = "您的订单($orderId)已取消。$refundText${if (reason.isNotBlank()) "原因:$reason" else ""}",
        relatedNo = orderId.toString(),
        eventTime = eventTime,
        templateScene = if (shouldRefund) NotificationTemplateScenes.PAYMENT_RESULT else null,
        page = "pages/order-detail/index?id=$orderId",
        templateFields =
          mapOf(
            "thing1" to "订单号:$orderId",
            "time3" to formatTime(eventTime),
          ),
      ),
    )
  }
}

/**
 * 支付超时通知(用户端)
 */
@Component
class PaymentTimeoutNotificationHandler(
  publisher: NotificationEventPublisher,
) : BaseNotificationEventHandler(publisher) {
  override fun getSupportedEventType(): DomainEventType = DomainEventType.PAYMENT_TIMEOUT

  override fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  ) {
    val orderId = requireLong(data, "orderId")
    val userId = requireLong(data, "userId")
    val eventTime = eventTimeMillis(data)
    publisher.publish(
      NotificationEventData(
        receiverType = "USER",
        userId = userId,
        messageType = "ORDER",
        scene = DomainEventType.PAYMENT_TIMEOUT.value,
        title = "支付超时",
        summary = "订单($orderId)未在有效时间内完成支付,已自动取消。",
        relatedNo = orderId.toString(),
        eventTime = eventTime,
        page = "pages/order-detail/index?id=$orderId",
        templateFields = mapOf("thing1" to "订单号:$orderId", "time3" to formatTime(eventTime)),
      ),
    )
  }
}

/**
 * 配送派单通知:用户端「配送员接单」+ 送水工端「被派单」
 */
@Component
class DeliveryAssignedNotificationHandler(
  publisher: NotificationEventPublisher,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
  private val orderQueryApiService: OrderQueryApiService,
) : BaseNotificationEventHandler(publisher) {
  override fun getSupportedEventType(): DomainEventType = DomainEventType.DELIVERY_ASSIGNED

  override fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  ) {
    val orderId = requireLong(data, "orderId")
    val userId = requireLong(data, "userId")
    val deliveryWorkerId = requireLong(data, "deliveryWorkerId")
    val eventTime = eventTimeMillis(data)
    val orderNo = orderQueryApiService.findById(orderId)?.orderNo ?: orderId.toString()

    // 用户端:配送员接单
    publisher.publish(
      NotificationEventData(
        receiverType = "USER",
        userId = userId,
        messageType = "ORDER",
        scene = DomainEventType.DELIVERY_ASSIGNED.value,
        title = "配送员接单",
        summary = "您的订单($orderId)已被接单,配送员即将为您配送。",
        relatedNo = orderId.toString(),
        eventTime = eventTime,
        templateScene = NotificationTemplateScenes.DELIVERY_ASSIGNED,
        page = "pages/order-detail/index?id=$orderId",
        templateFields = mapOf("thing1" to "订单号:$orderId", "time3" to formatTime(eventTime)),
      ),
    )

    // 送水工端:被派单
    val pushEnabled = systemConfigQueryApiService.getBooleanValue("delivery_push_assigned_enabled", true)
    if (!pushEnabled) {
      return
    }
    publisher.publish(
      NotificationEventData(
        receiverType = "WORKER",
        workerId = deliveryWorkerId,
        messageType = "ORDER",
        scene = DomainEventType.DELIVERY_ASSIGNED.value,
        title = "新派单",
        summary = "订单($orderNo)已派给您,请及时处理。",
        relatedNo = orderNo,
        eventTime = eventTime,
      ),
    )
  }
}

/**
 * 开始配送通知:用户端「开始配送」+ 送水工端「配送开始」
 */
@Component
class DeliveryStartedNotificationHandler(
  publisher: NotificationEventPublisher,
  private val orderQueryApiService: OrderQueryApiService,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) : BaseNotificationEventHandler(publisher) {
  override fun getSupportedEventType(): DomainEventType = DomainEventType.DELIVERY_STARTED

  override fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  ) {
    val orderId = requireLong(data, "orderId")
    val deliveryWorkerId = requireLong(data, "deliveryWorkerId")
    val eventTime = eventTimeMillis(data)
    val order = orderQueryApiService.findById(orderId)
    val userId = order?.userId
    val orderNo = order?.orderNo ?: orderId.toString()

    // 送水工端:配送开始
    val workerPush = systemConfigQueryApiService.getBooleanValue("delivery_push_assigned_enabled", true)
    if (workerPush) {
      publisher.publish(
        NotificationEventData(
          receiverType = "WORKER",
          workerId = deliveryWorkerId,
          messageType = "ORDER",
          scene = DomainEventType.DELIVERY_STARTED.value,
          title = "开始配送",
          summary = "订单($orderNo)配送开始,请按时送达。",
          relatedNo = orderNo,
          eventTime = eventTime,
        ),
      )
    }

    // 用户端:开始配送
    if (userId != null) {
      publisher.publish(
        NotificationEventData(
          receiverType = "USER",
          userId = userId,
          messageType = "ORDER",
          scene = DomainEventType.DELIVERY_STARTED.value,
          title = "开始配送",
          summary = "您的订单($orderId)已开始配送,请保持电话畅通。",
          relatedNo = orderId.toString(),
          eventTime = eventTime,
          templateScene = NotificationTemplateScenes.DELIVERY_STARTED,
          page = "pages/order-detail/index?id=$orderId",
          templateFields = mapOf("thing1" to "订单号:$orderId", "time3" to formatTime(eventTime)),
        ),
      )
    }
  }
}

/**
 * 配送完成通知:用户端「送达确认」+ 送水工端「配送完成」
 */
@Component
class DeliveryCompletedNotificationHandler(
  publisher: NotificationEventPublisher,
  private val orderQueryApiService: OrderQueryApiService,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) : BaseNotificationEventHandler(publisher) {
  override fun getSupportedEventType(): DomainEventType = DomainEventType.DELIVERY_COMPLETED

  override fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  ) {
    val orderId = requireLong(data, "orderId")
    val deliveryWorkerId = requireLong(data, "deliveryWorkerId")
    val eventTime = eventTimeMillis(data)
    val order = orderQueryApiService.findById(orderId)
    val userId = order?.userId
    val orderNo = order?.orderNo ?: orderId.toString()

    // 送水工端:配送完成
    val workerPush = systemConfigQueryApiService.getBooleanValue("delivery_push_assigned_enabled", true)
    if (workerPush) {
      publisher.publish(
        NotificationEventData(
          receiverType = "WORKER",
          workerId = deliveryWorkerId,
          messageType = "ORDER",
          scene = DomainEventType.DELIVERY_COMPLETED.value,
          title = "配送完成",
          summary = "订单($orderNo)已完成配送。",
          relatedNo = orderNo,
          eventTime = eventTime,
        ),
      )
    }

    // 用户端:送达确认
    if (userId != null) {
      publisher.publish(
        NotificationEventData(
          receiverType = "USER",
          userId = userId,
          messageType = "ORDER",
          scene = DomainEventType.DELIVERY_COMPLETED.value,
          title = "送达确认",
          summary = "您的订单($orderId)已送达,感谢您的信任,欢迎再次下单。",
          relatedNo = orderId.toString(),
          eventTime = eventTime,
          templateScene = NotificationTemplateScenes.DELIVERY_COMPLETED,
          page = "pages/order-detail/index?id=$orderId",
          templateFields = mapOf("thing1" to "订单号:$orderId", "time3" to formatTime(eventTime)),
        ),
      )
    }
  }
}

/**
 * 订单完成通知:用户端「订单完成」
 */
@Component
class OrderCompletedNotificationHandler(
  publisher: NotificationEventPublisher,
) : BaseNotificationEventHandler(publisher) {
  override fun getSupportedEventType(): DomainEventType = DomainEventType.ORDER_COMPLETED

  override fun handleEvent(
    event: DomainEvent,
    data: Map<String, Any>,
  ) {
    val orderId = requireLong(data, "orderId")
    val userId = requireLong(data, "userId")
    val eventTime = eventTimeMillis(data)
    publisher.publish(
      NotificationEventData(
        receiverType = "USER",
        userId = userId,
        messageType = "ORDER",
        scene = DomainEventType.ORDER_COMPLETED.value,
        title = "订单完成",
        summary = "您的订单($orderId)已完成,欢迎评价。",
        relatedNo = orderId.toString(),
        eventTime = eventTime,
        page = "pages/order-detail/index?id=$orderId",
        templateFields = mapOf("thing1" to "订单号:$orderId", "time3" to formatTime(eventTime)),
      ),
    )
  }
}
