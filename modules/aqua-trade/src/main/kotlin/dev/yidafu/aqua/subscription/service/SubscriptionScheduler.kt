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

package dev.yidafu.aqua.subscription.service

import dev.yidafu.aqua.api.dto.CreateOrderRequest
import dev.yidafu.aqua.api.dto.OrderItemRequest
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionStatus
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.subscription.domain.repository.SubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 周期订购到期调度(G77/F-ORD-026,03-core-flows §12):
 * 每日(默认 05:30,Asia/Shanghai,可配 `aqua.subscription.scan-cron`)keyset 分页扫描
 * `ACTIVE AND next_run_date <= today` 的计划,按 `client_request_id = sub-{id}-{期次}`
 * 幂等生成代客下单订单(现场收款 CASH,直接进入待派单),随后发布 SUBSCRIPTION_ORDER_CREATED
 * 事件提醒送水工按时配送(绑定送水工→定向站内消息+推送,未绑定→广播)。
 *
 * 不静默失败(R-ORD-13):生成订单失败时记录 last_error 并保持 next_run_date 不变(次日重试),
 * 后台列表可见失败原因;单条失败不阻断其余计划。仅 admin 服务运行([AdminService])。
 */
@Component
@AdminService
class SubscriptionScheduler(
  private val subscriptionRepository: SubscriptionRepository,
  private val orderMutationService: OrderMutationService,
  private val eventPublishService: SimplifiedEventPublishService,
) {
  private val logger = LoggerFactory.getLogger(SubscriptionScheduler::class.java)

  @Scheduled(
    cron = "\${aqua.subscription.scan-cron:0 30 5 * * *}",
    zone = "Asia/Shanghai",
  )
  fun scanDueSubscriptions() {
    val today = LocalDate.now(ZoneId.of("Asia/Shanghai"))
    var afterId = 0L
    while (true) {
      val batch =
        subscriptionRepository.findDue(
          afterId = afterId,
          dueOnOrBefore = today,
          limit = SCAN_BATCH_SIZE,
        )
      if (batch.isEmpty()) break
      batch.forEach { plan ->
        afterId = plan.id ?: afterId
        executePlan(plan, today)
      }
    }
  }

  /** 执行单个计划:任何异常不外抛(单条失败不阻断批次) */
  private fun executePlan(
    plan: SubscriptionModel,
    today: LocalDate,
  ) {
    val period = plan.executedPeriods + 1
    val dueDate = plan.nextRunDate
    val order =
      try {
        createOrder(plan, period)
      } catch (e: Exception) {
        recordFailure(plan, period, e)
        return
      }

    // 推进计划(失败则下期由幂等键兑底重试;不与提醒发布互相阻断)
    try {
      advancePlan(plan, period, dueDate, today)
    } catch (e: Exception) {
      logger.error("周期订购计划推进失败,将于下次扫描幂等重试: planId={}, period={}", plan.id, period, e)
    }

    try {
      publishWorkerReminder(plan, order, period, dueDate)
    } catch (e: Exception) {
      logger.error("周期订购提醒事件发布失败: planId={}, orderId={}", plan.id, order.id, e)
    }
  }

  /**
   * 按计划生成代客下单订单:幂等键 `sub-{id}-{期次}-(user_id, client_request_id) 唯一索引兜底;
   * 商品在售/库存、地址可达、配送范围均走 createDeliveryOrder 既有校验(失败抛出 → 记录重试)。
   */
  private fun createOrder(
    plan: SubscriptionModel,
    period: Int,
  ): OrderModel {
    val items =
      subscriptionRepository.findItemsBySubscriptionId(plan.id!!).map { item ->
        OrderItemRequest(productId = item.productId, quantity = item.quantity)
      }
    require(items.isNotEmpty()) { "周期订购计划无商品明细" }
    return orderMutationService.createDeliveryOrder(
      plan.createdBy ?: 0L,
      CreateOrderRequest(
        userId = plan.userId,
        items = items,
        addressId = plan.addressId,
        remark = "[周期订购 第${period}期] ${plan.remark.orEmpty()}".trim(),
        clientRequestId = "sub-${plan.id}-$period",
        deliveryTimeSlot = plan.timeSlot,
      ),
    )
  }

  /** 成功推进:期次数 +1、清失败原因、计算下一次执行日;达期次上限/越过终止日 → 终止 */
  private fun advancePlan(
    plan: SubscriptionModel,
    period: Int,
    dueDate: LocalDate,
    today: LocalDate,
  ) {
    plan.executedPeriods = period
    plan.lastError = null
    plan.lastRunAt = LocalDateTime.now()
    var next =
      SubscriptionCycleCalculator.nextRun(
        after = dueDate,
        cycleType = plan.cycleType,
        weekdays = SubscriptionCycleCalculator.parseWeekdays(plan.weekdays),
        monthDay = plan.monthDay,
        startDate = plan.startDate,
      )
    // 失败重试跨多日后按到期日推进会得到已过去的执行日:跳过错过的期次(不补送),
    // 从今天重算,避免恢复后立刻连发两期
    if (next.isBefore(today)) {
      next =
        SubscriptionCycleCalculator.firstRun(
          startDate = today,
          cycleType = plan.cycleType,
          weekdays = SubscriptionCycleCalculator.parseWeekdays(plan.weekdays),
          monthDay = plan.monthDay,
        )
    }
    val reachedMax = plan.maxPeriods?.let { period >= it } ?: false
    val beyondEnd = plan.endDate?.let { next.isAfter(it) } ?: false
    if (reachedMax || beyondEnd) {
      plan.status = SubscriptionStatus.TERMINATED
      logger.info("周期订购计划已执行完毕终止: planId={}, period={}", plan.id, period)
    } else {
      plan.nextRunDate = next
    }
    plan.updatedAt = LocalDateTime.now()
    subscriptionRepository.save(plan)
  }

  /** 失败不静默:记录原因(列表可见),保持 next_run_date 次日重试 */
  private fun recordFailure(
    plan: SubscriptionModel,
    period: Int,
    e: Exception,
  ) {
    plan.lastError = (e.message ?: "生成订单失败").take(500)
    plan.lastRunAt = LocalDateTime.now()
    plan.updatedAt = LocalDateTime.now()
    runCatching { subscriptionRepository.save(plan) }
      .onFailure { saveError -> logger.error("周期订购计划失败状态保存失败: planId={}", plan.id, saveError) }
    logger.error("周期订购计划到期执行失败,次日重试: planId={}, period={}", plan.id, period, e)
  }

  /**
   * 发布提醒事件(平台层 handler 转 NOTIFICATION_SEND → 送水工站内消息+推送):
   * eventTime 锚定到期日(而非墙钟),同一期次重复发布由通知幂等键去重(R-NTF-02)。
   */
  private fun publishWorkerReminder(
    plan: SubscriptionModel,
    order: OrderModel,
    period: Int,
    dueDate: LocalDate,
  ) {
    val eventData =
      mutableMapOf<String, Any>(
        "orderId" to (order.id ?: 0L),
        "orderNo" to order.orderNo,
        "userId" to order.userId,
        "amountCents" to order.amountCents,
        "subscriptionId" to (plan.id ?: 0L),
        "period" to period,
        "deliveryTimeSlot" to plan.timeSlot.name,
        "timestamp" to dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
      )
    plan.workerId?.let { eventData["workerId"] = it }
    eventPublishService.publishDomainEvent(
      eventType = DomainEventType.SUBSCRIPTION_ORDER_CREATED.value,
      aggregateId = order.orderNo,
      eventData = eventData,
    )
  }

  companion object {
    /** 单批扫描计划数(keyset 分页,禁止全表) */
    const val SCAN_BATCH_SIZE = 100
  }
}
