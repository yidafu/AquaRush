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

package dev.yidafu.aqua.admin.logging.controller

import dev.yidafu.aqua.common.domain.model.ApiLogModel
import dev.yidafu.aqua.common.domain.model.RequestType
import dev.yidafu.aqua.logging.service.ApiLogService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import kotlin.test.assertEquals

class ApiLogControllerTest {
  private lateinit var apiLogService: ApiLogService
  private lateinit var controller: ApiLogController

  @BeforeEach
  fun setUp() {
    apiLogService = mockk(relaxed = true)
    controller = ApiLogController(apiLogService)
  }

  @Test
  fun `queryApiLogs should delegate with descending sort`() {
    // Given
    val pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
    every { apiLogService.queryApiLogs(RequestType.HTTP, "GET", 200, 1L, null, null, pageable) } returns
      PageImpl(listOf(mockk<ApiLogModel>(relaxed = true)))

    // When
    val result = controller.queryApiLogs(RequestType.HTTP, "GET", 200, 1L, null, null, 0, 20)

    // Then
    assertEquals(1, result.data!!.totalElements)
    verify { apiLogService.queryApiLogs(RequestType.HTTP, "GET", 200, 1L, null, null, pageable) }
  }

  @Test
  fun `getApiLogsByCorrelationId should return logs`() {
    // Given
    every { apiLogService.findByCorrelationId("corr-1") } returns listOf(mockk<ApiLogModel>(relaxed = true))

    // When
    val result = controller.getApiLogsByCorrelationId("corr-1")

    // Then
    assertEquals(1, result.data!!.size)
    verify { apiLogService.findByCorrelationId("corr-1") }
  }
}
