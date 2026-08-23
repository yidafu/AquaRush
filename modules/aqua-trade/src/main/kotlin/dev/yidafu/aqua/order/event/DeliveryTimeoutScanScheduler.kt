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

import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 配送超时扫描调度器(R-ORD-10 推送节点「配送超时」):
 * 周期扫描「配送中且开始配送超过阈值」的订单,发布 DELIVERY_TIMEOUT 领域事件,
 * 通知域 DeliveryTimeoutNotificationHandler 据此向配送该订单的送水工推送超时提醒
 * (开关 delivery_push_timeout_enabled)。
 *
 * 阈值由系统参数 delivery_timeout_minutes 配置(默认 120 分钟,仅超管可改);
 * 事件载荷 eventTime 取订单 deliveryStartedAt(稳定),重复扫描/双服务并发发布
 * 由消息网关幂等键(订单号+场景+事件时间)去重,不重复提醒;分页游标扫描(规则 13)。
 */
@Component
class DeliveryTimeoutScanScheduler(
  private val orderRepository: OrderRepository,
  private val eventPublishService: SimplifiedEventPublishService,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) {
  private val logger = LoggerFactory.getLogger(DeliveryTimeoutScanScheduler::class.java)

  @Scheduled(fixedDelay = 300_000, initialDelay = 60_000)
  fun scanDeliveryTimeout() {
    val timeoutMinutes = systemConfigQueryApiService.getLongValue(SystemSettingKeys.DELIVERY_TIMEOUT_MINUTES, DEFAULT_TIMEOUT_MINUTES)
    val cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes)
    var page = 0
    var published = 0

    while (true) {
      val result =
        orderRepository.findByStatusAndDeliveryStartedAtBefore(
          status = OrderModelStatus.DELIVERING,
          before = cutoff,
          pageable = PageRequest.of(page, PAGE_SIZE),
        )
      if (result.content.isEmpty()) {
        break
      }
      result.content.forEach { order ->
        val workerId = order.deliveryWorkerId
        val orderId = order.id ?: return@forEach
        if (workerId != null) {
          eventPublishService.publishDomainEvent(
            eventType = DomainEventType.DELIVERY_TIMEOUT.value,
            aggregateId = orderId.toString(),
            eventData =
              mapOf(
                "orderId" to orderId,
                "orderNumber" to order.orderNo,
                "deliveryWorkerId" to workerId,
                // 稳定事件时间(取开始配送时间,重复扫描幂等键不变,不重复提醒)
                "eventTime" to (order.deliveryStartedAt ?: order.createdAt).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                "timestamp" to System.currentTimeMillis(),
              ),
          )
          published++
        }
      }
      if (!result.hasNext()) {
        break
      }
      page++
    }

    if (published > 0) {
      logger.info("配送超时扫描完成: 共发布 {} 条超时事件(阈值 {} 分钟)", published, timeoutMinutes)
    }
  }

  companion object {
    private const val DEFAULT_TIMEOUT_MINUTES = 120L
    private const val PAGE_SIZE = 200
  }
}
