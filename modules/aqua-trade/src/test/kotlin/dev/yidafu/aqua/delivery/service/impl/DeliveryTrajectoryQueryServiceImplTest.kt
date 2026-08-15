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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import dev.yidafu.aqua.delivery.domain.repository.DeliveryTrajectoryRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 配送轨迹查询服务单元测试:按订单/时间区间/最新轨迹查询。
 */
class DeliveryTrajectoryQueryServiceImplTest {
  private lateinit var trajectoryRepository: DeliveryTrajectoryRepository
  private lateinit var service: DeliveryTrajectoryQueryServiceImpl

  private fun trajectory(id: Long) =
    DeliveryTrajectoryModel(id = id, deliveryWorkerId = 5L, orderId = 1L, longitude = 120.1, latitude = 30.2)

  @BeforeEach
  fun setUp() {
    trajectoryRepository = mockk(relaxed = true)
    service = DeliveryTrajectoryQueryServiceImpl(trajectoryRepository)
  }

  @Test
  @DisplayName("getTrajectoriesByOrder 透传")
  fun `getTrajectoriesByOrder delegates`() {
    val list = listOf(trajectory(1L), trajectory(2L))
    every { trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(5L, 1L) } returns list

    assertEquals(list, service.getTrajectoriesByOrder(5L, 1L))
  }

  @Test
  @DisplayName("getTrajectories 指定订单时忽略时间范围")
  fun `getTrajectories with order id ignores range`() {
    val list = listOf(trajectory(1L))
    every { trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(5L, 1L) } returns list

    assertEquals(list, service.getTrajectories(5L, 1L, null, null))
  }

  @Test
  @DisplayName("getTrajectories 未指定订单时按时间区间查询")
  fun `getTrajectories without order uses time range`() {
    val list = listOf(trajectory(1L))
    val start = LocalDateTime.of(2025, 1, 1, 0, 0)
    val end = LocalDateTime.of(2025, 1, 2, 0, 0)
    every {
      trajectoryRepository.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(5L, start, end)
    } returns list

    assertEquals(list, service.getTrajectories(5L, null, start, end))
  }

  @Test
  @DisplayName("getTrajectories 未指定订单与时间使用默认区间")
  fun `getTrajectories without order uses default range`() {
    val list = listOf(trajectory(1L))
    every {
      trajectoryRepository.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(5L, any(), any())
    } returns list

    assertEquals(list, service.getTrajectories(5L, null, null, null))
  }

  @Test
  @DisplayName("getLatestTrajectory 返回最新轨迹或 null")
  fun `getLatestTrajectory returns latest or null`() {
    val latest = trajectory(3L)
    every { trajectoryRepository.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(5L) } returns latest
    every { trajectoryRepository.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(6L) } returns null

    assertEquals(latest, service.getLatestTrajectory(5L))
    assertNull(service.getLatestTrajectory(6L))
  }
}
