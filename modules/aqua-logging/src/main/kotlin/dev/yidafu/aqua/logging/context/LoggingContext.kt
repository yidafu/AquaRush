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

package dev.yidafu.aqua.logging.context

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * 日志上下文，用于聚合请求相关的所有上下文信息
 */
class LoggingContext {
  private val customData = ConcurrentHashMap<String, Any>()

  /**
   * 关联ID
   */
  val correlationId: String? get() = CorrelationIdHolder.getCorrelationId()

  /**
   * 用户上下文
   */
  val userContext: UserContext? get() = UserContextHolder.getUserContext()

  /**
   * 请求开始时间
   */
  val startTime: Instant = Instant.now()

  /**
   * 添加自定义数据
   */
  fun put(
    key: String,
    value: Any,
  ) {
    customData[key] = value
  }

  /**
   * 获取自定义数据
   */
  fun get(key: String): Any? = customData[key]

  /**
   * 移除自定义数据
   */
  fun remove(key: String) = customData.remove(key)

  /**
   * 获取所有自定义数据
   */
  fun getAllCustomData(): Map<String, Any> = customData.toMap()

  /**
   * 清除所有自定义数据
   */
  fun clearCustomData() = customData.clear()

  /**
   * 构建完整的上下文映射
   */
  fun buildContextMap(): Map<String, Any> {
    val contextMap = mutableMapOf<String, Any>()

    // 添加关联ID
    correlationId?.let {
      contextMap["correlationId"] = it
    }

    // 添加用户信息
    userContext?.let { user ->
      contextMap["userId"] = user.userId
      contextMap["username"] = user.username
      user.role?.let { role -> contextMap["userRole"] = role }
      user.tenantId?.let { tenantId -> contextMap["tenantId"] = tenantId }
      if (user.additionalInfo.isNotEmpty()) {
        contextMap.putAll(user.additionalInfo)
      }
    }

    // 添加时间信息
    contextMap["timestamp"] = startTime.toString()

    // 添加自定义数据
    contextMap.putAll(customData)

    return contextMap
  }

  companion object {
    /**
     * 创建当前线程的日志上下文
     */
    fun current(): LoggingContext = LoggingContext()
  }
}
