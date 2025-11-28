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

package dev.yidafu.aqua.common.messaging.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * 简化的Spring Messaging配置属性
 * 基于ActiveMQ Artemis实现可靠的消息队列
 */
@ConfigurationProperties(prefix = "aqua.messaging")
data class SimplifiedMessagingProperties(
  /**
   * 是否启用消息队列
   */
  var enabled: Boolean = false,
  /**
   * 处理策略: artemis, hybrid, outbox-only, memory-only
   */
  var strategy: String = "artemis",
  /**
   * 内存队列配置
   */
  @NestedConfigurationProperty
  var memoryQueue: MemoryQueueProperties = MemoryQueueProperties(),
  /**
   * Outbox配置（传统模式作为备份）
   */
  @NestedConfigurationProperty
  var outbox: OutboxProperties = OutboxProperties(),
  /**
   * ActiveMQ Artemis配置
   */
  @NestedConfigurationProperty
  var artemis: ArtemisProperties = ArtemisProperties(),
)

/**
 * 内存队列配置属性
 */
data class MemoryQueueProperties(
  /**
   * 是否启用内存队列
   */
  var enabled: Boolean = true,
  /**
   * 最大队列大小
   */
  var maxSize: Int = 5000,
  /**
   * 批处理大小
   */
  var batchSize: Int = 50,
  /**
   * 轮询间隔（毫秒）
   */
  var pollIntervalMs: Long = 50L,
  /**
   * 高频事件类型（使用内存队列处理）
   */
  var highFrequencyEvents: List<String> =
    listOf(
      "ORDER_PAID",
      "PAYMENT_TIMEOUT",
      "DELIVERY_TIMEOUT",
    ),
  /**
   * 低频事件类型（可使用Outbox处理）
   */
  var lowFrequencyEvents: List<String> =
    listOf(
      "ORDER_CREATED",
      "ORDER_CANCELLED",
      "ORDER_DELIVERED",
    ),
)

/**
 * Outbox配置属性（传统模式）
 */
data class OutboxProperties(
  /**
   * 是否启用Outbox模式
   */
  var enabled: Boolean = true,
  /**
   * 轮询间隔（秒）
   */
  var pollIntervalSeconds: Long = 60L,
  /**
   * 最大重试次数
   */
  var maxRetryCount: Int = 5,
  /**
   * 重试延迟（毫秒）
   */
  var retryDelaysMs: List<Long> =
    listOf(
      60000L, // 1分钟
      300000L, // 5分钟
      900000L, // 15分钟
      3600000L, // 1小时
      21600000L, // 6小时
    ),
  /**
   * 清理天数
   */
  var cleanupDays: Int = 30,
)

/**
 * ActiveMQ Artemis配置属性
 */
data class ArtemisProperties(
  /**
   * 连接池配置
   */
  @NestedConfigurationProperty
  var pool: PoolProperties = PoolProperties(),
  /**
   * 重试配置
   */
  @NestedConfigurationProperty
  var retry: RetryProperties = RetryProperties(),
)

/**
 * 连接池配置属性
 */
data class PoolProperties(
  /**
   * 是否启用连接池
   */
  var enabled: Boolean = true,
  /**
   * 最大连接数
   */
  var maxConnections: Int = 10,
  /**
   * 每个连接的最大会话数
   */
  var maxSessionsPerConnection: Int = 50,
)

/**
 * 重试配置属性
 */
data class RetryProperties(
  /**
   * 最大重试次数
   */
  var maxAttempts: Int = 3,
  /**
   * 初始重试间隔（毫秒）
   */
  var initialInterval: Long = 1000,
  /**
   * 重试间隔倍数
   */
  var multiplier: Double = 2.0,
  /**
   * 最大重试间隔（毫秒）
   */
  var maxInterval: Long = 30000,
)
