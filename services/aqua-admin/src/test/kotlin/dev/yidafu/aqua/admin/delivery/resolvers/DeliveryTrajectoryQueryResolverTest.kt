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

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.api.service.delivery.DeliveryTrajectoryQueryService
import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class DeliveryTrajectoryQueryResolverTest :
  FunSpec({
    var deliveryTrajectoryQueryService: DeliveryTrajectoryQueryService = mockk()
    var resolver: DeliveryTrajectoryQueryResolver = mockk()

    fun createSampleTrajectory(): DeliveryTrajectoryModel =
      DeliveryTrajectoryModel(
        id = 1L,
        deliveryWorkerId = 2L,
        orderId = 1L,
        longitude = 113.94,
        latitude = 22.54,
        accuracy = 5.0,
        reportedAt = LocalDateTime.now(),
      )

    beforeTest {
      deliveryTrajectoryQueryService = mockk(relaxed = true)
      resolver = DeliveryTrajectoryQueryResolver(deliveryTrajectoryQueryService)
    }
    test("deliveryTrajectories should map trajectories") {
      // Given
      every { deliveryTrajectoryQueryService.getTrajectories(2L, null, null, null) } returns
        listOf(createSampleTrajectory())

      // When
      val result = resolver.deliveryTrajectories(2L, null, null, null)

      // Then
      result.size shouldBe 1
      verify { deliveryTrajectoryQueryService.getTrajectories(2L, null, null, null) }
    }
    test("deliveryWorkerLatestLocation should map latest trajectory") {
      // Given
      every { deliveryTrajectoryQueryService.getLatestTrajectory(2L) } returns createSampleTrajectory()

      // When
      val result = resolver.deliveryWorkerLatestLocation(2L)

      // Then
      result.shouldNotBeNull()
      result!!.longitude shouldBe 113.94f
    }
    test("deliveryWorkerLatestLocation should return null when no trajectory") {
      // Given
      every { deliveryTrajectoryQueryService.getLatestTrajectory(2L) } returns null

      // When
      val result = resolver.deliveryWorkerLatestLocation(2L)

      // Then
      result.shouldBeNull()
    }
  })
