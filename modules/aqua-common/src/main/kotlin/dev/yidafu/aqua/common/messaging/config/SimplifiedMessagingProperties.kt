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
   * ActiveMQ Artemis配置
   */
  @NestedConfigurationProperty
  var artemis: ArtemisProperties = ArtemisProperties(),
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
