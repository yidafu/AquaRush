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

package dev.yidafu.aqua.common.cache

import java.time.Duration

/**
 * 缓存配置类
 * 定义缓存的各种配置参数
 */
data class CacheConfig(
  /**
   * 默认TTL（生存时间）
   */
  val defaultTTL: Duration = Duration.ofMinutes(30),
  /**
   * 最大缓存条目数
   * 超过此限制时，根据淘汰策略移除条目
   */
  val maxSize: Long = 10000L,
  /**
   * 是否启用过期清理
   */
  val enableExpiration: Boolean = true,
  /**
   * 淘汰策略
   */
  val evictionPolicy: EvictionPolicy = EvictionPolicy.LRU,
  /**
   * 是否启用压缩
   */
  val enableCompression: Boolean = false,
  /**
   * 是否启用异步操作
   */
  val enableAsync: Boolean = true,
  /**
   * 缓存持久化配置
   */
  val persistenceConfig: PersistenceConfig = PersistenceConfig(),
  /**
   * 统计信息配置
   */
  val statsConfig: StatsConfig = StatsConfig(),
) {
  /**
   * 淘汰策略枚举
   */
  enum class EvictionPolicy {
    /**
     * 最近最少使用
     */
    LRU,

    /**
     * 最近最少使用（2-Q算法）
     */
    LRU_2Q,

    /**
     * 最少使用频率
     */
    LFU,

    /**
     * 先进先出
     */
    FIFO,

    /**
     * 弱引用
     */
    WEAK,

    /**
     * 软引用
     */
    SOFT,

    /**
     * 不淘汰
     */
    NONE,
  }
}

/**
 * 持久化配置
 */
data class PersistenceConfig(
  /**
   * 是否启用持久化
   */
  val enabled: Boolean = false,
  /**
   * 持久化文件路径
   */
  val filePath: String = "./cache",
  /**
   * 是否启用异步持久化
   */
  val async: Boolean = true,
  /**
   * 持久化间隔
   */
  val flushInterval: Duration = Duration.ofMinutes(5),
  /**
   * 是否在关闭时强制持久化
   */
  val forceFlushOnShutdown: Boolean = true,
)

/**
 * 统计信息配置
 */
data class StatsConfig(
  /**
   * 是否启用统计信息收集
   */
  val enabled: Boolean = true,
  /**
   * 是否记录命中率
   */
  val recordHitRate: Boolean = true,
  /**
   * 是否记录平均加载时间
   */
  val recordAverageLoadTime: Boolean = true,
  /**
   * 是否记录淘汰统计
   */
  val recordEvictionStats: Boolean = true,
  /**
   * 统计信息采样率（0.0-1.0）
   */
  val sampleRate: Double = 1.0,
)
