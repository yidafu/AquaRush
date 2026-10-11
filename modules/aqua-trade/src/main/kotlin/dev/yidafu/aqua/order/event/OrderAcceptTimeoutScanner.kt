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
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 接单超时预警扫描调度器(PRD 13-notification.md F-NTF-008 / R-NTF-07,缺口 G94),
 * 与配送超时扫描(DeliveryTimeoutScanScheduler,G72)对称覆盖「无人接单」侧:
 * 1. 「待派单」超 order_accept_timeout_minutes(默认 30 分钟)无人接单 → ORDER_ACCEPT_TIMEOUT;
 * 2. 「待配送」超 delivery_start_timeout_minutes(默认 60 分钟)未开始配送 → DELIVERY_START_TIMEOUT。
 *
 * 事件经 outbox 投递 → 通知域 OrderAcceptTimeoutNotificationHandler 向全体管理员落库预警站内消息
 * (后台消息列表 F-NTF-006 可查)。阈值由系统参数配置(F-SYS-002,热生效),非法值(≤0)回退默认;
 * 事件载荷 eventTime 取阶段稳定锚点(待派单 = 支付/建单时间,待配送 = 派单时间),消息网关幂等键
 * (订单号+事件类型+锚点)去重 → 同一订单同一阶段仅预警一次(R-NTF-07);
 * 订单状态在扫描与消费之间变化(被接单/取消)由通知处理器状态谓词拦截(AC-NTF-008-04);
 * 分页游标扫描(规则 13 禁止全表)。
 */
@Component
class OrderAcceptTimeoutScanner(
  private val orderRepository: OrderRepository,
  private val eventPublishService: SimplifiedEventPublishService,
  private val systemConfigQueryApiService: SystemConfigQueryApiService,
) {
  private val logger = LoggerFactory.getLogger(OrderAcceptTimeoutScanner::class.java)

  @Scheduled(fixedDelay = 300_000, initialDelay = 90_000)
  fun scanAcceptTimeout() {
    val acceptMinutes = positiveThreshold(SystemSettingKeys.ORDER_ACCEPT_TIMEOUT_MINUTES, DEFAULT_ACCEPT_TIMEOUT_MINUTES)
    val startMinutes = positiveThreshold(SystemSettingKeys.DELIVERY_START_TIMEOUT_MINUTES, DEFAULT_DELIVERY_START_TIMEOUT_MINUTES)
    val now = LocalDateTime.now()

    val acceptPublished =
      scanStage(
        eventType = DomainEventType.ORDER_ACCEPT_TIMEOUT,
        status = OrderModelStatus.PENDING_DISPATCH,
        cutoff = now.minusMinutes(acceptMinutes),
        timeoutMinutes = acceptMinutes,
        anchorOf = { order -> order.paymentTime ?: order.createdAt },
        query = orderRepository::findByStatusAndDispatchAnchorBefore,
      )
    val startPublished =
      scanStage(
        eventType = DomainEventType.DELIVERY_START_TIMEOUT,
        status = OrderModelStatus.PENDING_DELIVERY,
        cutoff = now.minusMinutes(startMinutes),
        timeoutMinutes = startMinutes,
        anchorOf = { order -> order.assignedAt ?: order.createdAt },
        query = orderRepository::findByStatusAndAssignedAtBefore,
      )

    val published = acceptPublished + startPublished
    if (published > 0) {
      logger.info(
        "接单超时扫描完成: 共发布 {} 条预警事件(待派单阈值 {} 分钟 {} 条,待配送阈值 {} 分钟 {} 条)",
        published,
        acceptMinutes,
        acceptPublished,
        startMinutes,
        startPublished,
      )
    }
  }

  /**
   * 扫描单个履约阶段:按截止时刻分页拉取订单并发布超时预警事件,返回本次发布条数。
   */
  private fun scanStage(
    eventType: DomainEventType,
    status: OrderModelStatus,
    cutoff: LocalDateTime,
    timeoutMinutes: Long,
    anchorOf: (OrderModel) -> LocalDateTime,
    query: (OrderModelStatus, LocalDateTime, Pageable) -> Page<OrderModel>,
  ): Int {
    var page = 0
    var published = 0
    while (true) {
      val result = query(status, cutoff, PageRequest.of(page, PAGE_SIZE))
      if (result.content.isEmpty()) {
        break
      }
      result.content.forEach { order ->
        val orderId = order.id ?: return@forEach
        val anchor = anchorOf(order)
        eventPublishService.publishDomainEvent(
          eventType = eventType.value,
          aggregateId = orderId.toString(),
          eventData =
            mapOf(
              "orderId" to orderId,
              "orderNumber" to order.orderNo,
              "amountCents" to order.amountCents,
              "timeoutMinutes" to timeoutMinutes,
              // 稳定事件时间(阶段锚点:重复扫描/双服务并发发布幂等键不变,同订单同阶段仅预警一次)
              "eventTime" to anchor.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
              "timestamp" to System.currentTimeMillis(),
            ),
        )
        published++
      }
      if (!result.hasNext()) {
        break
      }
      page++
    }
    return published
  }

  /** 阈值读取(热生效,每次扫描重读):非法值(≤0)回退默认(F-NTF-008 阈值非法回退) */
  private fun positiveThreshold(
    key: String,
    default: Long,
  ): Long {
    val value = systemConfigQueryApiService.getLongValue(key, default)
    return if (value > 0) value else default
  }

  companion object {
    private const val DEFAULT_ACCEPT_TIMEOUT_MINUTES = 30L
    private const val DEFAULT_DELIVERY_START_TIMEOUT_MINUTES = 60L
    private const val PAGE_SIZE = 200
  }
}
