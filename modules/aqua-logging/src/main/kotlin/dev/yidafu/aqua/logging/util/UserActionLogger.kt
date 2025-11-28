/*
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
import dev.yidafu.aqua.logging.formatter.UserActionLogFormatter
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * 用户操作日志记录器，提供便捷的用户行为记录方法
 */
@Component
@ConditionalOnProperty(prefix = "aqua.logging", name = ["enabled"], matchIfMissing = true)
class UserActionLogger(
  private val loggingProperties: LoggingProperties,
) {
  private val logger = LoggerFactory.getLogger("dev.yidafu.aqua.user.action")
  private val formatter = UserActionLogFormatter.instance

  /**
   * 坐标数据类
   */
  data class Coordinates(
    val screenX: Int, // 屏幕坐标X
    val screenY: Int, // 屏幕坐标Y
    val pageX: Int,   // 页面坐标X
    val pageY: Int,   // 页面坐标Y
  )

  /**
   * 记录页面访问
   */
  fun logPageView(
    pageUrl: String,
    pageTitle: String? = null,
    referrer: String? = null,
    userAgent: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    if (!loggingProperties.userAction.logPageViews) {
      return
    }

    val logMessage = formatter.formatPageView(
      pageUrl = pageUrl,
      pageTitle = pageTitle,
      referrer = referrer,
      userAgent = userAgent,
      additionalData = additionalData
    )

    logger.info(logMessage)
  }

  /**
   * 记录点击事件
   */
  fun logClick(
    elementId: String,
    elementType: String,
    elementText: String? = null,
    coordinates: Coordinates? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    if (!loggingProperties.userAction.logClicks) {
      return
    }

    val coordinatesMap = coordinates?.let {
      mapOf(
        "screenX" to it.screenX,
        "screenY" to it.screenY,
        "pageX" to it.pageX,
        "pageY" to it.pageY
      )
    }

    val logMessage = formatter.formatClick(
      elementId = elementId,
      elementType = elementType,
      elementText = elementText,
      coordinates = coordinatesMap,
      additionalData = additionalData
    )

    logger.info(logMessage)
  }

  /**
   * 记录拖拽事件
   */
  fun logDrag(
    startCoordinates: Coordinates,
    endCoordinates: Coordinates,
    elementId: String? = null,
    elementType: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    if (!loggingProperties.userAction.enabled) {
      return
    }

    val startMap = mapOf(
      "screenX" to startCoordinates.screenX,
      "screenY" to startCoordinates.screenY,
      "pageX" to startCoordinates.pageX,
      "pageY" to startCoordinates.pageY
    )

    val endMap = mapOf(
      "screenX" to endCoordinates.screenX,
      "screenY" to endCoordinates.screenY,
      "pageX" to endCoordinates.pageX,
      "pageY" to endCoordinates.pageY
    )

    val logMessage = formatter.formatDrag(
      startCoordinates = startMap,
      endCoordinates = endMap,
      elementId = elementId,
      elementType = elementType,
      additionalData = additionalData
    )

    logger.info(logMessage)
  }

  /**
   * 记录输入事件
   */
  fun logInput(
    elementId: String,
    inputValue: String,
    inputType: String = "text",
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    if (!loggingProperties.userAction.logInputs) {
      return
    }

    val logMessage = formatter.formatInput(
      elementId = elementId,
      inputValue = inputValue,
      inputType = inputType,
      additionalData = additionalData
    )

    logger.info(logMessage)
  }

  /**
   * 记录后台操作
   */
  fun logBackendOperation(
    operation: String,
    module: String,
    result: String,
    target: String,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    if (!loggingProperties.userAction.logBackendOps) {
      return
    }

    val logMessage = formatter.formatBackendOperation(
      operation = operation,
      module = module,
      result = result,
      target = target,
      additionalData = additionalData
    )

    logger.info(logMessage)
  }

  /**
   * 记录自定义用户操作
   */
  fun logCustomAction(
    actionType: String,
    target: String,
    properties: Map<String, Any> = emptyMap(),
    level: String = "INFO",
  ) {
    if (!loggingProperties.userAction.enabled) {
      return
    }

    val logMessage = formatter.formatCustomAction(
      actionType = actionType,
      target = target,
      properties = properties,
      level = level
    )

    when (level.uppercase()) {
      "ERROR", "FATAL" -> logger.error(logMessage)
      "WARN", "WARNING" -> logger.warn(logMessage)
      "DEBUG" -> logger.debug(logMessage)
      else -> logger.info(logMessage)
    }
  }

  /**
   * 记录用户登录操作
   */
  fun logUserLogin(
    userId: String,
    username: String,
    loginMethod: String,
    success: Boolean,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val properties = mutableMapOf<String, Any>()
    properties["userId"] = userId
    properties["username"] = username
    properties["loginMethod"] = loginMethod
    properties["success"] = success

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    val level = if (success) "INFO" else "WARN"
    logCustomAction("USER_LOGIN", "auth", properties, level)
  }

  /**
   * 记录用户登出操作
   */
  fun logUserLogout(
    userId: String,
    username: String,
    sessionDuration: Long? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val properties = mutableMapOf<String, Any>()
    properties["userId"] = userId
    properties["username"] = username

    sessionDuration?.let {
      properties["sessionDuration"] = it
      properties["sessionDurationMinutes"] = it / 60000 // 转换为分钟
    }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    logCustomAction("USER_LOGOUT", "auth", properties)
  }

  /**
   * 记录表单提交操作
   */
  fun logFormSubmit(
    formId: String,
    formType: String,
    success: Boolean,
    formFields: List<String> = emptyList(),
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val properties = mutableMapOf<String, Any>()
    properties["formId"] = formId
    properties["formType"] = formType
    properties["success"] = success
    properties["fieldCount"] = formFields.size

    if (formFields.isNotEmpty()) {
      properties["fields"] = formFields
    }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    val level = if (success) "INFO" else "WARN"
    logCustomAction("FORM_SUBMIT", formId, properties, level)
  }

  /**
   * 记录文件操作
   */
  fun logFileOperation(
    operation: String,
    fileName: String,
    fileSize: Long? = null,
    fileType: String? = null,
    success: Boolean,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val properties = mutableMapOf<String, Any>()
    properties["fileName"] = fileName
    properties["operation"] = operation
    properties["success"] = success

    fileSize?.let {
      properties["fileSize"] = it
      properties["fileSizeMB"] = String.format("%.2f", it / (1024.0 * 1024.0))
    }

    fileType?.let {
      properties["fileType"] = it
    }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    val level = if (success) "INFO" else "WARN"
    logCustomAction("FILE_OPERATION", fileName, properties, level)
  }

  /**
   * 记录搜索操作
   */
  fun logSearch(
    searchQuery: String,
    searchType: String,
    resultCount: Int? = null,
    searchTime: Long? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val properties = mutableMapOf<String, Any>()
    properties["searchQuery"] = searchQuery
    properties["searchType"] = searchType

    resultCount?.let {
      properties["resultCount"] = it
    }

    searchTime?.let {
      properties["searchTime"] = it
      properties["searchTimeSeconds"] = String.format("%.3f", it / 1000.0)
    }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    logCustomAction("SEARCH", searchType, properties)
  }

  /**
   * 记录分享操作
   */
  fun logShare(
    shareTarget: String,
    shareType: String,
    shareContent: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val properties = mutableMapOf<String, Any>()
    properties["shareTarget"] = shareTarget
    properties["shareType"] = shareType

    shareContent?.let {
      properties["shareContent"] = it
    }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    logCustomAction("SHARE", shareType, properties)
  }

  /**
   * 记录页面滚动
   */
  fun logScroll(
    scrollDirection: String,
    scrollDistance: Int,
    scrollTop: Int,
    documentHeight: Int,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val properties = mutableMapOf<String, Any>()
    properties["scrollDirection"] = scrollDirection
    properties["scrollDistance"] = scrollDistance
    properties["scrollTop"] = scrollTop
    properties["documentHeight"] = documentHeight
    properties["scrollPercentage"] = if (documentHeight > 0) {
      (scrollTop * 100) / documentHeight
    } else {
      0
    }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    logCustomAction("SCROLL", "document", properties)
  }

  companion object {
    @JvmStatic
    val instance: UserActionLogger by lazy {
      UserActionLogger(LoggingProperties())
    }
  }
}
