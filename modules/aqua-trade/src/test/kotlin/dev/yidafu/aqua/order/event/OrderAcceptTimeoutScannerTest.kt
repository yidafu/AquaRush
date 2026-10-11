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
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 接单超时预警扫描调度器测试(PRD 13-notification.md F-NTF-008 / R-NTF-07,缺口 G94,AC-NTF-008-01/02/03/05):
 * 双阈值(待派单/待配送)扫描 → 发布 ORDER_ACCEPT_TIMEOUT / DELIVERY_START_TIMEOUT 事件,
 * 事件锚点稳定(重复扫描幂等键不变 → 同订单同阶段仅预警一次)、阈值热生效且非法值(≤0)回退默认、
 * 分页游标逐页扫描(规则 13 禁止全表)。
 */
class OrderAcceptTimeoutScannerTest :
  FunSpec({
    val eventTypes = mutableListOf<String>()
    val aggregates = mutableListOf<String>()
    val payloads = mutableListOf<Map<String, Any>>()
    val acceptCutoffs = mutableListOf<LocalDateTime>()
    val startCutoffs = mutableListOf<LocalDateTime>()
    lateinit var orderRepository: OrderRepository
    lateinit var systemConfig: SystemConfigQueryApiService
    val eventPublishService = mockk<SimplifiedEventPublishService>()

    fun stubConfig(
      acceptMinutes: Long,
      startMinutes: Long,
    ) {
      every { systemConfig.getLongValue(SystemSettingKeys.ORDER_ACCEPT_TIMEOUT_MINUTES, 30L) } returns acceptMinutes
      every { systemConfig.getLongValue(SystemSettingKeys.DELIVERY_START_TIMEOUT_MINUTES, 60L) } returns startMinutes
    }

    fun emptyPage(): PageImpl<OrderModel> = PageImpl(emptyList(), PageRequest.of(0, 200), 0)

    fun scanner(): OrderAcceptTimeoutScanner = OrderAcceptTimeoutScanner(orderRepository, eventPublishService, systemConfig)

    /** 断言截止时刻落在 [minutes] 分钟阈值的容差窗口内(扫描执行耗时容差 ±1 分钟) */
    fun cutoffNear(
      cutoff: LocalDateTime,
      minutes: Long,
    ) {
      val reference = LocalDateTime.now().minusMinutes(minutes)
      cutoff.isAfter(reference.minusMinutes(1)) shouldBe true
      cutoff.isBefore(reference.plusMinutes(2)) shouldBe true
    }

    beforeTest {
      eventTypes.clear()
      aggregates.clear()
      payloads.clear()
      acceptCutoffs.clear()
      startCutoffs.clear()
      orderRepository = mockk()
      systemConfig = mockk()
      every { eventPublishService.publishDomainEvent(capture(eventTypes), capture(aggregates), capture(payloads)) } returns true
      stubConfig(30L, 60L)
      every {
        orderRepository.findByStatusAndDispatchAnchorBefore(OrderModelStatus.PENDING_DISPATCH, capture(acceptCutoffs), any())
      } returns emptyPage()
      every {
        orderRepository.findByStatusAndAssignedAtBefore(OrderModelStatus.PENDING_DELIVERY, capture(startCutoffs), any())
      } returns emptyPage()
    }

    test("无命中订单不发布任何事件") {
      scanner().scanAcceptTimeout()

      eventTypes shouldBe emptyList()
    }

    test("双阈值命中:发布两类事件,载荷含订单号/金额/超时时长/稳定锚点(AC-NTF-008-01/02)") {
      val paidAnchor = LocalDateTime.of(2026, 5, 1, 10, 0)
      val assignedAnchor = LocalDateTime.of(2026, 5, 1, 11, 0)
      val acceptOrder =
        OrderModel(
          id = 1001L,
          orderNo = "ORD-A1",
          status = OrderModelStatus.PENDING_DISPATCH,
          amountCents = 2500L,
          createdAt = paidAnchor.minusMinutes(10),
          paymentTime = paidAnchor,
        )
      val startOrder =
        OrderModel(
          id = 2002L,
          orderNo = "ORD-B1",
          status = OrderModelStatus.PENDING_DELIVERY,
          amountCents = 3300L,
          createdAt = assignedAnchor.minusHours(1),
          assignedAt = assignedAnchor,
        )
      every {
        orderRepository.findByStatusAndDispatchAnchorBefore(OrderModelStatus.PENDING_DISPATCH, capture(acceptCutoffs), any())
      } returns PageImpl(listOf(acceptOrder), PageRequest.of(0, 200), 1)
      every {
        orderRepository.findByStatusAndAssignedAtBefore(OrderModelStatus.PENDING_DELIVERY, capture(startCutoffs), any())
      } returns PageImpl(listOf(startOrder), PageRequest.of(0, 200), 1)

      scanner().scanAcceptTimeout()

      eventTypes shouldBe listOf("ORDER_ACCEPT_TIMEOUT", "DELIVERY_START_TIMEOUT")
      aggregates shouldBe listOf("1001", "2002")

      val accept = payloads[0]
      accept["orderNumber"] shouldBe "ORD-A1"
      accept["amountCents"] shouldBe 2500L
      accept["timeoutMinutes"] shouldBe 30L
      // 锚点 = 支付时刻(而非建单时刻):事件时间稳定 → 消息网关幂等键不变
      accept["eventTime"] shouldBe paidAnchor.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

      val start = payloads[1]
      start["orderNumber"] shouldBe "ORD-B1"
      start["timeoutMinutes"] shouldBe 60L
      start["eventTime"] shouldBe assignedAnchor.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

      // 截止时刻按配置阈值起算(热生效)
      cutoffNear(acceptCutoffs.single(), 30L)
      cutoffNear(startCutoffs.single(), 60L)
    }

    test("代客下单无支付时刻:锚点回退建单时刻") {
      val createdAt = LocalDateTime.of(2026, 5, 1, 9, 0)
      val cashOrder =
        OrderModel(
          id = 3003L,
          orderNo = "ORD-C1",
          status = OrderModelStatus.PENDING_DISPATCH,
          createdAt = createdAt,
        )
      every {
        orderRepository.findByStatusAndDispatchAnchorBefore(OrderModelStatus.PENDING_DISPATCH, any(), any())
      } returns PageImpl(listOf(cashOrder), PageRequest.of(0, 200), 1)

      scanner().scanAcceptTimeout()

      payloads[0]["eventTime"] shouldBe createdAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    test("阈值非法(≤0)回退默认 30/60(阈值非法回退)") {
      stubConfig(0L, -5L)

      scanner().scanAcceptTimeout()

      cutoffNear(acceptCutoffs.single(), 30L)
      cutoffNear(startCutoffs.single(), 60L)
    }

    test("阈值修改后即时生效(AC-NTF-008-05):下次扫描按新阈值起算") {
      stubConfig(15L, 45L)

      scanner().scanAcceptTimeout()

      cutoffNear(acceptCutoffs.single(), 15L)
      cutoffNear(startCutoffs.single(), 45L)
    }

    test("重复扫描事件锚点不变:幂等键稳定,同订单同阶段仅预警一次(R-NTF-07,AC-NTF-008-03)") {
      val paidAnchor = LocalDateTime.of(2026, 5, 1, 10, 0)
      val order =
        OrderModel(
          id = 1001L,
          orderNo = "ORD-A1",
          status = OrderModelStatus.PENDING_DISPATCH,
          createdAt = paidAnchor.minusMinutes(10),
          paymentTime = paidAnchor,
        )
      every {
        orderRepository.findByStatusAndDispatchAnchorBefore(OrderModelStatus.PENDING_DISPATCH, any(), any())
      } returns PageImpl(listOf(order), PageRequest.of(0, 200), 1)

      scanner().scanAcceptTimeout()
      scanner().scanAcceptTimeout()

      // 幂等锚点事件字段稳定(排除 timestamp 当次时钟) → relatedNo:scene:eventTime 幂等键不变,
      // 网关按唯一键去重,同订单同阶段仅预警一次
      payloads.size shouldBe 2
      payloads[0]["eventTime"] shouldBe payloads[1]["eventTime"]
      payloads[0]["orderNumber"] shouldBe payloads[1]["orderNumber"]
      payloads[0]["eventTime"] shouldBe paidAnchor.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    test("超过一页的订单逐页扫描不重不漏(规则 13 禁全表)") {
      val anchor = LocalDateTime.of(2026, 5, 1, 10, 0)
      val page0Orders =
        (1L..200L).map {
          OrderModel(
            id = it,
            orderNo = "ORD-$it",
            status = OrderModelStatus.PENDING_DISPATCH,
            createdAt = anchor,
            paymentTime = anchor,
          )
        }
      val page1Order =
        OrderModel(
          id = 201L,
          orderNo = "ORD-201",
          status = OrderModelStatus.PENDING_DISPATCH,
          createdAt = anchor,
          paymentTime = anchor,
        )
      every {
        orderRepository.findByStatusAndDispatchAnchorBefore(OrderModelStatus.PENDING_DISPATCH, any(), any())
      } returnsMany
        listOf(
          PageImpl(page0Orders, PageRequest.of(0, 200), 201),
          PageImpl(listOf(page1Order), PageRequest.of(1, 200), 201),
        )

      scanner().scanAcceptTimeout()

      eventTypes.count { it == "ORDER_ACCEPT_TIMEOUT" } shouldBe 201
      eventTypes.count { it == "DELIVERY_START_TIMEOUT" } shouldBe 0
      verify(exactly = 2) { orderRepository.findByStatusAndDispatchAnchorBefore(OrderModelStatus.PENDING_DISPATCH, any(), any()) }
    }
  })
