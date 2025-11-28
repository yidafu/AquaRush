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
import java.util.concurrent.ConcurrentHashMap

/**
 * MapDB缓存命名空间管理器实现
 * 支持多个命名空间，每个命名空间有独立的缓存实例
 */
class MapDBCacheNamespace : CacheNamespace {
  private val logger = LoggerFactory.getLogger(MapDBCacheNamespace::class.java)
  private val namespaceCaches = ConcurrentHashMap<String, CacheManager>()
  private val defaultConfig = CacheConfig()

  override val name: String = "mapdb-cache-namespace"

  init {
    logger.info("MapDB Cache Namespace Manager initialized")
  }

  override fun getCache(namespace: String): CacheManager =
    namespaceCaches.getOrPut(namespace) {
      createNamespace(namespace, defaultConfig)
    }

  override fun clearNamespace(namespace: String) {
    namespaceCaches[namespace]?.clear()
      ?: logger.warn("Namespace '{}' does not exist, cannot clear", namespace)
  }

  override fun getAllNamespaces(): Set<String> = namespaceCaches.keys.toSet()

  override fun hasNamespace(namespace: String): Boolean = namespaceCaches.containsKey(namespace)

  override fun createNamespace(
    namespace: String,
    config: CacheConfig,
  ): CacheManager =
    namespaceCaches.computeIfAbsent(namespace) {
      logger.info("Creating cache namespace '{}' with config: {}", namespace, config)
      MapDBCacheManager(config)
    }

  override fun deleteNamespace(namespace: String): Boolean =
    namespaceCaches.remove(namespace)?.let { cacheManager ->
      if (cacheManager is MapDBCacheManager) {
        cacheManager.shutdown()
      }
      logger.info("Deleted cache namespace: {}", namespace)
      true
    } ?: false

  /**
   * 关闭所有命名空间缓存
   */
  fun shutdown() {
    logger.info("Shutting down all cache namespaces")
    namespaceCaches.values.forEach { cacheManager ->
      if (cacheManager is MapDBCacheManager) {
        cacheManager.shutdown()
      }
    }
    namespaceCaches.clear()
    logger.info("All cache namespaces shutdown completed")
  }

  /**
   * 获取命名空间统计信息
   */
  fun getNamespaceStats(): Map<String, CacheStats> =
    namespaceCaches
      .mapNotNull { (namespace, cacheManager) ->
        if (cacheManager is MapDBCacheManager) {
          namespace to cacheManager.getStats()
        } else {
          null
        }
      }.toMap()
}
