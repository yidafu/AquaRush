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

import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import dev.yidafu.aqua.logging.config.DatabaseLogProperties
import dev.yidafu.aqua.logging.config.LoggingProperties
import dev.yidafu.aqua.logging.repository.BusinessLogRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import kotlin.test.assertEquals

class BusinessLogServiceTest {
  private lateinit var businessLogRepository: BusinessLogRepository
  private lateinit var service: BusinessLogService

  @BeforeEach
  fun setUp() {
    businessLogRepository = mockk(relaxed = true)
    service = BusinessLogService(businessLogRepository, LoggingProperties())
  }

  @Test
  fun `saveBusinessLog should persist log`() {
    service.saveBusinessLog("corr", "INFO", "ORDER", "message", operation = "CREATE")
    verify { businessLogRepository.save(any<BusinessLogModel>()) }
  }

  @Test
  fun `saveBusinessLog should skip when disabled`() {
    val props = LoggingProperties(database = DatabaseLogProperties(businessLogEnabled = false))
    val disabledService = BusinessLogService(businessLogRepository, props)
    disabledService.saveBusinessLog("corr", "INFO", "ORDER", "message")
    verify(exactly = 0) { businessLogRepository.save(any<BusinessLogModel>()) }
  }

  @Test
  fun `saveBusinessLog should swallow repository exceptions`() {
    every { businessLogRepository.save(any<BusinessLogModel>()) } throws RuntimeException("db down")
    service.saveBusinessLog("corr", "INFO", "ORDER", "message")
  }

  @Test
  fun `saveBusinessLog should pass stack trace and user info`() {
    service.saveBusinessLog(
      correlationId = "c",
      level = "ERROR",
      loggerName = "ORDER",
      message = "m",
      stackTrace = "trace",
      userId = 1L,
      username = "u",
      operation = "CREATE",
    )
    verify { businessLogRepository.save(any<BusinessLogModel>()) }
  }

  @Test
  fun `queryBusinessLogs rest variant should delegate with null operation`() {
    val pageable = PageRequest.of(0, 10)
    val page = PageImpl(listOf(BusinessLogModel(level = "INFO")), pageable, 1)
    every { businessLogRepository.findByFilters(any(), any(), any(), any(), any(), any(), any()) } returns page
    val result = service.queryBusinessLogs("INFO", "ORDER", null, null, null, pageable)
    assertEquals(1, result.totalElements)
  }

  @Test
  fun `queryBusinessLogs graphql variant should pass operation filter`() {
    val pageable = PageRequest.of(0, 10)
    val page = PageImpl(listOf(BusinessLogModel(operation = "CREATE")), pageable, 1)
    every {
      businessLogRepository.findByFilters("INFO", "ORDER", "CREATE", 1L, null, null, pageable)
    } returns page
    val result = service.queryBusinessLogs("INFO", "ORDER", "CREATE", 1L, null, null, pageable)
    assertEquals(1, result.totalElements)
  }

  @Test
  fun `findByCorrelationId should delegate to repository`() {
    val model = BusinessLogModel(correlationId = "corr")
    every { businessLogRepository.findByCorrelationId("corr") } returns listOf(model)
    val result = service.findByCorrelationId("corr")
    assertEquals(1, result.size)
  }
}
