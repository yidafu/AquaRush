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

import dev.yidafu.aqua.logging.config.LoggingProperties
import dev.yidafu.aqua.logging.config.UserActionProperties
import dev.yidafu.aqua.logging.dto.CoordinatesDto
import dev.yidafu.aqua.logging.dto.UserActionLogRequest
import dev.yidafu.aqua.logging.util.UserActionLogger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UserActionEventServiceTest :
  FunSpec({
    var loggingProperties: LoggingProperties = mockk()
    var userActionLogger: UserActionLogger = mockk()
    var userActionLogService: UserActionLogService = mockk()
    var service: UserActionEventService = mockk()

    fun request(
      actionType: String,
      target: String = "target",
      properties: Map<String, Any> = emptyMap(),
      coordinates: CoordinatesDto? = null,
    ): UserActionLogRequest =
      UserActionLogRequest(
        userId = "1",
        username = "u",
        actionType = actionType,
        target = target,
        pageUrl = "/page",
        elementId = "e1",
        elementType = "button",
        elementText = "确定",
        coordinates = coordinates,
        properties = properties,
      )

    beforeTest {
      loggingProperties = LoggingProperties()
      userActionLogger = mockk(relaxed = true)
      userActionLogService = mockk(relaxed = true)
      service = UserActionEventService(loggingProperties, userActionLogger, userActionLogService)
    }
    afterTest {
      service.shutdown()
    }
    test("processUserActionAsync should enqueue request") {
      // 注意:调度器以 0 延迟启动,事件可能已被立即批量处理;仅断言入队调用成功
      val result = service.processUserActionAsync(request("CLICK", "btn"))
      result shouldBe true
    }
    test("processUserActionSync page view should log page view") {
      val result = service.processUserActionSync(request("PAGE_VIEW", "/home", mapOf("pageTitle" to "首页", "referrer" to "/x")))
      result shouldBe true
      verify { userActionLogger.logPageView("/home", "首页", "/x", null, any()) }
    }
    test("processUserActionSync click with coordinates should log click") {
      val coordinates = CoordinatesDto(screenX = 1, screenY = 2, pageX = 3, pageY = 4)
      val result = service.processUserActionSync(request("CLICK", "btn", mapOf("elementType" to "link"), coordinates))
      result shouldBe true
      verify { userActionLogger.logClick("btn", "link", null, any(), any()) }
    }
    test("processUserActionSync click without coordinates should log click with null coordinates") {
      val result = service.processUserActionSync(request("CLICK", "btn"))
      result shouldBe true
      verify { userActionLogger.logClick("btn", "button", null, null, any()) }
    }
    test("processUserActionSync form submit should log form submit") {
      val result =
        service.processUserActionSync(
          request(
            "FORM_SUBMIT",
            "form-1",
            mapOf(
              "success" to true,
              "fields" to listOf("a"),
              "formType" to "order",
            ),
          ),
        )
      result shouldBe true
      verify { userActionLogger.logFormSubmit("form-1", "order", true, listOf("a"), any()) }
    }
    test("processUserActionSync search should log search") {
      val result =
        service.processUserActionSync(
          request("SEARCH", "search", mapOf("searchQuery" to "水", "searchType" to "product", "resultCount" to 3, "searchTime" to 100L)),
        )
      result shouldBe true
      verify { userActionLogger.logSearch("水", "product", 3, 100L, any()) }
    }
    test("processUserActionSync share should log share") {
      val result = service.processUserActionSync(request("SHARE", "share", mapOf("shareType" to "wx", "shareContent" to "hello")))
      result shouldBe true
      verify { userActionLogger.logShare("share", "wx", "hello", any()) }
    }
    test("processUserActionSync scroll should log scroll") {
      val result =
        service.processUserActionSync(
          request(
            "SCROLL",
            "document",
            mapOf("scrollDirection" to "down", "scrollDistance" to 10, "scrollTop" to 50, "documentHeight" to 200),
          ),
        )
      result shouldBe true
      verify { userActionLogger.logScroll("down", 10, 50, 200, any()) }
    }
    test("processUserActionSync file operation should log file operation") {
      val result =
        service.processUserActionSync(
          request(
            "FILE_OPERATION",
            "a.txt",
            mapOf(
              "operation" to "upload",
              "fileName" to "a.txt",
              "fileSize" to 1024L,
              "fileType" to "txt",
              "success" to true,
            ),
          ),
        )
      result shouldBe true
      verify { userActionLogger.logFileOperation("upload", "a.txt", 1024L, "txt", true, any()) }
    }
    test("processUserActionSync custom action should log custom action") {
      val result = service.processUserActionSync(request("CUSTOM_ACTION", "target", mapOf("k" to "v")))
      result shouldBe true
      verify { userActionLogger.logCustomAction("CUSTOM_ACTION", "target", any(), "INFO") }
    }
    test("processUserActionSync should save to database") {
      service.processUserActionSync(request("PAGE_VIEW", "/home"))
      verify { userActionLogService.saveUserActionLog("1", "u", "PAGE_VIEW", "/home", any(), any(), any(), any(), any(), any(), any()) }
    }
    test("processUserActionSync should return false on logger failure") {
      every { userActionLogger.logCustomAction(any(), any(), any(), any()) } throws RuntimeException("boom")
      val result = service.processUserActionSync(request("CUSTOM", "target"))
      result shouldBe false
    }
    test("processUserActionsBatch should process all requests") {
      val requests = listOf(request("PAGE_VIEW", "/a"), request("PAGE_VIEW", "/b"))
      val results = service.processUserActionsBatch(requests)
      results shouldBe listOf(true, true)
    }
    test("processUserActionsBatch should chunk by batch size") {
      val props = LoggingProperties(userAction = UserActionProperties(batchSize = 1))
      val batchService = UserActionEventService(props, userActionLogger, userActionLogService)
      try {
        val requests = listOf(request("PAGE_VIEW", "/a"), request("PAGE_VIEW", "/b"))
        val results = batchService.processUserActionsBatch(requests)
        results shouldBe listOf(true, true)
      } finally {
        batchService.shutdown()
      }
    }
    test("flushQueue should process queued events") {
      service.processUserActionAsync(request("PAGE_VIEW", "/a"))
      service.processUserActionAsync(request("PAGE_VIEW", "/b"))
      service.flushQueue()
      service.getQueueStatus()["queueSize"] shouldBe 0
    }
    test("flushQueue with empty queue should be no-op") {
      service.flushQueue()
      service.getQueueStatus()["queueSize"] shouldBe 0
    }
    test("getQueueStatus should report config") {
      val status = service.getQueueStatus()
      status["batchSize"] shouldBe 100
      status["flushIntervalMs"] shouldBe 5000L
      status["asyncProcessing"] shouldBe true
      status["eventStoreSize"] shouldBe 0
    }
    test("cleanupExpiredEvents should remove stored events") {
      service.cleanupExpiredEvents()
      service.getQueueStatus()["eventStoreSize"] shouldBe 0
    }
    test("shutdown should be idempotent") {
      service.shutdown()
      service.shutdown()
    }
    test("sanitize should strip sensitive keys when enabled") {
      val props = LoggingProperties(userAction = UserActionProperties(sanitizeSensitiveData = true))
      val sensitiveService = UserActionEventService(props, userActionLogger, userActionLogService)
      try {
        val result =
          sensitiveService.processUserActionSync(
            request("PAGE_VIEW", "/home", mapOf("password" to "secret", "phone" to "138", "ok" to "fine")),
          )
        result shouldBe true
        verify {
          userActionLogService.saveUserActionLog(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            match {
              it != null &&
                !it.contains("secret")
            },
          )
        }
      } finally {
        sensitiveService.shutdown()
      }
    }
  })
