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

package dev.yidafu.aqua.logging.formatter

import tools.jackson.databind.ObjectMapper
// import tools.jackson.datatype.jsr310.JavaTimeModule
import dev.yidafu.aqua.logging.context.LoggingContext
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 用户操作日志格式化器，专门用于格式化用户操作行为日志
 */
class UserActionLogFormatter {
  private val objectMapper = ObjectMapper()
  private val dateFormatter = DateTimeFormatter.ISO_INSTANT

  init {
    // 配置ObjectMapper
//    objectMapper.registerModule(JavaTimeModule())
//    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  /**
   * 格式化用户操作日志
   */
  fun format(
    actionType: String,
    target: String,
    properties: Map<String, Any> = emptyMap(),
  ): String {
    val logEntry = mutableMapOf<String, Any>()

    // 基础字段
    logEntry["@timestamp"] = dateFormatter.format(Instant.now())
    logEntry["level"] = "INFO"
    logEntry["logger"] = "user-action"
    logEntry["eventType"] = "USER_ACTION"
    logEntry["actionType"] = actionType.uppercase()
    logEntry["target"] = target

    // 添加上下文信息
    val context = LoggingContext.current()
    context.correlationId?.let { logEntry["correlationId"] = it }
    context.userContext?.let { user ->
      logEntry["userId"] = user.userId
      logEntry["username"] = user.username
      user.role?.let { logEntry["userRole"] = it }
      user.tenantId?.let { logEntry["tenantId"] = it }
    }

    // 添加自定义属性
    if (properties.isNotEmpty()) {
      logEntry.putAll(properties)
    }

    // 添加服务信息
    logEntry["service"] = "aqua-rush"
    logEntry["version"] = "1.0.0"

    return try {
      objectMapper.writeValueAsString(logEntry)
    } catch (e: Exception) {
      fallbackFormat(actionType, target, properties)
    }
  }

  /**
   * 格式化页面访问日志
   */
  fun formatPageView(
    pageUrl: String,
    pageTitle: String? = null,
    referrer: String? = null,
    userAgent: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ): String {
    val properties = mutableMapOf<String, Any>()
    properties["pageUrl"] = pageUrl
    pageTitle?.let { properties["pageTitle"] = it }
    referrer?.let { properties["referrer"] = it }
    userAgent?.let { properties["userAgent"] = it }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    return format("PAGE_VIEW", pageUrl, properties)
  }

  /**
   * 格式化点击事件日志
   */
  fun formatClick(
    elementId: String,
    elementType: String,
    elementText: String? = null,
    coordinates: Map<String, Int>? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ): String {
    val properties = mutableMapOf<String, Any>()
    properties["elementId"] = elementId
    properties["elementType"] = elementType
    elementText?.let { properties["elementText"] = it }

    coordinates?.let {
      properties["coordinates"] = it
      properties["screenX"] = it["screenX"] ?: 0
      properties["screenY"] = it["screenY"] ?: 0
      properties["pageX"] = it["pageX"] ?: 0
      properties["pageY"] = it["pageY"] ?: 0
    }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    return format("CLICK", elementId, properties)
  }

  /**
   * 格式化拖拽事件日志
   */
  fun formatDrag(
    startCoordinates: Map<String, Int>,
    endCoordinates: Map<String, Int>,
    elementId: String? = null,
    elementType: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ): String {
    val properties = mutableMapOf<String, Any>()
    properties["startCoordinates"] = startCoordinates
    properties["endCoordinates"] = endCoordinates
    properties["distanceX"] = (endCoordinates["pageX"] ?: 0) - (startCoordinates["pageX"] ?: 0)
    properties["distanceY"] = (endCoordinates["pageY"] ?: 0) - (startCoordinates["pageY"] ?: 0)

    elementId?.let { properties["elementId"] = it }
    elementType?.let { properties["elementType"] = it }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    val target = elementId ?: "drag_operation"
    return format("DRAG", target, properties)
  }

  /**
   * 格式化输入事件日志
   */
  fun formatInput(
    elementId: String,
    inputValue: String,
    inputType: String = "text",
    inputLength: Int = inputValue.length,
    additionalData: Map<String, Any> = emptyMap(),
  ): String {
    val properties = mutableMapOf<String, Any>()
    properties["elementId"] = elementId
    properties["inputType"] = inputType
    properties["inputLength"] = inputLength

    // 对敏感数据进行脱敏处理
    val sanitizedValue = sanitizeInput(inputValue, inputType)
    if (sanitizedValue != null) {
      properties["inputValue"] = sanitizedValue
    }

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    return format("INPUT", elementId, properties)
  }

  /**
   * 格式化后台操作日志
   */
  fun formatBackendOperation(
    operation: String,
    module: String,
    result: String,
    target: String,
    additionalData: Map<String, Any> = emptyMap(),
  ): String {
    val properties = mutableMapOf<String, Any>()
    properties["operation"] = operation
    properties["module"] = module.uppercase()
    properties["backendResult"] = result
    properties["target"] = target

    if (additionalData.isNotEmpty()) {
      properties.putAll(additionalData)
    }

    val level =
      when (result.uppercase()) {
        "SUCCESS", "COMPLETED" -> "INFO"
        "FAILURE", "ERROR", "FAILED" -> "ERROR"
        "PARTIAL", "WARNING" -> "WARN"
        else -> "INFO"
      }

    return createLogWithLevel(level, "BACKEND_OPERATION", target, properties)
  }

  /**
   * 格式化自定义用户操作日志
   */
  fun formatCustomAction(
    actionType: String,
    target: String,
    properties: Map<String, Any> = emptyMap(),
    level: String = "INFO",
  ): String {
    return createLogWithLevel(level, actionType.uppercase(), target, properties)
  }

  /**
   * 创建指定级别的日志
   */
  private fun createLogWithLevel(
    level: String,
    actionType: String,
    target: String,
    properties: Map<String, Any>,
  ): String {
    val logEntry = mutableMapOf<String, Any>()

    // 基础字段
    logEntry["@timestamp"] = dateFormatter.format(Instant.now())
    logEntry["level"] = level.uppercase()
    logEntry["logger"] = "user-action"
    logEntry["eventType"] = "USER_ACTION"
    logEntry["actionType"] = actionType
    logEntry["target"] = target

    // 添加上下文信息
    val context = LoggingContext.current()
    context.correlationId?.let { logEntry["correlationId"] = it }
    context.userContext?.let { user ->
      logEntry["userId"] = user.userId
      logEntry["username"] = user.username
      user.role?.let { logEntry["userRole"] = it }
      user.tenantId?.let { logEntry["tenantId"] = it }
    }

    // 添加自定义属性
    if (properties.isNotEmpty()) {
      logEntry.putAll(properties)
    }

    // 添加服务信息
    logEntry["service"] = "aqua-rush"
    logEntry["version"] = "1.0.0"

    return try {
      objectMapper.writeValueAsString(logEntry)
    } catch (e: Exception) {
      fallbackFormat(actionType, target, properties)
    }
  }

  /**
   * 对敏感输入数据进行脱敏处理
   */
  private fun sanitizeInput(
    input: String,
    inputType: String,
  ): String? {
    return when (inputType.lowercase()) {
      "password", "passwd", "pwd" -> "***"
      "email" ->
        if (input.contains("@")) {
          val parts = input.split("@")
          if (parts[0].length > 2) {
            "${parts[0].substring(0, 2)}***@${parts[1]}"
          } else {
            "***@${parts[1]}"
          }
        } else {
          "***"
        }
      "phone", "tel", "mobile" ->
        if (input.length >= 7) {
          "${input.substring(0, 3)}***${input.substring(input.length - 4)}"
        } else {
          "***"
        }
      "creditcard", "card", "bankcard" ->
        if (input.length >= 8) {
          "${input.substring(0, 4)}***${input.substring(input.length - 4)}"
        } else {
          "***"
        }
      else -> {
        // 对于一般输入，如果太长则截断
        if (input.length > 100) {
          "${input.substring(0, 50)}...[TRUNCATED]"
        } else {
          input
        }
      }
    }
  }

  /**
   * JSON序列化失败时的备用格式化方法
   */
  private fun fallbackFormat(
    actionType: String,
    target: String,
    properties: Map<String, Any>,
  ): String {
    val parts =
      mutableListOf(
        "[${dateFormatter.format(Instant.now())}]",
        "INFO",
        "user-action",
        "-",
        "User action: $actionType on $target",
      )

    // 添加上下文信息
    val context = LoggingContext.current()
    context.correlationId?.let { parts.add("correlationId=$it") }
    context.userContext?.let { user ->
      parts.add("userId=${user.userId}")
      parts.add("username=${user.username}")
    }

    // 添加属性信息
    properties.forEach { (key, value) ->
      parts.add("$key=$value")
    }

    return parts.joinToString(" ")
  }

  companion object {
    @JvmStatic
    val instance: UserActionLogFormatter by lazy { UserActionLogFormatter() }
  }
}
