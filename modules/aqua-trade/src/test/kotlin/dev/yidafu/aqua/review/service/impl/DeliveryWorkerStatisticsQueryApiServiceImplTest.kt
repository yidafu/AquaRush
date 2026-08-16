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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * 配送员统计跨模块查询服务单元测试。
 */
class DeliveryWorkerStatisticsQueryApiServiceImplTest :
  FunSpec({
    var deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository = mockk()
    var service: DeliveryWorkerStatisticsQueryApiServiceImpl = mockk()

    beforeTest {
      deliveryWorkerStatisticsRepository = mockk(relaxed = true)
      service = DeliveryWorkerStatisticsQueryApiServiceImpl(deliveryWorkerStatisticsRepository)
    }
    test("findByDeliveryWorkerId returns stats or null") {
      val stats = DeliveryWorkerStatisticsModel(id = 1L, deliveryWorkerId = 5L)
      every { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(5L) } returns stats
      every { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(6L) } returns null

      service.findByDeliveryWorkerId(5L) shouldBe stats
      service.findByDeliveryWorkerId(6L).shouldBeNull()
      verify(exactly = 1) { deliveryWorkerStatisticsRepository.findByDeliveryWorkerId(5L) }
    }
  })
