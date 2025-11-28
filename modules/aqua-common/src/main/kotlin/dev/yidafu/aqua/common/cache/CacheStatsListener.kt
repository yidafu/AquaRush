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

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.atomic.AtomicLong

/**
 * 缓存统计监听器
 * 定期收集和报告缓存统计信息
 */
class CacheStatsListener(
  private val namespace: CacheNamespace,
  private val properties: CacheProperties,
) {
  private val logger = LoggerFactory.getLogger(CacheStatsListener::class.java)
  private val lastReportTime = AtomicLong(System.currentTimeMillis())

  init {
    logger.info("Cache statistics listener initialized")
    reportInitialStats()
  }

  /**
   * 定时报告缓存统计信息
   */
  @Scheduled(fixedRate = 60000) // 60 seconds, can be made configurable later
  fun reportStats() {
    try {
      if (namespace is MapDBCacheNamespace) {
        val allStats = namespace.getNamespaceStats()

        if (allStats.isNotEmpty()) {
          logger.info("=== Cache Statistics Report ===")
          allStats.forEach { (nsName, stats) ->
            logger.info("Namespace: {}", nsName)
            logger.info("  Size: {} entries", stats.size)
            logger.info("  Hit Rate: {:.2f}%", stats.hitRate * 100)
            logger.info("  Hits: {}, Misses: {}", stats.hitCount, stats.missCount)
            logger.info("  Puts: {}, Evictions: {}", stats.putCount, stats.evictionCount)
            logger.info("  ---")
          }
          logger.info("=== End Cache Statistics ===")
        }
      }
    } catch (e: Exception) {
      logger.error("Error reporting cache statistics", e)
    }
  }

  /**
   * 报告初始统计信息
   */
  private fun reportInitialStats() {
    try {
      logger.info("Cache initialized with namespaces: {}", namespace.getAllNamespaces())

      if (namespace is MapDBCacheNamespace) {
        val allStats = namespace.getNamespaceStats()
        allStats.forEach { (nsName, stats) ->
          logger.info("Namespace '{}' initialized with size: {}", nsName, stats.size)
        }
      }
    } catch (e: Exception) {
      logger.error("Error reporting initial cache statistics", e)
    }
  }

  /**
   * 获取命名空间统计信息
   */
  fun getNamespaceStats(namespaceName: String): CacheStats? =
    if (namespace is MapDBCacheNamespace) {
      namespace.getNamespaceStats()[namespaceName]
    } else {
      null
    }

  /**
   * 获取所有命名空间统计信息
   */
  fun getAllStats(): Map<String, CacheStats> =
    if (namespace is MapDBCacheNamespace) {
      namespace.getNamespaceStats()
    } else {
      emptyMap()
    }
}
