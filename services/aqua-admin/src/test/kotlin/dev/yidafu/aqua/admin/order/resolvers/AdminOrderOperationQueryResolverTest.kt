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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AdminOrderOperationQueryResolverTest {
  private lateinit var orderOperationService: OrderOperationService
  private lateinit var resolver: AdminOrderOperationQueryResolver

  @BeforeEach
  fun setUp() {
    orderOperationService = mockk(relaxed = true)
    resolver = AdminOrderOperationQueryResolver(orderOperationService)
  }

  @Test
  fun `orderOperations should return operation history`() {
    // Given
    val operations =
      listOf(
        OrderOperationModel(
          id = 1L,
          orderId = 1L,
          operationType = OrderOperationType.ORDER_CREATED,
          operatorId = 1L,
          createdAt = LocalDateTime.now(),
        ),
      )
    every { orderOperationService.getOrderOperations(1L) } returns operations

    // When
    val result = resolver.orderOperations(1L)

    // Then
    assertEquals(1, result.size)
    assertEquals(OrderOperationType.ORDER_CREATED, result[0].operationType)
    verify { orderOperationService.getOrderOperations(1L) }
  }

  @Test
  fun `orderOperations should return empty list when no history`() {
    // Given
    every { orderOperationService.getOrderOperations(2L) } returns emptyList()

    // When
    val result = resolver.orderOperations(2L)

    // Then
    assertEquals(0, result.size)
  }
}
