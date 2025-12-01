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

import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.slf4j.LoggerFactory
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * MapDB缓存管理器实现
 * 使用MapDB作为底层存储，支持TTL、持久化等高级特性
 */
class MapDBCacheManager(
  private val config: CacheConfig = CacheConfig(),
  private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) : CacheManager {
  private val logger = LoggerFactory.getLogger(MapDBCacheManager::class.java)
  private val executorService =
    Executors.newCachedThreadPool { runnable ->
      Thread(runnable, "mapdb-cache-${Thread.currentThread().id}").apply {
        isDaemon = true
      }
    }

  // 用于定时清理过期缓存的调度器
  private val scheduledExecutor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor { runnable ->
      Thread(runnable, "mapdb-cache-cleanup").apply {
        isDaemon = true
      }
    }

  // 数据库实例
  private val db: DB =
    if (config.persistenceConfig.enabled) {
      DBMaker
        .fileDB(config.persistenceConfig.filePath)
        .fileMmapEnable()
        .fileMmapPreclearDisable()
        .make()
    } else {
      DBMaker.memoryDB().make()
    }

  // 主缓存映射
  private val cacheMap =
    db
      .hashMap("cache", Serializer.STRING, Serializer.JAVA)
      .expireMaxSize(config.maxSize)
      .expireAfterCreate()
      .createOrOpen()

  // TTL映射 - 存储键的过期时间
  private val ttlMap =
    db
      .hashMap("ttl", Serializer.STRING, Serializer.LONG)
      .createOrOpen()

  // 统计信息
  private val stats = ConcurrentHashMap<String, Any>()
  private var hitCount = 0L
  private var missCount = 0L
  private var putCount = 0L
  private var evictionCount = 0L

  // 关闭标志
  @Volatile
  private var isShutdown = false

  init {
    // 启动定时清理任务
    if (config.enableExpiration) {
      scheduledExecutor.scheduleAtFixedRate(
        this::cleanupExpired,
        1,
        1,
        TimeUnit.MINUTES,
      )
    }

    // 注册JVM关闭钩子
    Runtime.getRuntime().addShutdownHook(
      Thread {
        shutdown()
      },
    )

    logger.info("MapDB Cache Manager initialized with config: {}", config)
  }

  override fun <T> put(
    key: String,
    value: T,
    ttl: Duration?,
  ) {
    try {
      val serializedValue = objectMapper.writeValueAsString(value)
      cacheMap[key] = serializedValue

      // 设置TTL
      if (ttl != null && ttl > Duration.ZERO) {
        ttlMap[key] = Instant.now().plus(ttl).toEpochMilli()
      } else if (config.defaultTTL > Duration.ZERO) {
        ttlMap[key] = Instant.now().plus(config.defaultTTL).toEpochMilli()
      } else {
        ttlMap.remove(key)
      }

      putCount++

      if (config.persistenceConfig.enabled && !config.persistenceConfig.async) {
        db.commit()
      }

      logger.debug("Cache put: key={}, ttl={}", key, ttl ?: config.defaultTTL)
    } catch (e: Exception) {
      logger.error("Error putting cache value for key: {}", key, e)
      throw CacheException("Failed to put cache value", e)
    }
  }

  override fun <T> putAsync(
    key: String,
    value: T,
    ttl: Duration?,
  ): CompletableFuture<Void> =
    CompletableFuture.runAsync({
      put(key, value, ttl)
    }, executorService)

  override fun <T> get(
    key: String,
    clazz: Class<T>,
  ): T? {
    try {
      // 检查是否过期
      if (isExpired(key)) {
        remove(key)
        missCount++
        return null
      }

      val serializedValue = cacheMap[key] as? String
      return if (serializedValue != null) {
        hitCount++
        try {
          objectMapper.readValue(serializedValue, clazz)
        } catch (e: Exception) {
          logger.warn("Failed to deserialize cache value for key: {}", key, e)
          remove(key)
          null
        }
      } else {
        missCount++
        null
      }
    } catch (e: Exception) {
      logger.error("Error getting cache value for key: {}", key, e)
      missCount++
      return null
    }
  }

  override fun <T> getAsync(
    key: String,
    clazz: Class<T>,
  ): CompletableFuture<T?> =
    CompletableFuture.supplyAsync({
      get(key, clazz)
    }, executorService)

  override fun <T> getOrPut(
    key: String,
    clazz: Class<T>,
    supplier: () -> T,
    ttl: Duration?,
  ): T {
    val cached = get(key, clazz)
    return if (cached != null) {
      cached
    } else {
      val value = supplier()
      put(key, value, ttl)
      value
    }
  }

  override fun <T> getOrPutAsync(
    key: String,
    clazz: Class<T>,
    supplier: () -> CompletableFuture<T>,
    ttl: Duration?,
  ): CompletableFuture<T> {
    val cached = getAsync(key, clazz)
    return cached.thenCompose { value ->
      if (value != null) {
        CompletableFuture.completedFuture(value)
      } else {
        supplier().thenApply { newValue ->
          put(key, newValue, ttl)
          newValue
        }
      }
    }
  }

  override fun containsKey(key: String): Boolean =
    if (isExpired(key)) {
      remove(key)
      false
    } else {
      cacheMap.containsKey(key)
    }

  override fun containsKeyAsync(key: String): CompletableFuture<Boolean> =
    CompletableFuture.supplyAsync({
      containsKey(key)
    }, executorService)

  override fun remove(key: String): Boolean =
    try {
      val existed = cacheMap.containsKey(key)
      cacheMap.remove(key)
      ttlMap.remove(key)

      if (existed) {
        if (config.persistenceConfig.enabled && !config.persistenceConfig.async) {
          db.commit()
        }
        logger.debug("Cache remove: key={}", key)
      }

      existed
    } catch (e: Exception) {
      logger.error("Error removing cache value for key: {}", key, e)
      false
    }

  override fun removeAsync(key: String): CompletableFuture<Boolean> =
    CompletableFuture.supplyAsync({
      remove(key)
    }, executorService)

  override fun clear() {
    try {
      cacheMap.clear()
      ttlMap.clear()
      hitCount = 0
      missCount = 0
      putCount = 0
      evictionCount = 0

      if (config.persistenceConfig.enabled && !config.persistenceConfig.async) {
        db.commit()
      }

      logger.info("Cache cleared")
    } catch (e: Exception) {
      logger.error("Error clearing cache", e)
      throw CacheException("Failed to clear cache", e)
    }
  }

  override fun clearAsync(): CompletableFuture<Void> =
    CompletableFuture.runAsync({
      clear()
    }, executorService)

  override fun size(): Long = cacheMap.size.toLong()

  override fun sizeAsync(): CompletableFuture<Long> =
    CompletableFuture.supplyAsync({
      size()
    }, executorService)

  override fun keys(): Set<String> {
    // 过滤掉过期的键
    return cacheMap.keys.filterNot { isExpired(it) }.toSet()
  }

  override fun keysAsync(): CompletableFuture<Set<String>> =
    CompletableFuture.supplyAsync({
      keys()
    }, executorService)

  override fun removeAll(keys: Set<String>): Long = keys.count { remove(it) }.toLong()

  override fun removeAllAsync(keys: Set<String>): CompletableFuture<Long> =
    CompletableFuture.supplyAsync({
      removeAll(keys)
    }, executorService)

  override fun setTTL(
    key: String,
    ttl: Duration?,
  ): Boolean {
    return try {
      if (!cacheMap.containsKey(key)) {
        return false
      }

      if (ttl != null && ttl > Duration.ZERO) {
        ttlMap[key] = Instant.now().plus(ttl).toEpochMilli()
      } else {
        ttlMap.remove(key)
      }

      if (config.persistenceConfig.enabled && !config.persistenceConfig.async) {
        db.commit()
      }

      true
    } catch (e: Exception) {
      logger.error("Error setting TTL for key: {}", key, e)
      false
    }
  }

  override fun setTTLAsync(
    key: String,
    ttl: Duration?,
  ): CompletableFuture<Boolean> =
    CompletableFuture.supplyAsync({
      setTTL(key, ttl)
    }, executorService)

  override fun getTTL(key: String): Duration? {
    if (!cacheMap.containsKey(key) || isExpired(key)) {
      return null
    }

    val expiryTime = ttlMap[key]
    return if (expiryTime != null) {
      val remaining = Duration.between(Instant.now(), Instant.ofEpochMilli(expiryTime))
      if (remaining.isNegative) {
        remove(key)
        null
      } else {
        remaining
      }
    } else {
      null
    }
  }

  override fun getTTLAsync(key: String): CompletableFuture<Duration?> =
    CompletableFuture.supplyAsync({
      getTTL(key)
    }, executorService)

  /**
   * 检查键是否过期
   */
  private fun isExpired(key: String): Boolean {
    if (!config.enableExpiration) {
      return false
    }

    val expiryTime = ttlMap[key] ?: return false
    return Instant.now().isAfter(Instant.ofEpochMilli(expiryTime))
  }

  /**
   * 清理过期缓存
   */
  private fun cleanupExpired() {
    if (!config.enableExpiration || isShutdown) {
      return
    }

    try {
      val now = Instant.now()
      val expiredKeys =
        ttlMap
          .filter { (_, expiryTime) ->
            now.isAfter(Instant.ofEpochMilli(expiryTime))
          }.keys

      expiredKeys.forEach { key ->
        cacheMap.remove(key)
        ttlMap.remove(key)
        evictionCount++
      }

      if (expiredKeys.isNotEmpty()) {
        if (config.persistenceConfig.enabled && !isShutdown) {
          db.commit()
        }
        logger.debug("Cleaned up {} expired cache entries", expiredKeys.size)
      }
    } catch (e: Exception) {
      logger.error("Error during cache cleanup", e)
    }
  }

  /**
   * 获取缓存统计信息
   */
  fun getStats(): CacheStats =
    CacheStats(
      hitCount = hitCount,
      missCount = missCount,
      putCount = putCount,
      evictionCount = evictionCount,
      size = size(),
      hitRate =
        if (hitCount + missCount > 0) {
          hitCount.toDouble() / (hitCount + missCount)
        } else {
          0.0
        },
    )

  /**
   * 关闭缓存管理器
   */
  fun shutdown() {
    try {
      logger.info("Shutting down MapDB Cache Manager")

      // 强制持久化
      if (config.persistenceConfig.enabled && config.persistenceConfig.forceFlushOnShutdown) {
        db.commit()
      }

      // 关闭数据库
      db.close()

      // 设置关闭标志，防止后续操作
      isShutdown = true

      // 关闭执行器
      executorService.shutdown()
      scheduledExecutor.shutdown()

      if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
        executorService.shutdownNow()
      }

      if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduledExecutor.shutdownNow()
      }

      logger.info("MapDB Cache Manager shutdown completed")
    } catch (e: Exception) {
      logger.error("Error during cache manager shutdown", e)
    }
  }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
  val hitCount: Long,
  val missCount: Long,
  val putCount: Long,
  val evictionCount: Long,
  val size: Long,
  val hitRate: Double,
)

/**
 * 缓存异常
 */
class CacheException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)
