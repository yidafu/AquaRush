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

package dev.yidafu.aqua.review.service.impl

import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.review.domain.repository.DeliveryWorkerStatisticsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 配送员统计跨模块查询服务单元测试。
 */
class DeliveryWorkerStatisticsQueryApiServiceImplTest {
  private lateinit var deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository
  private lateinit var service: DeliveryWorkerStatisticsQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    deliveryWorkerStatisticsRepository = mockk(relaxed = true)
    service = DeliveryWorkerStatisticsQueryApiServiceImpl(deliveryWorkerStatisticsRepository)
  }

  @Test
  @DisplayName("findByDeliveryWorkerId 命中返回统计,未命中返回 null")
  fun `findByDeliveryWorkerId returns stats or null`() {
    val stats = DeliveryWorkerStatisticsModel(id = 1L, deliveryWorkerId = 5L)
    every { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(5L) } returns stats
    every { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(6L) } returns null

    assertEquals(stats, service.findByDeliveryWorkerId(5L))
    assertNull(service.findByDeliveryWorkerId(6L))
    verify(exactly = 1) { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(5L) }
  }
}
