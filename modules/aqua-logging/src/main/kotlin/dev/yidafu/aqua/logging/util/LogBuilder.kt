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

import tools.jackson.databind.json.JsonMapper
// import tools.jackson.datatype.jsr310.JavaTimeModule
import dev.yidafu.aqua.logging.context.LoggingContext
import java.time.Instant

/**
 * 日志构建器，用于构建结构化日志消息
 */
class LogBuilder {
  private val objectMapper =
    JsonMapper.builder()
//    .addModule(JavaTimeModule())
      .build()
  private val data = mutableMapOf<String, Any>()

  init {
    // 添加默认时间戳
    data["timestamp"] = Instant.now().toString()

    // 添加上下文信息
    val context = LoggingContext.current()
    data.putAll(context.buildContextMap())
  }

  /**
   * 添加字段
   */
  fun field(
    key: String,
    value: Any,
  ): LogBuilder {
    data[key] = value
    return this
  }

  /**
   * 添加多个字段
   */
  fun fields(fields: Map<String, Any>): LogBuilder {
    data.putAll(fields)
    return this
  }

  /**
   * 添加消息
   */
  fun message(message: String): LogBuilder {
    data["message"] = message
    return this
  }

  /**
   * 添加事件类型
   */
  fun eventType(eventType: String): LogBuilder {
    data["eventType"] = eventType
    return this
  }

  /**
   * 添加操作结果
   */
  fun result(
    success: Boolean,
    error: Throwable? = null,
  ): LogBuilder {
    data["success"] = success
    if (!success && error != null) {
      data["errorType"] = error::class.simpleName ?: "Unknown"
      data["errorMessage"] = error.message ?: "No message"
      data["errorStackTrace"] = error.stackTraceToString()
    }
    return this
  }

  /**
   * 添加性能指标
   */
  fun performance(durationMs: Long): LogBuilder {
    data["durationMs"] = durationMs
    data["performance"] = true
    return this
  }

  /**
   * 添加审计信息
   */
  fun audit(
    action: String,
    resource: String,
    result: String,
  ): LogBuilder {
    data["audit"] = true
    data["auditAction"] = action
    data["auditResource"] = resource
    data["auditResult"] = result
    return this
  }

  /**
   * 添加安全事件信息
   */
  fun security(
    eventType: String,
    severity: String,
    details: Map<String, Any> = emptyMap(),
  ): LogBuilder {
    data["security"] = true
    data["securityEventType"] = eventType
    data["securitySeverity"] = severity
    if (details.isNotEmpty()) {
      data["securityDetails"] = details
    }
    return this
  }

  /**
   * 添加业务操作信息
   */
  fun business(
    operation: String,
    module: String,
    result: String,
  ): LogBuilder {
    data["business"] = true
    data["businessOperation"] = operation
    data["businessModule"] = module
    data["businessResult"] = result
    return this
  }

  /**
   * 构建JSON格式的日志消息
   */
  fun buildJson(): String =
    try {
      objectMapper.writeValueAsString(data)
    } catch (e: Exception) {
      // 如果JSON序列化失败，返回简单的字符串格式
      buildString()
    }

  /**
   * 构建字符串格式的日志消息
   */
  fun buildString(): String {
    val parts = mutableListOf<String>()

    // 添加时间戳
    data["timestamp"]?.let { parts.add("timestamp=$it") }

    // 添加关联ID
    data["correlationId"]?.let { parts.add("correlationId=$it") }

    // 添加用户信息
    data["userId"]?.let { parts.add("userId=$it") }
    data["username"]?.let { parts.add("username=$it") }

    // 添加消息
    data["message"]?.let { parts.add("message=$it") }

    // 添加事件类型
    data["eventType"]?.let { parts.add("eventType=$it") }

    // 添加操作结果
    data["success"]?.let { parts.add("success=$it") }
    data["errorType"]?.let { parts.add("errorType=$it") }
    data["errorMessage"]?.let { parts.add("errorMessage=$it") }

    // 添加性能指标
    data["durationMs"]?.let { parts.add("durationMs=$it") }

    // 添加审计信息
    data["auditAction"]?.let { parts.add("auditAction=$it") }
    data["auditResource"]?.let { parts.add("auditResource=$it") }
    data["auditResult"]?.let { parts.add("auditResult=$it") }

    // 添加业务信息
    data["businessOperation"]?.let { parts.add("businessOperation=$it") }
    data["businessModule"]?.let { parts.add("businessModule=$it") }
    data["businessResult"]?.let { parts.add("businessResult=$it") }

    // 添加安全信息
    data["securityEventType"]?.let { parts.add("securityEventType=$it") }
    data["securitySeverity"]?.let { parts.add("securitySeverity=$it") }

    return parts.joinToString(" | ")
  }

  /**
   * 获取所有数据
   */
  fun getData(): Map<String, Any> = data.toMap()
}
