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

import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.CompletableFuture

/**
 * 缓存模板类
 * 提供简化的缓存操作API，类似于Spring的CacheTemplate
 */
@Component
class CacheTemplate(
  private val cacheNamespace: CacheNamespace,
) {
  /**
   * 获取默认命名空间的缓存管理器
   */
  private val defaultCache: CacheManager = cacheNamespace.getCache("default")

  /**
   * 存储值到缓存
   *
   * @param key 缓存键
   * @param value 缓存值
   * @param ttl 过期时间
   */
  fun <T> put(
    key: String,
    value: T,
    ttl: Duration? = null,
  ) {
    defaultCache.put(key, value, ttl)
  }

  /**
   * 存储值到指定命名空间
   *
   * @param namespace 命名空间
   * @param key 缓存键
   * @param value 缓存值
   * @param ttl 过期时间
   */
  fun <T> put(
    namespace: String,
    key: String,
    value: T,
    ttl: Duration? = null,
  ) {
    cacheNamespace.getCache(namespace).put(key, value, ttl)
  }

  /**
   * 从缓存获取值
   *
   * @param key 缓存键
   * @param clazz 值的类型
   * @return 缓存值
   */
  fun <T> get(
    key: String,
    clazz: Class<T>,
  ): T? = defaultCache.get(key, clazz)

  /**
   * 从指定命名空间获取值
   *
   * @param namespace 命名空间
   * @param key 缓存键
   * @param clazz 值的类型
   * @return 缓存值
   */
  fun <T> get(
    namespace: String,
    key: String,
    clazz: Class<T>,
  ): T? = cacheNamespace.getCache(namespace).get(key, clazz)

  /**
   * 获取值，如果不存在则使用提供者计算并存储
   *
   * @param key 缓存键
   * @param clazz 值的类型
   * @param supplier 值提供者
   * @param ttl 过期时间
   * @return 缓存值
   */
  fun <T> getOrPut(
    key: String,
    clazz: Class<T>,
    supplier: () -> T,
    ttl: Duration? = null,
  ): T = defaultCache.getOrPut(key, clazz, supplier, ttl)

  /**
   * 从指定命名空间获取值，如果不存在则使用提供者计算并存储
   *
   * @param namespace 命名空间
   * @param key 缓存键
   * @param clazz 值的类型
   * @param supplier 值提供者
   * @param ttl 过期时间
   * @return 缓存值
   */
  fun <T> getOrPut(
    namespace: String,
    key: String,
    clazz: Class<T>,
    supplier: () -> T,
    ttl: Duration? = null,
  ): T = cacheNamespace.getCache(namespace).getOrPut(key, clazz, supplier, ttl)

  /**
   * 异步获取值，如果不存在则使用提供者计算并存储
   *
   * @param key 缓存键
   * @param clazz 值的类型
   * @param supplier 异步值提供者
   * @param ttl 过期时间
   * @return CompletableFuture包含缓存值
   */
  fun <T> getOrPutAsync(
    key: String,
    clazz: Class<T>,
    supplier: () -> CompletableFuture<T>,
    ttl: Duration? = null,
  ): CompletableFuture<T> = defaultCache.getOrPutAsync(key, clazz, supplier, ttl)

  /**
   * 从指定命名空间异步获取值，如果不存在则使用提供者计算并存储
   *
   * @param namespace 命名空间
   * @param key 缓存键
   * @param clazz 值的类型
   * @param supplier 异步值提供者
   * @param ttl 过期时间
   * @return CompletableFuture包含缓存值
   */
  fun <T> getOrPutAsync(
    namespace: String,
    key: String,
    clazz: Class<T>,
    supplier: () -> CompletableFuture<T>,
    ttl: Duration? = null,
  ): CompletableFuture<T> = cacheNamespace.getCache(namespace).getOrPutAsync(key, clazz, supplier, ttl)

  /**
   * 检查键是否存在
   *
   * @param key 缓存键
   * @return true如果存在
   */
  fun containsKey(key: String): Boolean = defaultCache.containsKey(key)

  /**
   * 检查指定命名空间的键是否存在
   *
   * @param namespace 命名空间
   * @param key 缓存键
   * @return true如果存在
   */
  fun containsKey(
    namespace: String,
    key: String,
  ): Boolean = cacheNamespace.getCache(namespace).containsKey(key)

  /**
   * 移除缓存项
   *
   * @param key 缓存键
   * @return true如果成功移除
   */
  fun remove(key: String): Boolean = defaultCache.remove(key)

  /**
   * 移除指定命名空间的缓存项
   *
   * @param namespace 命名空间
   * @param key 缓存键
   * @return true如果成功移除
   */
  fun remove(
    namespace: String,
    key: String,
  ): Boolean = cacheNamespace.getCache(namespace).remove(key)

  /**
   * 清空默认命名空间缓存
   */
  fun clear() {
    defaultCache.clear()
  }

  /**
   * 清空指定命名空间缓存
   *
   * @param namespace 命名空间
   */
  fun clear(namespace: String) {
    cacheNamespace.clearNamespace(namespace)
  }

  /**
   * 获取缓存大小
   *
   * @return 缓存项数量
   */
  fun size(): Long = defaultCache.size()

  /**
   * 获取指定命名空间的缓存大小
   *
   * @param namespace 命名空间
   * @return 缓存项数量
   */
  fun size(namespace: String): Long = cacheNamespace.getCache(namespace).size()

  /**
   * 获取所有命名空间
   *
   * @return 命名空间集合
   */
  fun getAllNamespaces(): Set<String> = cacheNamespace.getAllNamespaces()
}
