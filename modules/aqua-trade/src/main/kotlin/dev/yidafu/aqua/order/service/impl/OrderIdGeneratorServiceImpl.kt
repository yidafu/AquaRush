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
    orderIdDb = fileBackedDb
    sequenceMap = fileBackedSequenceMap
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

  companion object {
    /**
     * **进程级单例**的文件型 MapDB。
     *
     * 为什么必须是单例：同一 JVM 里可能同时存在多个 Spring 上下文（服务集成测试会缓存
     * 数十个上下文），每个上下文都会实例化本服务。若每个实例各自 `DBMaker.fileDB()`
     * 打开同一个文件，多个 MapDB 实例会在 `fileLockDisable()` 下以 mmap 同时持有它，
     * 写出损坏的 header —— 之后任何上下文启动都抛
     * `DBException$DataCorruption: Header checksum broken`。因为
     * `order-id-sequence.db` 被 `.gitignore` 忽略，全新 clone 首次跑 admin 集成测试
     * 必然踩到（此前靠本机遗留的一个「健康」文件掩盖了问题）。
     *
     * 生产只有一个 Spring 上下文，单例语义与原来完全一致。
     *
     * 若确实遇到 header 损坏（例如上一次 JVM 被 SIGKILL）：不要用
     * `checksumHeaderBypass()` 绕过 —— 那会让序列号回退、可能生成重复订单号。
     * 该文件只是本机序列号缓存，直接删除即可，服务会重新建库。
     */
    private val fileBackedDb: DB by lazy {
      val dbFile = java.io.File("./order-id-sequence.db").absoluteFile
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
    }

    private val fileBackedSequenceMap: ConcurrentMap<String, Long> by lazy {
      fileBackedDb.hashMap("order_sequence", Serializer.STRING, Serializer.LONG).createOrOpen()
    }

    // 用于测试: 使用传入的DB实例
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
