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

import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import dev.yidafu.aqua.logging.service.BusinessLogService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class BizLogControllerTest :
  FunSpec({
    var businessLogService: BusinessLogService = mockk()
    var controller: BizLogController = mockk()

    beforeTest {
      businessLogService = mockk(relaxed = true)
      controller = BizLogController(businessLogService)
    }
    test("queryBusinessLogs should delegate with descending sort") {
      // Given
      val pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
      every { businessLogService.queryBusinessLogs("INFO", "ORDER", 1L, null, null, pageable) } returns
        PageImpl(listOf(mockk<BusinessLogModel>(relaxed = true)))

      // When
      val result = controller.queryBusinessLogs("INFO", "ORDER", 1L, null, null, 1, 10)

      // Then
      result.data!!.totalElements shouldBe 1
      verify { businessLogService.queryBusinessLogs("INFO", "ORDER", 1L, null, null, pageable) }
    }
    test("getBusinessLogsByCorrelationId should return logs") {
      // Given
      every { businessLogService.findByCorrelationId("corr-2") } returns listOf(mockk<BusinessLogModel>(relaxed = true))

      // When
      val result = controller.getBusinessLogsByCorrelationId("corr-2")

      // Then
      result.size shouldBe 1
      verify { businessLogService.findByCorrelationId("corr-2") }
    }
  })
