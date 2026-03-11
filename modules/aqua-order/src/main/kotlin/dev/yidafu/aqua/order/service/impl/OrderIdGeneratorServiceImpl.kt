package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.service.OrderIdGeneratorService
import dev.yidafu.aqua.common.cache.MapDBCacheManager
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class OrderIdGeneratorServiceImpl(
  private val cacheManager: MapDBCacheManager,
) : OrderIdGeneratorService {
  private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHH")
  private val maxSequence = 1_000_000

  override fun generateOrderId(userId: Long): String {
    val timestamp = LocalDateTime.now().format(formatter)
    val userIdPart = userId.toString().takeLast(10).padStart(10, '0')
    val sequence = getNextSequence(timestamp)
    return "$timestamp$userIdPart$sequence"
  }

  @Synchronized
  private fun getNextSequence(key: String): String {
    val counter = cacheManager.get(key, Long::class.java) ?: 1
    val nextCounter = counter + 1
    val sequence = nextCounter % maxSequence
    cacheManager.put(key, sequence, Duration.ofHours(2))
    return sequence.toString().padStart(6, '0')
  }
}
