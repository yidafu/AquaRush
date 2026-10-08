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

package dev.yidafu.aqua.logging.util

import dev.yidafu.aqua.logging.config.LoggingProperties
import dev.yidafu.aqua.logging.config.UserActionProperties
import io.kotest.core.spec.style.FunSpec

class UserActionLoggerTest :
  FunSpec({
    val enabledLogger = UserActionLogger(LoggingProperties())

    fun disabledLogger(props: UserActionProperties): UserActionLogger = UserActionLogger(LoggingProperties(userAction = props))

    test("logPageView should log when enabled") {
      enabledLogger.logPageView("/home", pageTitle = "首页", referrer = "/login", userAgent = "wechat")
    }
    test("logPageView should skip when disabled") {
      val logger = disabledLogger(UserActionProperties(logPageViews = false))
      logger.logPageView("/home")
    }
    test("logClick should log with coordinates") {
      enabledLogger.logClick(
        elementId = "btn",
        elementType = "button",
        elementText = "确定",
        coordinates = UserActionLogger.Coordinates(1, 2, 3, 4),
        additionalData = mapOf("page" to "/home"),
      )
    }
    test("logClick should log without coordinates") {
      enabledLogger.logClick(elementId = "btn", elementType = "button")
    }
    test("logClick should skip when disabled") {
      val logger = disabledLogger(UserActionProperties(logClicks = false))
      logger.logClick(elementId = "btn", elementType = "button")
    }
    test("logDrag should log drag event") {
      val start = UserActionLogger.Coordinates(0, 0, 10, 10)
      val end = UserActionLogger.Coordinates(0, 0, 30, 40)
      enabledLogger.logDrag(start, end, elementId = "drag", elementType = "map")
    }
    test("logDrag should skip when disabled") {
      val logger = disabledLogger(UserActionProperties(enabled = false))
      val c = UserActionLogger.Coordinates(0, 0, 0, 0)
      logger.logDrag(c, c)
    }
    test("logInput should log when enabled") {
      val logger = disabledLogger(UserActionProperties(logInputs = true))
      logger.logInput("field", "hello", inputType = "text")
    }
    test("logInput should skip when disabled") {
      val logger = disabledLogger(UserActionProperties(logInputs = false))
      logger.logInput("field", "hello")
    }
    test("logCustomAction should log with info level") {
      enabledLogger.logCustomAction("CUSTOM", "target", mapOf("k" to "v"))
    }
    test("logCustomAction should log with error level") {
      enabledLogger.logCustomAction("CUSTOM", "target", level = "ERROR")
    }
    test("logCustomAction should log with warn level") {
      enabledLogger.logCustomAction("CUSTOM", "target", level = "WARN")
    }
    test("logCustomAction should log with debug level") {
      enabledLogger.logCustomAction("CUSTOM", "target", level = "DEBUG")
    }
    test("logCustomAction should skip when disabled") {
      val logger = disabledLogger(UserActionProperties(enabled = false))
      logger.logCustomAction("CUSTOM", "target")
    }
    test("logUserLogin success should log") {
      enabledLogger.logUserLogin("1", "alice", "WECHAT", success = true)
    }
    test("logUserLogin failure should log warn level") {
      enabledLogger.logUserLogin("1", "alice", "WECHAT", success = false, additionalData = mapOf("reason" to "bad code"))
    }
    test("logUserLogout should log with session duration") {
      enabledLogger.logUserLogout("1", "alice", sessionDuration = 60000)
    }
    test("logUserLogout should log without session duration") {
      enabledLogger.logUserLogout("1", "alice")
    }
    test("logFormSubmit should log success") {
      enabledLogger.logFormSubmit("f1", "order", success = true, formFields = listOf("name", "phone"))
    }
    test("logFormSubmit should log failure without fields") {
      enabledLogger.logFormSubmit("f1", "order", success = false)
    }
    test("logFileOperation should log with file size") {
      enabledLogger.logFileOperation("upload", "a.txt", fileSize = 2097152L, fileType = "txt", success = true)
    }
    test("logFileOperation should log without file size and type") {
      enabledLogger.logFileOperation("delete", "a.txt", success = false)
    }
    test("logSearch should log with result count and time") {
      enabledLogger.logSearch("水", "product", resultCount = 5, searchTime = 250L)
    }
    test("logSearch should log without optional params") {
      enabledLogger.logSearch("水", "product")
    }
    test("logShare should log with content") {
      enabledLogger.logShare("wx", "friend", shareContent = "content")
    }
    test("logShare should log without content") {
      enabledLogger.logShare("wx", "friend")
    }
    test("logScroll should log with document height") {
      enabledLogger.logScroll("down", 100, 50, 200)
    }
    test("logScroll should log with zero document height") {
      enabledLogger.logScroll("down", 100, 50, 0)
    }
  })
