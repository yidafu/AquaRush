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

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * 缓存配置属性
 * 支持通过application.yml进行配置
 */
@ConfigurationProperties(prefix = "aqua.cache")
data class CacheProperties(
  /**
   * 是否启用缓存
   */
  val enabled: Boolean = true,
  /**
   * 默认配置
   */
  val default: DefaultCacheConfig = DefaultCacheConfig(),
  /**
   * 命名空间配置
   */
  val namespaces: Map<String, NamespaceCacheConfig> = emptyMap(),
  /**
   * 全局配置
   */
  val global: GlobalCacheConfig = GlobalCacheConfig(),
) {
  /**
   * 默认缓存配置
   */
  data class DefaultCacheConfig(
    /**
     * 默认TTL
     */
    val ttl: Duration = Duration.ofMinutes(30),
    /**
     * 最大缓存大小
     */
    val maxSize: Long = 10000L,
    /**
     * 淘汰策略
     */
    val evictionPolicy: String = "LRU",
    /**
     * 是否启用过期清理
     */
    val enableExpiration: Boolean = true,
    /**
     * 是否启用压缩
     */
    val enableCompression: Boolean = false,
    /**
     * 是否启用异步操作
     */
    val enableAsync: Boolean = true,
  )

  /**
   * 命名空间缓存配置
   */
  data class NamespaceCacheConfig(
    /**
     * TTL
     */
    val ttl: Duration? = null,
    /**
     * 最大缓存大小
     */
    val maxSize: Long? = null,
    /**
     * 淘汰策略
     */
    val evictionPolicy: String? = null,
    /**
     * 是否启用过期清理
     */
    val enableExpiration: Boolean? = null,
    /**
     * 是否启用压缩
     */
    val enableCompression: Boolean? = null,
    /**
     * 是否启用异步操作
     */
    val enableAsync: Boolean? = null,
  )

  /**
   * 全局缓存配置
   */
  data class GlobalCacheConfig(
    /**
     * 持久化配置
     */
    val persistence: PersistenceConfig = PersistenceConfig(),
    /**
     * 统计信息配置
     */
    val stats: StatsConfig = StatsConfig(),
  ) {
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
      /**
       * 统计信息报告间隔（毫秒）
       */
      val reportInterval: Long = 60000L,
    )
  }

  /**
   * 转换为CacheConfig对象
   */
  fun toCacheConfig(namespace: String? = null): CacheConfig {
    val config =
      if (namespace != null && namespaces.containsKey(namespace)) {
        namespaces[namespace]!!
      } else {
        null
      }

    return CacheConfig(
      defaultTTL = config?.ttl ?: default.ttl,
      maxSize = config?.maxSize ?: default.maxSize,
      enableExpiration = config?.enableExpiration ?: default.enableExpiration,
      evictionPolicy =
        when ((config?.evictionPolicy ?: default.evictionPolicy).uppercase()) {
          "LRU" -> CacheConfig.EvictionPolicy.LRU
          "LRU_2Q" -> CacheConfig.EvictionPolicy.LRU_2Q
          "LFU" -> CacheConfig.EvictionPolicy.LFU
          "FIFO" -> CacheConfig.EvictionPolicy.FIFO
          "WEAK" -> CacheConfig.EvictionPolicy.WEAK
          "SOFT" -> CacheConfig.EvictionPolicy.SOFT
          "NONE" -> CacheConfig.EvictionPolicy.NONE
          else -> CacheConfig.EvictionPolicy.LRU
        },
      enableCompression = config?.enableCompression ?: default.enableCompression,
      enableAsync = config?.enableAsync ?: default.enableAsync,
      persistenceConfig =
        PersistenceConfig(
          enabled = global.persistence.enabled,
          filePath = global.persistence.filePath,
          async = global.persistence.async,
          flushInterval = global.persistence.flushInterval,
          forceFlushOnShutdown = global.persistence.forceFlushOnShutdown,
        ),
      statsConfig =
        StatsConfig(
          enabled = global.stats.enabled,
          recordHitRate = global.stats.recordHitRate,
          recordAverageLoadTime = global.stats.recordAverageLoadTime,
          recordEvictionStats = global.stats.recordEvictionStats,
          sampleRate = global.stats.sampleRate,
        ),
    )
  }
}
