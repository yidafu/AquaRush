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

import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsDeliveryWorkerRepositoryCustom
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerStatisticsQueryApiService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * 送水员统计服务单元测试: 总量/在岗/配送中/已完成, 排行榜按今日完成数排序
 */
class DeliveryWorkerStatisticsServiceImplTest {
  private lateinit var adminService: AdminService
  private lateinit var deliveryWorkerStatisticsQueryApiService: DeliveryWorkerStatisticsQueryApiService
  private lateinit var statisticsDeliveryWorkerRepository: StatisticsDeliveryWorkerRepositoryCustom
  private lateinit var service: DeliveryWorkerStatisticsServiceImpl

  @BeforeEach
  fun setUp() {
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

  private fun worker(
    id: Long,
    name: String?,
    earnings: Long?,
  ): AdminModel =
    AdminModel(
      id = id,
      username = "worker$id",
      passwordHash = "hash",
      role = AdminRoleModel.DELIVERY_WORKER,
    ).apply {
      realName = name
      earningCents = earnings
    }

  @Test
  fun `getDeliveryWorkerStatistics aggregates worker counts`() {
    every { adminService.findAllDeliveryCapable() } returns listOf(worker(1L, "张三", null), worker(2L, "李四", null))
    every { statisticsDeliveryWorkerRepository.countTodayActiveWorkers(any()) } returns 2L
    every { statisticsDeliveryWorkerRepository.countDeliveringOrders() } returns 5L
    every { statisticsDeliveryWorkerRepository.countCompletedOrdersSince(any()) } returns 8L

    val result = service.getDeliveryWorkerStatistics()

    assertEquals(2L, result.totalWorkers)
    assertEquals(2L, result.todayActiveWorkers)
    assertEquals(5L, result.deliveringOrders)
    assertEquals(8L, result.todayCompletedOrders)
  }
}
