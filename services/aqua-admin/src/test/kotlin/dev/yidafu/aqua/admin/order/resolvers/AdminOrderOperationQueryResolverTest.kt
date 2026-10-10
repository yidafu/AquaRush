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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.OrderOperationType as OrderOperationTypeG

class AdminOrderOperationQueryResolverTest :
  FunSpec({
    var orderOperationService: OrderOperationService = mockk()
    var resolver: AdminOrderOperationQueryResolver = mockk()

    beforeTest {
      orderOperationService = mockk(relaxed = true)
      resolver = AdminOrderOperationQueryResolver(orderOperationService)
    }
    test("orderOperations should return operation history") {
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
      result.size shouldBe 1
      result[0].operationType shouldBe OrderOperationTypeG.ORDER_CREATED
      verify { orderOperationService.getOrderOperations(1L) }
    }
    test("orderOperations should return empty list when no history") {
      // Given
      every { orderOperationService.getOrderOperations(2L) } returns emptyList()

      // When
      val result = resolver.orderOperations(2L)

      // Then
      result.size shouldBe 0
    }
  })
