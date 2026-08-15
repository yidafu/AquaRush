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

package dev.yidafu.aqua.logging.service

import dev.yidafu.aqua.common.domain.model.ApiLogModel
import dev.yidafu.aqua.common.domain.model.OperationType
import dev.yidafu.aqua.common.domain.model.RequestType
import dev.yidafu.aqua.logging.config.DatabaseLogProperties
import dev.yidafu.aqua.logging.config.LoggingProperties
import dev.yidafu.aqua.logging.repository.ApiLogRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime
import kotlin.test.assertEquals

class ApiLogServiceTest {
  private lateinit var apiLogRepository: ApiLogRepository
  private lateinit var loggingProperties: LoggingProperties
  private lateinit var service: ApiLogService

  @BeforeEach
  fun setUp() {
    apiLogRepository = mockk(relaxed = true)
    loggingProperties = LoggingProperties()
    service = ApiLogService(apiLogRepository, loggingProperties)
  }

  @Test
  fun `saveApiLog should persist http log`() {
    service.saveApiLog("corr", RequestType.HTTP, "GET", "/api", responseStatus = 200)
    verify { apiLogRepository.save(any<ApiLogModel>()) }
  }

  @Test
  fun `saveApiLog should persist graphql log`() {
    service.saveApiLog("corr", RequestType.GRAPHQL, null, "/graphql", operationType = OperationType.QUERY, operationName = "orders")
    verify { apiLogRepository.save(any<ApiLogModel>()) }
  }

  @Test
  fun `saveApiLog should skip http log when disabled`() {
    val props = LoggingProperties(database = DatabaseLogProperties(httpLogEnabled = false))
    val disabledService = ApiLogService(apiLogRepository, props)
    disabledService.saveApiLog("corr", RequestType.HTTP, "GET", "/api")
    verify(exactly = 0) { apiLogRepository.save(any<ApiLogModel>()) }
  }

  @Test
  fun `saveApiLog should skip graphql log when disabled`() {
    val props = LoggingProperties(database = DatabaseLogProperties(graphqlLogEnabled = false))
    val disabledService = ApiLogService(apiLogRepository, props)
    disabledService.saveApiLog("corr", RequestType.GRAPHQL, null, "/graphql")
    verify(exactly = 0) { apiLogRepository.save(any<ApiLogModel>()) }
  }

  @Test
  fun `saveApiLog should swallow repository exceptions`() {
    every { apiLogRepository.save(any<ApiLogModel>()) } throws RuntimeException("db down")
    service.saveApiLog("corr", RequestType.HTTP, "GET", "/api")
  }

  @Test
  fun `queryApiLogs should delegate to repository filters`() {
    val pageable = PageRequest.of(0, 10)
    val page = PageImpl(listOf(ApiLogModel(correlationId = "c")), pageable, 1)
    every {
      apiLogRepository.findByFilters(any(), any(), any(), any(), any(), any(), any())
    } returns page
    val result =
      service.queryApiLogs(RequestType.HTTP, "GET", 200, 1L, null, null, pageable)
    assertEquals(1, result.totalElements)
  }

  @Test
  fun `findByCorrelationId should delegate to repository`() {
    val model = ApiLogModel(correlationId = "corr")
    every { apiLogRepository.findByCorrelationId("corr") } returns listOf(model)
    val result = service.findByCorrelationId("corr")
    assertEquals(1, result.size)
    assertEquals("corr", result[0].correlationId)
  }

  @Test
  fun `saveApiLog should accept all optional parameters`() {
    service.saveApiLog(
      correlationId = "c",
      requestType = RequestType.HTTP,
      method = "POST",
      uri = "/api",
      query = "a=1",
      requestBody = "{}",
      responseStatus = 500,
      durationMs = 10L,
      ipAddress = "127.0.0.1",
      userAgent = "ua",
      userId = 1L,
      username = "u",
      errorMessage = "err",
      operationType = OperationType.MUTATION,
      operationName = "create",
    )
    verify { apiLogRepository.save(any<ApiLogModel>()) }
  }

  @Test
  fun `queryApiLogs should pass through LocalDateTime filters`() {
    val pageable = PageRequest.of(0, 20)
    val start = LocalDateTime.now().minusDays(1)
    val end = LocalDateTime.now()
    val page = PageImpl(emptyList<ApiLogModel>(), pageable, 0)
    every {
      apiLogRepository.findByFilters(RequestType.HTTP, null, null, null, start, end, pageable)
    } returns page
    val result = service.queryApiLogs(RequestType.HTTP, null, null, null, start, end, pageable)
    assertEquals(0, result.totalElements)
  }
}
