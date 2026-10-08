package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.service.order.OrderIdGeneratorService
import org.mapdb.DB
import org.mapdb.DBMaker
import org.mapdb.Serializer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentMap

/**
 * 订单号生成服务实现
 * 使用专用MapDB持久化序列号，确保重启后不重复
 *
 * 订单号格式: YYMMDD + 10位序列号 (共16位)
 * - YYMMDD: 年月日 (6位)
 * - 序列号: 10位 (0-9999999999)，每天从1开始
 */
@Service
class OrderIdGeneratorServiceImpl : OrderIdGeneratorService {
  private val logger = LoggerFactory.getLogger(OrderIdGeneratorServiceImpl::class.java)

  private val formatter = DateTimeFormatter.ofPattern("yyMMdd")

  constructor() {
    val dbFile = java.io.File("./order-id-sequence.db").absoluteFile
    val db =
      DBMaker
        .fileDB(dbFile)
        .fileMmapEnable()
        .fileMmapPreclearDisable()
        // Disable file locking so a JVM killed before closeOnJvmShutdown
        // can run (SIGKILL, OOM, etc.) cannot leave a stale OS-level lock
        // that blocks the next startup. This service is single-instance.
        .fileLockDisable()
        .closeOnJvmShutdown()
        .make()

    orderIdDb = db

    sequenceMap = db.hashMap("order_sequence", Serializer.STRING, Serializer.LONG).createOrOpen()
  }

  // 测试注入：使用传入的 MapDB 实例，不打开文件型存储
  internal constructor(db: DB) {
    orderIdDb = db
    sequenceMap = db.hashMap("order_sequence", Serializer.STRING, Serializer.LONG).createOrOpen()
  }

  // 专用MapDB实例，用于持久化订单序列号
  private val orderIdDb: DB

  // 订单序列号存储，使用日期作为key
  private val sequenceMap: ConcurrentMap<String, Long>

  // 用于测试: 使用传入的DB实例
  companion object {
    fun createWithDb(db: DB): OrderIdGeneratorServiceImpl = OrderIdGeneratorServiceImpl(db)
  }

  @Synchronized
  override fun generateOrderId(): String {
    val dateKey = LocalDate.now().format(formatter)
    val sequence = getNextSequence(dateKey)
    return "$dateKey${sequence.toString().padStart(10, '0')}"
  }

  private fun getNextSequence(dateKey: String): Long {
    val currentSequence = sequenceMap[dateKey] ?: 0L

    // 随机增量 1-1000，确保序列号随机递增
    val randomIncrement = (1..1000).random()
    val nextSequence = currentSequence + randomIncrement

    // 每天序列号从1开始，最大支持100亿/天
    require(nextSequence < 10_000_000_000L) {
      "订单号序列号已达到每日上限: $nextSequence"
    }

    sequenceMap[dateKey] = nextSequence
    orderIdDb.commit()

    logger.debug("Generated order sequence: dateKey=$dateKey, sequence=$nextSequence, increment=$randomIncrement")

    return nextSequence
  }
}
