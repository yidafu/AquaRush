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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

/**
 * 配送轨迹查询服务单元测试:按订单/时间区间/最新轨迹查询。
 */
class DeliveryTrajectoryQueryServiceImplTest :
  FunSpec({
    var trajectoryRepository: DeliveryTrajectoryRepository = mockk()
    var service: DeliveryTrajectoryQueryServiceImpl = mockk()

    fun trajectory(id: Long) = DeliveryTrajectoryModel(id = id, deliveryWorkerId = 5L, orderId = 1L, longitude = 120.1, latitude = 30.2)

    beforeTest {
      trajectoryRepository = mockk(relaxed = true)
      service = DeliveryTrajectoryQueryServiceImpl(trajectoryRepository)
    }
    test("getTrajectoriesByOrder delegates") {
      val list = listOf(trajectory(1L), trajectory(2L))
      every { trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(5L, 1L) } returns list

      service.getTrajectoriesByOrder(5L, 1L) shouldBe list
    }
    test("getTrajectories with order id ignores range") {
      val list = listOf(trajectory(1L))
      every { trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(5L, 1L) } returns list

      service.getTrajectories(5L, 1L, null, null) shouldBe list
    }
    test("getTrajectories without order uses time range") {
      val list = listOf(trajectory(1L))
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      val end = LocalDateTime.of(2025, 1, 2, 0, 0)
      every {
        trajectoryRepository.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(5L, start, end)
      } returns list

      service.getTrajectories(5L, null, start, end) shouldBe list
    }
    test("getTrajectories without order and without range returns all trajectories") {
      val list = listOf(trajectory(1L))
      every { trajectoryRepository.findByDeliveryWorkerIdOrderByReportedAtAsc(5L) } returns list

      service.getTrajectories(5L, null, null, null) shouldBe list
    }
    test("getTrajectories with only start uses safe upper bound") {
      val list = listOf(trajectory(1L))
      val start = LocalDateTime.of(2025, 1, 1, 0, 0)
      every {
        trajectoryRepository.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(
          5L,
          start,
          LocalDateTime.of(9999, 12, 31, 23, 59, 59),
        )
      } returns list

      service.getTrajectories(5L, null, start, null) shouldBe list
    }
    test("getTrajectories with only end uses safe lower bound") {
      val list = listOf(trajectory(1L))
      val end = LocalDateTime.of(2025, 1, 2, 0, 0)
      every {
        trajectoryRepository.findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(
          5L,
          LocalDateTime.of(1970, 1, 1, 0, 0),
          end,
        )
      } returns list

      service.getTrajectories(5L, null, null, end) shouldBe list
    }
    test("getLatestTrajectory returns latest or null") {
      val latest = trajectory(3L)
      every { trajectoryRepository.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(5L) } returns latest
      every { trajectoryRepository.findFirstByDeliveryWorkerIdOrderByReportedAtDesc(6L) } returns null

      service.getLatestTrajectory(5L) shouldBe latest
      service.getLatestTrajectory(6L).shouldBeNull()
    }
  })
