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
import java.util.concurrent.CompletableFuture

/**
 * 缓存抽象接口
 * 提供通用的缓存操作方法，支持泛型键值对
 */
interface CacheManager {
  /**
   * 存储键值对到缓存
   *
   * @param key 缓存键
   * @param value 缓存值
   * @param ttl 过期时间，null表示永不过期
   */
  fun <T> put(
    key: String,
    value: T,
    ttl: Duration? = null,
  )

  /**
   * 异步存储键值对到缓存
   *
   * @param key 缓存键
   * @param value 缓存值
   * @param ttl 过期时间，null表示永不过期
   * @return CompletableFuture
   */
  fun <T> putAsync(
    key: String,
    value: T,
    ttl: Duration? = null,
  ): CompletableFuture<Void>

  /**
   * 从缓存获取值
   *
   * @param key 缓存键
   * @param clazz 值的类型
   * @return 缓存值，如果不存在返回null
   */
  fun <T> get(
    key: String,
    clazz: Class<T>,
  ): T?

  /**
   * 异步从缓存获取值
   *
   * @param key 缓存键
   * @param clazz 值的类型
   * @return CompletableFuture包含缓存值，如果不存在返回null
   */
  fun <T> getAsync(
    key: String,
    clazz: Class<T>,
  ): CompletableFuture<T?>

  /**
   * 从缓存获取值，如果不存在则使用提供者计算并存储
   *
   * @param key 缓存键
   * @param clazz 值的类型
   * @param supplier 值提供者，当缓存不存在时调用
   * @param ttl 过期时间，null表示永不过期
   * @return 缓存值
   */
  fun <T> getOrPut(
    key: String,
    clazz: Class<T>,
    supplier: () -> T,
    ttl: Duration? = null,
  ): T

  /**
   * 异步从缓存获取值，如果不存在则使用提供者计算并存储
   *
   * @param key 缓存键
   * @param clazz 值的类型
   * @param supplier 异步值提供者
   * @param ttl 过期时间，null表示永不过期
   * @return CompletableFuture包含缓存值
   */
  fun <T> getOrPutAsync(
    key: String,
    clazz: Class<T>,
    supplier: () -> CompletableFuture<T>,
    ttl: Duration? = null,
  ): CompletableFuture<T>

  /**
   * 检查键是否存在
   *
   * @param key 缓存键
   * @return true如果存在，false如果不存在
   */
  fun containsKey(key: String): Boolean

  /**
   * 异步检查键是否存在
   *
   * @param key 缓存键
   * @return CompletableFuture包含true如果存在，false如果不存在
   */
  fun containsKeyAsync(key: String): CompletableFuture<Boolean>

  /**
   * 移除缓存项
   *
   * @param key 缓存键
   * @return true如果成功移除，false如果键不存在
   */
  fun remove(key: String): Boolean

  /**
   * 异步移除缓存项
   *
   * @param key 缓存键
   * @return CompletableFuture包含true如果成功移除，false如果键不存在
   */
  fun removeAsync(key: String): CompletableFuture<Boolean>

  /**
   * 清空所有缓存
   */
  fun clear()

  /**
   * 异步清空所有缓存
   *
   * @return CompletableFuture
   */
  fun clearAsync(): CompletableFuture<Void>

  /**
   * 获取缓存大小
   *
   * @return 缓存项数量
   */
  fun size(): Long

  /**
   * 异步获取缓存大小
   *
   * @return CompletableFuture包含缓存项数量
   */
  fun sizeAsync(): CompletableFuture<Long>

  /**
   * 获取所有键
   *
   * @return 键的集合
   */
  fun keys(): Set<String>

  /**
   * 异步获取所有键
   *
   * @return CompletableFuture包含键的集合
   */
  fun keysAsync(): CompletableFuture<Set<String>>

  /**
   * 批量移除键
   *
   * @param keys 要移除的键集合
   * @return 成功移除的键数量
   */
  fun removeAll(keys: Set<String>): Long

  /**
   * 异步批量移除键
   *
   * @param keys 要移除的键集合
   * @return CompletableFuture包含成功移除的键数量
   */
  fun removeAllAsync(keys: Set<String>): CompletableFuture<Long>

  /**
   * 设置TTL（生存时间）
   *
   * @param key 缓存键
   * @param ttl 过期时间，null表示永不过期
   * @return true如果设置成功，false如果键不存在
   */
  fun setTTL(
    key: String,
    ttl: Duration?,
  ): Boolean

  /**
   * 异步设置TTL（生存时间）
   *
   * @param key 缓存键
   * @param ttl 过期时间，null表示永不过期
   * @return CompletableFuture包含true如果设置成功，false如果键不存在
   */
  fun setTTLAsync(
    key: String,
    ttl: Duration?,
  ): CompletableFuture<Boolean>

  /**
   * 获取剩余TTL
   *
   * @param key 缓存键
   * @return 剩余时间，永不过期返回null，键不存在返回null
   */
  fun getTTL(key: String): Duration?

  /**
   * 异步获取剩余TTL
   *
   * @param key 缓存键
   * @return CompletableFuture包含剩余时间，永不过期返回null，键不存在返回null
   */
  fun getTTLAsync(key: String): CompletableFuture<Duration?>
}
