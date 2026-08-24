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

package dev.yidafu.aqua.order.service

import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.ops.SystemConfigQueryApiService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.order.config.ReceiptAutoConfirmProperties
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 自动确认收货兜底扫描测试(R-ORD-11):
 * 分页游标分批扫描超期「待确认收货」订单(送达时间早于 截止时刻=now-N 天) → 逐条 autoConfirmReceipt;
 * 配置天数 ≤0 跳过、空页终止、单条失败不影响后续、多页推进;
 * 互斥与幂等由服务内 CAS 认领(confirmReceiptAtomically)保证(见 OrderMutationServiceImplTest)。
 */
class ReceiptAutoConfirmSchedulerTest :
  FunSpec({
    var orderRepository: OrderRepository = mockk()
    var orderMutationService: OrderMutationService = mockk()
    var systemConfigQueryApiService: SystemConfigQueryApiService = mockk()
    var properties: ReceiptAutoConfirmProperties = ReceiptAutoConfirmProperties()
    var scheduler: ReceiptAutoConfirmScheduler = mockk()

    fun order(
      id: Long,
      deliveredAt: LocalDateTime,
    ) = OrderModel(
      id = id,
      orderNo = "ORDER-$id",
      userId = 100L,
      status = OrderModelStatus.AWAITING_CONFIRMATION,
      deliveryConfirmedAt = deliveredAt,
    )

    beforeTest {
      orderRepository = mockk(relaxed = true)
      orderMutationService = mockk(relaxed = true)
      systemConfigQueryApiService = mockk(relaxed = true)
      properties = ReceiptAutoConfirmProperties(scanBatchSize = 2)
      scheduler =
        ReceiptAutoConfirmScheduler(orderRepository, orderMutationService, systemConfigQueryApiService, properties)
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.AUTO_CONFIRM_RECEIPT_DAYS, 7L) } returns 7L
    }
    test("scansExpiredAwaitingOrdersAndDelegatesAutoConfirm") {
      val now = LocalDateTime.now()
      every {
        orderRepository.findByStatusAndDeliveryConfirmedAtBefore(OrderModelStatus.AWAITING_CONFIRMATION, any(), any())
      } returns
        PageImpl(
          listOf(
            order(1L, now.minusDays(8)),
            order(2L, now.minusDays(9)),
          ),
        )

      scheduler.autoConfirmExpiredReceipts()

      verify(exactly = 1) { orderMutationService.autoConfirmReceipt(1L) }
      verify(exactly = 1) { orderMutationService.autoConfirmReceipt(2L) }
    }
    test("paginatesThroughAllExpiredOrders") {
      val now = LocalDateTime.now()
      // total=5, size=2 → 3 页(0/1/2);PageImpl.hasNext 由 pageable+total 计算
      val page0 = PageImpl(listOf(order(1L, now.minusDays(8))), PageRequest.of(0, 2), 5)
      val page1 = PageImpl(listOf(order(2L, now.minusDays(9))), PageRequest.of(1, 2), 5)
      val page2 = PageImpl(listOf(order(3L, now.minusDays(10))), PageRequest.of(2, 2), 5)
      every {
        orderRepository.findByStatusAndDeliveryConfirmedAtBefore(OrderModelStatus.AWAITING_CONFIRMATION, any(), any())
      } returnsMany
        listOf(page0, page1, page2)

      scheduler.autoConfirmExpiredReceipts()

      verify(exactly = 1) { orderMutationService.autoConfirmReceipt(1L) }
      verify(exactly = 1) { orderMutationService.autoConfirmReceipt(2L) }
      verify(exactly = 1) { orderMutationService.autoConfirmReceipt(3L) }
    }
    test("skipsWhenNoExpiredOrders") {
      every {
        orderRepository.findByStatusAndDeliveryConfirmedAtBefore(any(), any(), any())
      } returns
        PageImpl(emptyList<OrderModel>())

      scheduler.autoConfirmExpiredReceipts()

      verify(exactly = 0) { orderMutationService.autoConfirmReceipt(any()) }
    }
    test("continuesScanningWhenSingleAutoConfirmFails") {
      val now = LocalDateTime.now()
      every {
        orderRepository.findByStatusAndDeliveryConfirmedAtBefore(OrderModelStatus.AWAITING_CONFIRMATION, any(), any())
      } returns
        PageImpl(
          listOf(
            order(1L, now.minusDays(8)),
            order(2L, now.minusDays(9)),
          ),
        )
      every { orderMutationService.autoConfirmReceipt(1L) } throws RuntimeException("boom")

      scheduler.autoConfirmExpiredReceipts()

      verify(exactly = 1) { orderMutationService.autoConfirmReceipt(1L) }
      verify(exactly = 1) { orderMutationService.autoConfirmReceipt(2L) }
    }
    test("skipsScanWhenConfiguredDaysNonPositive(防御)") {
      every { systemConfigQueryApiService.getLongValue(SystemSettingKeys.AUTO_CONFIRM_RECEIPT_DAYS, 7L) } returns 0L

      scheduler.autoConfirmExpiredReceipts()

      verify(exactly = 0) {
        orderRepository.findByStatusAndDeliveryConfirmedAtBefore(any(), any(), any())
      }
      verify(exactly = 0) { orderMutationService.autoConfirmReceipt(any()) }
    }
    test("pageQueryFailurePropagatesForRetryNextCycle") {
      every {
        orderRepository.findByStatusAndDeliveryConfirmedAtBefore(any(), any(), any())
      } throws RuntimeException("db down")

      shouldThrow<RuntimeException> { scheduler.autoConfirmExpiredReceipts() }
    }
  })
