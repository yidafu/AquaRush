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

package dev.yidafu.aqua.admin.logging.resolvers

import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import dev.yidafu.aqua.common.graphql.generated.BizLogDomain
import dev.yidafu.aqua.common.graphql.generated.BizLogLevel
import dev.yidafu.aqua.common.graphql.generated.BusinessLogListInput
import dev.yidafu.aqua.logging.service.BusinessLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.time.LocalDateTime
import kotlin.test.assertEquals

class AdminBusinessLogQueryResolverTest {
  private lateinit var businessLogService: BusinessLogService
  private lateinit var resolver: AdminBusinessLogQueryResolver

  private fun createSampleLog(): BusinessLogModel =
    BusinessLogModel(
      id = 1L,
      correlationId = "corr-1",
      level = "INFO",
      loggerName = "ORDER",
      operation = "ORDER_CREATED",
      message = "订单创建",
      createdAt = LocalDateTime.now(),
    )

  @BeforeEach
  fun setUp() {
    businessLogService = mockk(relaxed = true)
    resolver = AdminBusinessLogQueryResolver(businessLogService)
  }

  @Test
  fun `businessLogs should pass through input filters`() {
    // Given
    val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
    every {
      businessLogService.queryBusinessLogs("INFO", "ORDER", "ORDER_CREATED", 1L, null, null, pageable)
    } returns PageImpl(listOf(createSampleLog()))
    val input =
      BusinessLogListInput(
        domain = BizLogDomain.ORDER,
        endTime = null,
        level = BizLogLevel.INFO,
        operationType = "ORDER_CREATED",
        page = 0,
        size = 10,
        startTime = null,
        userId = 1L,
      )

    // When
    val result = resolver.businessLogs(input)

    // Then
    assertEquals(1, result.list.size)
    assertEquals(1, result.pageInfo.total)
    verify { businessLogService.queryBusinessLogs("INFO", "ORDER", "ORDER_CREATED", 1L, null, null, pageable) }
  }

  @Test
  fun `businessLogs should use defaults when input is null`() {
    // Given
    val pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
    every {
      businessLogService.queryBusinessLogs(null, null, null, null, null, null, pageable)
    } returns PageImpl(emptyList())

    // When
    val result = resolver.businessLogs(null)

    // Then
    assertEquals(0, result.list.size)
    verify { businessLogService.queryBusinessLogs(null, null, null, null, null, null, pageable) }
  }
}
