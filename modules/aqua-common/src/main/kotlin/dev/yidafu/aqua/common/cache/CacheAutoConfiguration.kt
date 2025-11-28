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

import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 缓存自动配置类
 * 在Spring Boot环境中自动配置缓存组件
 */
@AutoConfiguration
@EnableConfigurationProperties(CacheProperties::class)
@ConditionalOnProperty(prefix = "aqua.cache", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
class CacheAutoConfiguration {
  private val logger = LoggerFactory.getLogger(CacheAutoConfiguration::class.java)
  private var cacheNamespace: MapDBCacheNamespace? = null

  @Bean
  fun cacheNamespace(properties: CacheProperties): CacheNamespace {
    if (!properties.enabled) {
      throw IllegalStateException("Cache is disabled but bean creation was attempted")
    }

    val namespace = MapDBCacheNamespace()
    cacheNamespace = namespace

    // 预创建配置中定义的命名空间
    properties.namespaces.forEach { (name, config) ->
      logger.info("Pre-creating cache namespace: {} with config: {}", name, config)
      namespace.createNamespace(name, properties.toCacheConfig(name))
    }

    // 创建默认命名空间
    if (!namespace.hasNamespace("default")) {
      namespace.createNamespace("default", properties.toCacheConfig())
    }

    logger.info("Cache namespace manager initialized with namespaces: {}", namespace.getAllNamespaces())
    return namespace
  }

  @Bean
  fun cacheManager(namespace: CacheNamespace): CacheManager = namespace.getCache("default")

  @PreDestroy
  fun cleanup() {
    cacheNamespace?.shutdown()
    logger.info("Cache auto configuration cleanup completed")
  }
}

/**
 * 缓存配置类
 * 提供额外的缓存相关配置
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "aqua.cache", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class CacheConfiguration {
  private val logger = LoggerFactory.getLogger(CacheConfiguration::class.java)

  /**
   * 可选：缓存监听器Bean，用于监控缓存事件
   */
  @Bean
  @ConditionalOnProperty(prefix = "aqua.cache.stats", name = ["enabled"], havingValue = "true", matchIfMissing = true)
  fun cacheStatsListener(
    namespace: CacheNamespace,
    properties: CacheProperties,
  ): CacheStatsListener? {
    if (!properties.global.stats.enabled) {
      return null
    }

    return CacheStatsListener(namespace, properties).apply {
      logger.info("Cache statistics listener enabled")
    }
  }
}
