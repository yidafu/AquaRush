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

package dev.yidafu.aqua.common.cache.spring

import dev.yidafu.aqua.common.cache.*
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * MapDB 缓存自动配置类
 * 专门用于配置 MapDB 相关的缓存组件
 */
@AutoConfiguration
@EnableConfigurationProperties(CacheProperties::class)
@ConditionalOnProperty(prefix = "aqua.cache", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
class MapDBCacheAutoConfiguration {

  
  
  @Bean
  @ConditionalOnMissingBean
  fun mapDBCacheManager(properties: CacheProperties): MapDBCacheManager {
    val config = properties.default
    return MapDBCacheManager(
      config = properties.toCacheConfig(),
    )
  }

  @Bean
  @ConditionalOnMissingBean
  fun mapDBCacheNamespace(): MapDBCacheNamespace {
    return MapDBCacheNamespace()
  }
}
