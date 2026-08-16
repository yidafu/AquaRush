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

import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import dev.yidafu.aqua.logging.dto.BatchUserAction
import dev.yidafu.aqua.logging.dto.BatchUserActionLogRequest
import dev.yidafu.aqua.logging.dto.UserActionLogRequest
import dev.yidafu.aqua.logging.service.UserActionEventService
import dev.yidafu.aqua.logging.service.UserActionLogService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

class UserActionLogControllerTest :
  FunSpec({
    var userActionLogService: UserActionLogService = mockk()
    var userActionEventService: UserActionEventService = mockk()
    var controller: UserActionLogController = mockk()

    beforeTest {
      userActionLogService = mockk(relaxed = true)
      userActionEventService = mockk(relaxed = true)
      controller = UserActionLogController(userActionLogService, userActionEventService)
    }
    test("queryUserActionLogs should delegate with descending sort") {
      // Given
      val pageable = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
      every { userActionLogService.queryUserActionLogs("1", "CLICK", "张三", null, null, null, pageable) } returns
        PageImpl(listOf(mockk<UserActionLogModel>(relaxed = true)))

      // When
      val result = controller.queryUserActionLogs("1", "CLICK", "张三", null, null, null, 1, 10)

      // Then
      result.data!!.totalElements shouldBe 1
      verify { userActionLogService.queryUserActionLogs("1", "CLICK", "张三", null, null, null, pageable) }
    }
    test("logUserAction should process asynchronously") {
      // Given
      val request =
        UserActionLogRequest(
          userId = "1",
          username = "张三",
          actionType = "CLICK",
          target = "button-add",
        )

      // When
      val result = controller.logUserAction(request, mockk(relaxed = true))

      // Then
      result.body!!.success shouldBe true
      verify { userActionEventService.processUserActionAsync(request) }
    }
    test("logUserAction should not fail on service error") {
      // Given
      every { userActionEventService.processUserActionAsync(any()) } throws RuntimeException("boom")
      val request =
        UserActionLogRequest(
          userId = "1",
          actionType = "CLICK",
          target = "button-add",
        )

      // When
      val result = controller.logUserAction(request, mockk(relaxed = true))

      // Then
      result.body!!.success shouldBe true
    }
    test("logUserActionsBatch should use first x-forwarded-for ip") {
      // Given
      val httpRequest = mockk<HttpServletRequest>(relaxed = true)
      every { httpRequest.getHeader("User-Agent") } returns "Mozilla/5.0"
      every { httpRequest.getHeader("X-Forwarded-For") } returns "10.0.0.1, 10.0.0.2"
      val request =
        BatchUserActionLogRequest(
          actions =
            listOf(
              BatchUserAction(actionType = "CLICK", target = "btn"),
            ),
        )

      // When
      val result = controller.logUserActionsBatch(request, httpRequest)

      // Then
      result.body!!.success shouldBe true
      verify { userActionEventService.processUserActionsBatch(any()) }
    }
    test("logUserActionsBatch should fall back to x-real-ip then remoteAddr") {
      // Given
      val httpRequest = mockk<HttpServletRequest>(relaxed = true)
      every { httpRequest.getHeader("User-Agent") } returns "Mozilla/5.0"
      every { httpRequest.getHeader("X-Forwarded-For") } returns null
      every { httpRequest.getHeader("X-Real-IP") } returns null
      every { httpRequest.remoteAddr } returns "192.168.1.10"
      val request =
        BatchUserActionLogRequest(
          actions = listOf(BatchUserAction(actionType = "VIEW", target = "page")),
        )

      // When
      val result = controller.logUserActionsBatch(request, httpRequest)

      // Then
      result.body!!.success shouldBe true
      verify { userActionEventService.processUserActionsBatch(any()) }
    }
    test("logUserActionsBatch should not fail on service error") {
      // Given
      every { userActionEventService.processUserActionsBatch(any()) } throws RuntimeException("boom")
      val httpRequest = mockk<HttpServletRequest>(relaxed = true)
      every { httpRequest.getHeader("X-Forwarded-For") } returns "10.0.0.1"
      val request =
        BatchUserActionLogRequest(
          actions = listOf(BatchUserAction(actionType = "CLICK", target = "btn")),
        )

      // When
      val result = controller.logUserActionsBatch(request, httpRequest)

      // Then
      result.body!!.success shouldBe true
      result.body!!.data!!.contains("warnings") shouldBe true
    }
  })
