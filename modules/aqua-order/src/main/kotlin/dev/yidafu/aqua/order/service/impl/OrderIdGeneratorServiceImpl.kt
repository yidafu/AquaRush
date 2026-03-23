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
    val db =
      DBMaker
        .fileDB("./order-id-sequence.db")
        .fileMmapEnable()
        .fileMmapPreclearDisable()
        .closeOnJvmShutdown()
        .make()

    orderIdDb = db

    sequenceMap = db.hashMap("order_sequence", Serializer.STRING, Serializer.LONG).createOrOpen()
  }

  // 专用MapDB实例，用于持久化订单序列号
  private val orderIdDb: DB

  // 订单序列号存储，使用日期作为key
  private val sequenceMap: ConcurrentMap<String, Long>

  // 用于测试: 使用传入的DB实例
  companion object {
    private val testFormatter = DateTimeFormatter.ofPattern("yyMMdd")

    fun createWithDb(db: DB): OrderIdGeneratorServiceImpl {
      return object : OrderIdGeneratorServiceImpl() {
        private val testDb = db
        private val testMap = db.hashMap("order_sequence", Serializer.STRING, Serializer.LONG).createOrOpen()

        @Synchronized
        override fun generateOrderId(): String {
          val dateKey = LocalDate.now().format(testFormatter)
          val sequence = getNextSequenceInternal(dateKey)
          return "$dateKey${sequence.toString().padStart(10, '0')}"
        }

        private fun getNextSequenceInternal(dateKey: String): Long {
          val currentSequence = testMap[dateKey] ?: 0L
          val randomIncrement = (1..1000).random()
          val nextSequence = currentSequence + randomIncrement

          require(nextSequence < 10_000_000_000L) {
            "订单号序列号已达到每日上限: $nextSequence"
          }

          testMap[dateKey] = nextSequence
          testDb.commit()

          return nextSequence
        }
      }
    }
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
