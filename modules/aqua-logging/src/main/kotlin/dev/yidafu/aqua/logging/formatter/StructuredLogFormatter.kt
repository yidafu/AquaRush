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

import dev.yidafu.aqua.logging.context.LoggingContext
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 结构化日志格式化器，将日志事件格式化为JSON格式
 */
class StructuredLogFormatter {
  private val objectMapper = ObjectMapper()
  private val dateFormatter = DateTimeFormatter.ISO_INSTANT

  init {
    // 配置ObjectMapper
//    objectMapper.registerModule(JavaTimeModule())
//    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  /**
   * 格式化日志事件为JSON字符串
   */
  fun format(
    level: String,
    loggerName: String,
    message: String,
    exception: Throwable? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ): String {
    val logEntry = mutableMapOf<String, Any>()

    // 基础字段
    logEntry["@timestamp"] = dateFormatter.format(Instant.now())
    logEntry["level"] = level.uppercase()
    logEntry["logger"] = loggerName
    logEntry["message"] = message

    // 添加上下文信息
    val context = LoggingContext.current()
    context.correlationId?.let { logEntry["correlationId"] = it }
    context.userContext?.let { user ->
      logEntry["userId"] = user.userId
      logEntry["username"] = user.username
      user.role?.let { logEntry["userRole"] = it }
      user.tenantId?.let { logEntry["tenantId"] = it }
    }

    // 添加异常信息
    exception?.let {
      logEntry["errorType"] = it::class.simpleName ?: "Unknown"
      logEntry["errorMessage"] = it.message ?: "No message"
      logEntry["errorStackTrace"] = it.stackTraceToString()
    }

    // 添加自定义数据
    if (additionalData.isNotEmpty()) {
      logEntry.putAll(additionalData)
    }

    // 添加服务信息
    logEntry["service"] = "aqua-rush"
    logEntry["version"] = "1.0.0"

    return try {
      objectMapper.writeValueAsString(logEntry)
    } catch (e: Exception) {
      // 如果JSON序列化失败，返回简化的格式
      fallbackFormat(level, loggerName, message, exception, additionalData)
    }
  }

  /**
   * 格式化性能日志
   */
  fun formatPerformance(
    operation: String,
    durationMs: Long,
    success: Boolean = true,
    additionalData: Map<String, Any> = emptyMap(),
  ): String {
    val logEntry = mutableMapOf<String, Any>()

    // 基础字段
    logEntry["@timestamp"] = dateFormatter.format(Instant.now())
    logEntry["level"] = if (success) "INFO" else "WARN"
    logEntry["logger"] = "performance"
    logEntry["eventType"] = "PERFORMANCE"
    logEntry["operation"] = operation
    logEntry["durationMs"] = durationMs
    logEntry["success"] = success

    // 性能分类
    logEntry["performanceCategory"] =
      when {
        durationMs < 50 -> "FAST"
        durationMs < 200 -> "NORMAL"
        durationMs < 1000 -> "SLOW"
        else -> "VERY_SLOW"
      }

    // 添加上下文信息
    val context = LoggingContext.current()
    context.correlationId?.let { logEntry["correlationId"] = it }
    context.userContext?.let { user ->
      logEntry["userId"] = user.userId
      logEntry["username"] = user.username
    }

    // 添加自定义数据
    if (additionalData.isNotEmpty()) {
      logEntry.putAll(additionalData)
    }

    logEntry["service"] = "aqua-rush"

    return try {
      objectMapper.writeValueAsString(logEntry)
    } catch (e: Exception) {
      fallbackFormat("INFO", "performance", "Performance: $operation took ${durationMs}ms", null, additionalData)
    }
  }

  /**
   * 格式化审计日志
   */
  fun formatAudit(
    action: String,
    resource: String,
    result: String,
    details: Map<String, Any> = emptyMap(),
  ): String {
    val logEntry = mutableMapOf<String, Any>()

    // 基础字段
    logEntry["@timestamp"] = dateFormatter.format(Instant.now())
    logEntry["level"] = "INFO"
    logEntry["logger"] = "audit"
    logEntry["eventType"] = "AUDIT"
    logEntry["auditAction"] = action
    logEntry["auditResource"] = resource
    logEntry["auditResult"] = result

    // 添加上下文信息
    val context = LoggingContext.current()
    context.correlationId?.let { logEntry["correlationId"] = it }
    context.userContext?.let { user ->
      logEntry["userId"] = user.userId
      logEntry["username"] = user.username
      user.role?.let { logEntry["userRole"] = it }
    }

    // 添加审计详情
    if (details.isNotEmpty()) {
      logEntry["auditDetails"] = details
    }

    logEntry["service"] = "aqua-rush"

    return try {
      objectMapper.writeValueAsString(logEntry)
    } catch (e: Exception) {
      fallbackFormat("INFO", "audit", "Audit: $action on $resource resulted in $result", null, details)
    }
  }

  /**
   * 格式化安全事件日志
   */
  fun formatSecurity(
    eventType: String,
    severity: String,
    message: String,
    details: Map<String, Any> = emptyMap(),
  ): String {
    val logEntry = mutableMapOf<String, Any>()

    // 基础字段
    logEntry["@timestamp"] = dateFormatter.format(Instant.now())
    logEntry["level"] =
      when (severity.uppercase()) {
        "CRITICAL", "HIGH" -> "ERROR"
        "MEDIUM" -> "WARN"
        else -> "INFO"
      }
    logEntry["logger"] = "security"
    logEntry["eventType"] = "SECURITY"
    logEntry["securityEventType"] = eventType
    logEntry["securitySeverity"] = severity
    logEntry["message"] = message

    // 添加上下文信息
    val context = LoggingContext.current()
    context.correlationId?.let { logEntry["correlationId"] = it }

    // 对于安全事件，如果有用户上下文则记录，否则可能是匿名操作
    context.userContext?.let { user ->
      logEntry["userId"] = user.userId
      logEntry["username"] = user.username
      user.role?.let { logEntry["userRole"] = it }
    }

    // 添加安全详情
    if (details.isNotEmpty()) {
      logEntry["securityDetails"] = details
    }

    logEntry["service"] = "aqua-rush"

    return try {
      objectMapper.writeValueAsString(logEntry)
    } catch (e: Exception) {
      fallbackFormat(logEntry["level"] as String, "security", message, null, details)
    }
  }

  /**
   * JSON序列化失败时的备用格式化方法
   */
  private fun fallbackFormat(
    level: String,
    loggerName: String,
    message: String,
    exception: Throwable? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ): String {
    val parts =
      mutableListOf(
        "[${dateFormatter.format(Instant.now())}]",
        level.uppercase(),
        loggerName,
        "-",
        message,
      )

    // 添加上下文信息
    val context = LoggingContext.current()
    context.correlationId?.let { parts.add("correlationId=$it") }
    context.userContext?.let { user ->
      parts.add("userId=${user.userId}")
      parts.add("username=${user.username}")
    }

    // 添加异常信息
    exception?.let {
      parts.add("errorType=${it::class.simpleName}")
      parts.add("errorMessage=${it.message}")
    }

    // 添加自定义数据
    additionalData.forEach { (key, value) ->
      parts.add("$key=$value")
    }

    return parts.joinToString(" ")
  }

  companion object {
    @JvmStatic
    val instance: StructuredLogFormatter by lazy { StructuredLogFormatter() }
  }
}
