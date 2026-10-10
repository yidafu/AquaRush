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

package dev.yidafu.aqua.analytics.statistics.service.impl

import dev.yidafu.aqua.analytics.statistics.domain.repository.CompletedOrderTiming
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsDeliveryWorkerRepository
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerStatisticsQueryApiService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * 送水员统计服务单元测试: 总量/在岗/配送中/已完成, 排行榜按今日完成数排序
 */
class DeliveryWorkerStatisticsServiceImplTest :
  FunSpec({
    var adminService: AdminService = mockk()
    var deliveryWorkerStatisticsQueryApiService: DeliveryWorkerStatisticsQueryApiService = mockk()
    var statisticsDeliveryWorkerRepository: StatisticsDeliveryWorkerRepository = mockk()
    var service: DeliveryWorkerStatisticsServiceImpl = mockk()

    beforeTest {
      adminService = mockk(relaxed = true)
      deliveryWorkerStatisticsQueryApiService = mockk(relaxed = true)
      statisticsDeliveryWorkerRepository = mockk(relaxed = true)
      service =
        DeliveryWorkerStatisticsServiceImpl(
          adminService,
          deliveryWorkerStatisticsQueryApiService,
          statisticsDeliveryWorkerRepository,
        )
    }
    test("getDeliveryWorkerStatistics aggregates worker counts") {
      every { adminService.countDeliveryCapable() } returns 2L
      every { statisticsDeliveryWorkerRepository.countTodayActiveWorkers(any()) } returns 2L
      every { statisticsDeliveryWorkerRepository.countDeliveringOrders() } returns 5L
      every { statisticsDeliveryWorkerRepository.countCompletedOrdersSince(any()) } returns 8L
      every { statisticsDeliveryWorkerRepository.findCompletedOrderTimingsSince(any()) } returns emptyList()

      val result = service.getDeliveryWorkerStatistics()

      result.totalWorkers shouldBe 2L
      result.todayActiveWorkers shouldBe 2L
      result.deliveringOrders shouldBe 5L
      result.todayCompletedOrders shouldBe 8L
      // G36: 人均单量 = 今日完成 / 今日在岗;无今日完成订单 → 平均履约时长 null
      result.averageOrdersPerWorker shouldBe 4.0f
      result.averageFulfillmentMinutes shouldBe null
    }
    test("getDeliveryWorkerStatistics computes average fulfillment minutes") {
      // G36: 平均履约时长 = Σ(completedAt − 派单时间)/N,派单时间取最早 DELIVERY_ASSIGNED 操作时间
      val today = java.time.LocalDate.now()
      val todayStart = today.atStartOfDay()
      val completedAt1 = todayStart.plusHours(10)
      val completedAt2 = todayStart.plusHours(11)
      every { adminService.countDeliveryCapable() } returns 1L
      every { statisticsDeliveryWorkerRepository.countTodayActiveWorkers(any()) } returns 1L
      every { statisticsDeliveryWorkerRepository.countDeliveringOrders() } returns 0L
      every { statisticsDeliveryWorkerRepository.countCompletedOrdersSince(any()) } returns 2L
      every { statisticsDeliveryWorkerRepository.findCompletedOrderTimingsSince(any()) } returns
        listOf(
          // 派单时间 09:00 → 10:00 完成 = 60 分钟
          CompletedOrderTiming(
            orderId = 1L,
            completedAt = completedAt1,
            deliveryStartedAt = null,
            createdAt = todayStart.plusHours(8),
          ),
          // 无派单操作时间 → 回退 deliveryStartedAt 09:30 → 11:00 完成 = 90 分钟
          CompletedOrderTiming(
            orderId = 2L,
            completedAt = completedAt2,
            deliveryStartedAt = todayStart.plusHours(9).plusMinutes(30),
            createdAt = todayStart.plusHours(8),
          ),
        )
      every { statisticsDeliveryWorkerRepository.findEarliestAssignedAtByOrderIds(any()) } returns
        mapOf(1L to todayStart.plusHours(9))

      val result = service.getDeliveryWorkerStatistics()

      result.todayCompletedOrders shouldBe 2L
      result.averageOrdersPerWorker shouldBe 2.0f
      // (60 + 90) / 2 = 75 分钟
      result.averageFulfillmentMinutes!!.toDouble() shouldBe 75.0
    }
    test("getDeliveryWorkerStatistics handles zero active workers") {
      // G36 边界:今日在岗为 0 → 人均单量 null;无完成订单 → 平均履约时长 null
      every { adminService.countDeliveryCapable() } returns 0L
      every { statisticsDeliveryWorkerRepository.countTodayActiveWorkers(any()) } returns 0L
      every { statisticsDeliveryWorkerRepository.countDeliveringOrders() } returns 0L
      every { statisticsDeliveryWorkerRepository.countCompletedOrdersSince(any()) } returns 0L
      every { statisticsDeliveryWorkerRepository.findCompletedOrderTimingsSince(any()) } returns emptyList()

      val result = service.getDeliveryWorkerStatistics()

      result.averageOrdersPerWorker shouldBe null
      result.averageFulfillmentMinutes shouldBe null
    }
  })
