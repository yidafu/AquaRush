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
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.SubscriptionItemModel
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.subscription.domain.repository.SubscriptionRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 周期订购到期调度测试(G77/F-ORD-026,03-core-flows §12):
 * 幂等键 sub-{id}-{期次}、成功推进/达上限终止、失败记 last_error 且不推进(次日重试)、
 * 单条失败不阻断批次、提醒事件载荷(workerId 可选)。
 */
class SubscriptionSchedulerTest :
  FunSpec({
    var repository: SubscriptionRepository = mockk()
    var orderMutationService: OrderMutationService = mockk()
    var eventPublishService: SimplifiedEventPublishService = mockk()
    var scheduler: SubscriptionScheduler = mockk()
    val today: LocalDate = LocalDate.now(ZoneId.of("Asia/Shanghai"))

    fun plan(
      id: Long = 1L,
      nextRunDate: LocalDate = today,
      executedPeriods: Int = 0,
      maxPeriods: Int? = null,
      workerId: Long? = null,
      status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    ) = SubscriptionModel(
      id = id,
      userId = 100L,
      addressId = 10L,
      workerId = workerId,
      cycleType = SubscriptionCycleType.WEEKLY,
      weekdays = "1,2,3,4,5,6,7", // 每天 → 下一期恒为 due+1,断言确定
      monthDay = null,
      timeSlot = DeliveryTimeSlot.MORNING,
      startDate = today.minusMonths(1),
      maxPeriods = maxPeriods,
      executedPeriods = executedPeriods,
      nextRunDate = nextRunDate,
      status = status,
      createdBy = 7L,
    )

    fun order(id: Long = 900L) =
      OrderModel(
        id = id,
        orderNo = "ORD-SUB-$id",
        userId = 100L,
        status = OrderModelStatus.PENDING_DISPATCH,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      repository = mockk(relaxed = true)
      orderMutationService = mockk(relaxed = true)
      eventPublishService = mockk(relaxed = true)
      scheduler = SubscriptionScheduler(repository, orderMutationService, eventPublishService)
      every { repository.save(any<SubscriptionModel>()) } answers { firstArg() }
      every { repository.findItemsBySubscriptionId(any()) } returns
        listOf(SubscriptionItemModel(id = 1L, subscriptionId = 1L, productId = 5L, quantity = 2))
    }

    test("no due plans: no order creation and no reminder") {
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns emptyList()

      scheduler.scanDueSubscriptions()

      verify(exactly = 0) { orderMutationService.createDeliveryOrder(any(), any()) }
      verify(exactly = 0) { eventPublishService.publishDomainEvent(any(), any(), any()) }
    }

    test("due plan generates order with sub idempotency key and advances period") {
      val p = plan()
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns
        listOf(p) andThen emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } returns order()
      val requestSlot = slot<CreateOrderRequest>()
      val eventSlot = slot<Map<String, Any>>()

      scheduler.scanDueSubscriptions()

      verify(exactly = 1) { orderMutationService.createDeliveryOrder(eq(7L), capture(requestSlot)) }
      requestSlot.captured.clientRequestId shouldBe "sub-1-1"
      requestSlot.captured.deliveryTimeSlot shouldBe DeliveryTimeSlot.MORNING
      requestSlot.captured.userId shouldBe 100L
      requestSlot.captured.items
        .single()
        .productId shouldBe 5L

      p.executedPeriods shouldBe 1
      p.lastError.shouldBeNull()
      p.status shouldBe SubscriptionStatus.ACTIVE
      p.nextRunDate shouldBe today.plusDays(1)

      verify(exactly = 1) {
        eventPublishService.publishDomainEvent(eq("SUBSCRIPTION_ORDER_CREATED"), eq("ORD-SUB-900"), capture(eventSlot))
      }
      eventSlot.captured["orderId"] shouldBe 900L
      eventSlot.captured["subscriptionId"] shouldBe 1L
      eventSlot.captured["period"] shouldBe 1
      eventSlot.captured["deliveryTimeSlot"] shouldBe "MORNING"
      eventSlot.captured.containsKey("workerId") shouldBe false
    }

    test("bound worker carried into reminder event payload") {
      val p = plan(workerId = 42L)
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns
        listOf(p) andThen emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } returns order()
      val eventSlot = slot<Map<String, Any>>()

      scheduler.scanDueSubscriptions()

      verify { eventPublishService.publishDomainEvent(any(), any(), capture(eventSlot)) }
      eventSlot.captured["workerId"] shouldBe 42L
    }

    test("failure records lastError, keeps nextRunDate and skips reminder (不静默,次日重试)") {
      val p = plan()
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns
        listOf(p) andThen emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } throws BadRequestException("商品库存不足")

      scheduler.scanDueSubscriptions()

      p.executedPeriods shouldBe 0
      p.nextRunDate shouldBe today
      p.lastError shouldBe "商品库存不足"
      (p.lastRunAt != null) shouldBe true
      verify(exactly = 0) { eventPublishService.publishDomainEvent(any(), any(), any()) }
      // 失败状态已回写(列表可见)
      verify(exactly = 1) { repository.save(p) }
    }

    test("reaching maxPeriods terminates the plan") {
      val p = plan(maxPeriods = 1)
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns
        listOf(p) andThen emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } returns order()

      scheduler.scanDueSubscriptions()

      p.status shouldBe SubscriptionStatus.TERMINATED
      p.executedPeriods shouldBe 1
    }

    test("crossing endDate terminates the plan") {
      // 每天周期 + 今天是终止日:执行后推进到明天已越过 endDate → 终止
      val p = plan().apply { endDate = today }
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns
        listOf(p) andThen emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } returns order()

      scheduler.scanDueSubscriptions()

      p.status shouldBe SubscriptionStatus.TERMINATED
    }

    test("single plan failure does not block subsequent plans in batch") {
      val bad = plan(id = 1L)
      val good = plan(id = 2L)
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns
        listOf(bad, good) andThen emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } throws
        BadRequestException("地址不可达") andThen order(id = 901L)

      scheduler.scanDueSubscriptions()

      bad.lastError shouldBe "地址不可达"
      bad.executedPeriods shouldBe 0
      good.executedPeriods shouldBe 1
      good.lastError.shouldBeNull()
      verify(exactly = 1) { eventPublishService.publishDomainEvent(any(), any(), any()) }
    }

    test("scan advances keyset cursor across batches") {
      val first = plan(id = 11L)
      val second = plan(id = 22L)
      every { repository.findDue(any(), eq(today), eq(SubscriptionScheduler.SCAN_BATCH_SIZE)) } returns
        listOf(first) andThen
        listOf(second) andThen
        emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } returns order()

      scheduler.scanDueSubscriptions()

      first.executedPeriods shouldBe 1
      second.executedPeriods shouldBe 1
      verify(exactly = 1) { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) }
      verify(exactly = 1) { repository.findDue(11L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) }
      verify(exactly = 1) { repository.findDue(22L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) }
    }

    test("skip missed periods when retry spans multiple days") {
      // 上周同期次失败,今天重试成功:按到期日推进会得到已过去的下一期 → 从今天重算(不补送)
      val due = today.minusDays(10)
      val p = plan(nextRunDate = due).apply { weekdays = "1,2,3,4,5,6,7" }
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns
        listOf(p) andThen emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } returns order()

      scheduler.scanDueSubscriptions()

      (p.nextRunDate.isBefore(today)) shouldBe false
      p.executedPeriods shouldBe 1
    }

    test("request remark carries period tag") {
      val p = plan().apply { remark = "公司前台" }
      every { repository.findDue(0L, today, SubscriptionScheduler.SCAN_BATCH_SIZE) } returns
        listOf(p) andThen emptyList()
      every { orderMutationService.createDeliveryOrder(any(), any()) } returns order()
      val requestSlot = slot<CreateOrderRequest>()

      scheduler.scanDueSubscriptions()

      verify { orderMutationService.createDeliveryOrder(any(), capture(requestSlot)) }
      requestSlot.captured.remark shouldBe "[周期订购 第1期] 公司前台"
    }
  })
