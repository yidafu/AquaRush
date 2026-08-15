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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserActionEventServiceTest {
  private lateinit var loggingProperties: LoggingProperties
  private lateinit var userActionLogger: UserActionLogger
  private lateinit var userActionLogService: UserActionLogService
  private lateinit var service: UserActionEventService

  private fun request(
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

  @BeforeEach
  fun setUp() {
    loggingProperties = LoggingProperties()
    userActionLogger = mockk(relaxed = true)
    userActionLogService = mockk(relaxed = true)
    service = UserActionEventService(loggingProperties, userActionLogger, userActionLogService)
  }

  @AfterEach
  fun tearDown() {
    service.shutdown()
  }

  @Test
  fun `processUserActionAsync should enqueue request`() {
    // 注意:调度器以 0 延迟启动,事件可能已被立即批量处理;仅断言入队调用成功
    val result = service.processUserActionAsync(request("CLICK", "btn"))
    assertTrue(result)
  }

  @Test
  fun `processUserActionSync page view should log page view`() {
    val result = service.processUserActionSync(request("PAGE_VIEW", "/home", mapOf("pageTitle" to "首页", "referrer" to "/x")))
    assertTrue(result)
    verify { userActionLogger.logPageView("/home", "首页", "/x", null, any()) }
  }

  @Test
  fun `processUserActionSync click with coordinates should log click`() {
    val coordinates = CoordinatesDto(screenX = 1, screenY = 2, pageX = 3, pageY = 4)
    val result = service.processUserActionSync(request("CLICK", "btn", mapOf("elementType" to "link"), coordinates))
    assertTrue(result)
    verify { userActionLogger.logClick("btn", "link", null, any(), any()) }
  }

  @Test
  fun `processUserActionSync click without coordinates should log click with null coordinates`() {
    val result = service.processUserActionSync(request("CLICK", "btn"))
    assertTrue(result)
    verify { userActionLogger.logClick("btn", "button", null, null, any()) }
  }

  @Test
  fun `processUserActionSync form submit should log form submit`() {
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
    assertTrue(result)
    verify { userActionLogger.logFormSubmit("form-1", "order", true, listOf("a"), any()) }
  }

  @Test
  fun `processUserActionSync search should log search`() {
    val result =
      service.processUserActionSync(
        request("SEARCH", "search", mapOf("searchQuery" to "水", "searchType" to "product", "resultCount" to 3, "searchTime" to 100L)),
      )
    assertTrue(result)
    verify { userActionLogger.logSearch("水", "product", 3, 100L, any()) }
  }

  @Test
  fun `processUserActionSync share should log share`() {
    val result = service.processUserActionSync(request("SHARE", "share", mapOf("shareType" to "wx", "shareContent" to "hello")))
    assertTrue(result)
    verify { userActionLogger.logShare("share", "wx", "hello", any()) }
  }

  @Test
  fun `processUserActionSync scroll should log scroll`() {
    val result =
      service.processUserActionSync(
        request(
          "SCROLL",
          "document",
          mapOf("scrollDirection" to "down", "scrollDistance" to 10, "scrollTop" to 50, "documentHeight" to 200),
        ),
      )
    assertTrue(result)
    verify { userActionLogger.logScroll("down", 10, 50, 200, any()) }
  }

  @Test
  fun `processUserActionSync file operation should log file operation`() {
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
    assertTrue(result)
    verify { userActionLogger.logFileOperation("upload", "a.txt", 1024L, "txt", true, any()) }
  }

  @Test
  fun `processUserActionSync custom action should log custom action`() {
    val result = service.processUserActionSync(request("CUSTOM_ACTION", "target", mapOf("k" to "v")))
    assertTrue(result)
    verify { userActionLogger.logCustomAction("CUSTOM_ACTION", "target", any(), "INFO") }
  }

  @Test
  fun `processUserActionSync should save to database`() {
    service.processUserActionSync(request("PAGE_VIEW", "/home"))
    verify { userActionLogService.saveUserActionLog("1", "u", "PAGE_VIEW", "/home", any(), any(), any(), any(), any(), any(), any()) }
  }

  @Test
  fun `processUserActionSync should return false on logger failure`() {
    every { userActionLogger.logCustomAction(any(), any(), any(), any()) } throws RuntimeException("boom")
    val result = service.processUserActionSync(request("CUSTOM", "target"))
    assertFalse(result)
  }

  @Test
  fun `processUserActionsBatch should process all requests`() {
    val requests = listOf(request("PAGE_VIEW", "/a"), request("PAGE_VIEW", "/b"))
    val results = service.processUserActionsBatch(requests)
    assertEquals(listOf(true, true), results)
  }

  @Test
  fun `processUserActionsBatch should chunk by batch size`() {
    val props = LoggingProperties(userAction = UserActionProperties(batchSize = 1))
    val batchService = UserActionEventService(props, userActionLogger, userActionLogService)
    try {
      val requests = listOf(request("PAGE_VIEW", "/a"), request("PAGE_VIEW", "/b"))
      val results = batchService.processUserActionsBatch(requests)
      assertEquals(listOf(true, true), results)
    } finally {
      batchService.shutdown()
    }
  }

  @Test
  fun `flushQueue should process queued events`() {
    service.processUserActionAsync(request("PAGE_VIEW", "/a"))
    service.processUserActionAsync(request("PAGE_VIEW", "/b"))
    service.flushQueue()
    assertEquals(0, service.getQueueStatus()["queueSize"])
  }

  @Test
  fun `flushQueue with empty queue should be no-op`() {
    service.flushQueue()
    assertEquals(0, service.getQueueStatus()["queueSize"])
  }

  @Test
  fun `getQueueStatus should report config`() {
    val status = service.getQueueStatus()
    assertEquals(100, status["batchSize"])
    assertEquals(5000L, status["flushIntervalMs"])
    assertEquals(true, status["asyncProcessing"])
    assertEquals(0, status["eventStoreSize"])
  }

  @Test
  fun `cleanupExpiredEvents should remove stored events`() {
    service.cleanupExpiredEvents()
    assertEquals(0, service.getQueueStatus()["eventStoreSize"])
  }

  @Test
  fun `shutdown should be idempotent`() {
    service.shutdown()
    service.shutdown()
  }

  @Test
  fun `sanitize should strip sensitive keys when enabled`() {
    val props = LoggingProperties(userAction = UserActionProperties(sanitizeSensitiveData = true))
    val sensitiveService = UserActionEventService(props, userActionLogger, userActionLogService)
    try {
      val result =
        sensitiveService.processUserActionSync(
          request("PAGE_VIEW", "/home", mapOf("password" to "secret", "phone" to "138", "ok" to "fine")),
        )
      assertTrue(result)
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
}
