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

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Logger 扩展函数，提供便捷的日志记录方法
 */

/**
 * 记录业务操作日志
 */
fun Logger.logBusiness(
  operation: String,
  module: String,
  result: String = "SUCCESS",
  message: String? = null,
  additionalData: Map<String, Any> = emptyMap(),
) {
  val logBuilder =
    LogBuilder()
      .business(operation, module, result)
      .eventType("BUSINESS_OPERATION")

  message?.let { logBuilder.message(it) }
  if (additionalData.isNotEmpty()) {
    logBuilder.fields(additionalData)
  }

  when (result) {
    "SUCCESS" -> info(logBuilder.buildJson())
    "FAILURE" -> error(logBuilder.buildJson())
    else -> warn(logBuilder.buildJson())
  }
}

/**
 * 记录性能日志
 */
fun Logger.logPerformance(
  operation: String,
  durationMs: Long,
  message: String? = null,
  additionalData: Map<String, Any> = emptyMap(),
) {
  val logBuilder =
    LogBuilder()
      .performance(durationMs)
      .eventType("PERFORMANCE")
      .message(operation)

  message?.let { logBuilder.message(it) }
  if (additionalData.isNotEmpty()) {
    logBuilder.fields(additionalData)
  }

  when {
    durationMs < 100 -> debug(logBuilder.buildJson())
    durationMs < 1000 -> info(logBuilder.buildJson())
    else -> warn(logBuilder.buildJson())
  }
}

/**
 * 记录安全事件日志
 */
fun Logger.logSecurity(
  eventType: String,
  severity: String = "INFO",
  message: String? = null,
  details: Map<String, Any> = emptyMap(),
) {
  val logBuilder =
    LogBuilder()
      .security(eventType, severity, details)
      .eventType("SECURITY")

  message?.let { logBuilder.message(it) }

  when (severity.uppercase()) {
    "CRITICAL", "HIGH" -> error(logBuilder.buildJson())
    "MEDIUM" -> warn(logBuilder.buildJson())
    else -> info(logBuilder.buildJson())
  }
}

/**
 * 记录审计日志
 */
fun Logger.logAudit(
  action: String,
  resource: String,
  result: String,
  message: String? = null,
  additionalData: Map<String, Any> = emptyMap(),
) {
  val logBuilder =
    LogBuilder()
      .audit(action, resource, result)
      .eventType("AUDIT")

  message?.let { logBuilder.message(it) }
  if (additionalData.isNotEmpty()) {
    logBuilder.fields(additionalData)
  }

  // 审计日志总是使用INFO级别
  info(logBuilder.buildJson())
}

/**
 * 记录结构化日志
 */
fun Logger.structured(
  level: LogLevel = LogLevel.INFO,
  message: String,
  eventType: String,
  data: Map<String, Any> = emptyMap(),
) {
  val logBuilder =
    LogBuilder()
      .message(message)
      .eventType(eventType)
      .fields(data)

  val logMessage = logBuilder.buildJson()

  when (level) {
    LogLevel.TRACE -> trace(logMessage)
    LogLevel.DEBUG -> debug(logMessage)
    LogLevel.INFO -> info(logMessage)
    LogLevel.WARN -> warn(logMessage)
    LogLevel.ERROR -> error(logMessage)
  }
}

/**
 * 记录异常信息
 */
fun Logger.logError(
  message: String,
  exception: Throwable,
  additionalData: Map<String, Any> = emptyMap(),
) {
  val logBuilder =
    LogBuilder()
      .message(message)
      .eventType("EXCEPTION")
      .result(false, exception)
      .fields(additionalData)

  error(logBuilder.buildJson(), exception)
}

/**
 * 日志级别枚举
 */
enum class LogLevel {
  TRACE,
  DEBUG,
  INFO,
  WARN,
  ERROR,
}

/**
 * 为任意类获取Logger的扩展函数
 */
inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)
