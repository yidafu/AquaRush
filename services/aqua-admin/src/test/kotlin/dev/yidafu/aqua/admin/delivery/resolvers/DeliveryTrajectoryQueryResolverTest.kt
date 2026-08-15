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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeliveryTrajectoryQueryResolverTest {
  private lateinit var deliveryTrajectoryQueryService: DeliveryTrajectoryQueryService
  private lateinit var resolver: DeliveryTrajectoryQueryResolver

  private fun createSampleTrajectory(): DeliveryTrajectoryModel =
    DeliveryTrajectoryModel(
      id = 1L,
      deliveryWorkerId = 2L,
      orderId = 1L,
      longitude = 113.94,
      latitude = 22.54,
      accuracy = 5.0,
      reportedAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    deliveryTrajectoryQueryService = mockk(relaxed = true)
    resolver = DeliveryTrajectoryQueryResolver(deliveryTrajectoryQueryService)
  }

  @Test
  fun `deliveryTrajectories should map trajectories`() {
    // Given
    every { deliveryTrajectoryQueryService.getTrajectories(2L, null, null, null) } returns
      listOf(createSampleTrajectory())

    // When
    val result = resolver.deliveryTrajectories(2L, null, null, null)

    // Then
    assertEquals(1, result.size)
    verify { deliveryTrajectoryQueryService.getTrajectories(2L, null, null, null) }
  }

  @Test
  fun `deliveryWorkerLatestLocation should map latest trajectory`() {
    // Given
    every { deliveryTrajectoryQueryService.getLatestTrajectory(2L) } returns createSampleTrajectory()

    // When
    val result = resolver.deliveryWorkerLatestLocation(2L)

    // Then
    assertNotNull(result)
    assertEquals(113.94f, result!!.longitude)
  }

  @Test
  fun `deliveryWorkerLatestLocation should return null when no trajectory`() {
    // Given
    every { deliveryTrajectoryQueryService.getLatestTrajectory(2L) } returns null

    // When
    val result = resolver.deliveryWorkerLatestLocation(2L)

    // Then
    assertNull(result)
  }
}
