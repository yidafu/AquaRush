/**
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

package dev.yidafu.aqua.common.messaging.outbox

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * outbox 轮询分发器(替代旧 JMS 监听,docs/compose/spec/outbox-messaging.md)。
 *
 * - 专用单线程调度:不与 `@Scheduled` 单线程池共用,避免被长任务(快照/扫描)饿死;
 * - 每周期:`recoverStuck`(可见性超时复位) → `claim`(SKIP LOCKED 认领,自开事务) → 顺序分发;
 * - 分发失败:指数退避重试,达 [OutboxProperties.maxAttempts] 置 DEAD;
 * - 任一环节异常仅记日志(调度任务不中断,下一周期继续)。
 */
@Component
@ConditionalOnProperty(name = ["aqua.outbox.enabled"], havingValue = "true", matchIfMissing = false)
class OutboxPoller(
  private val outboxEventRepository: OutboxEventRepository,
  private val properties: OutboxProperties,
  private val environment: Environment,
  handlers: List<OutboxDestinationHandler>,
) {
  private val logger = LoggerFactory.getLogger(OutboxPoller::class.java)

  /** destination → 处理器路由表。 */
  private val handlerByDestination: Map<String, OutboxDestinationHandler> =
    handlers
      .flatMap { handler -> handler.destinations.map { destination -> destination to handler } }
      .toMap()

  /** 本服务实例身份:aqua.outbox.instance 优先,回退 aqua.mode。 */
  private val instance: String =
    properties.instance.ifBlank {
      environment.getProperty("aqua.mode").orEmpty()
    }

  private val scheduler =
    Executors.newSingleThreadScheduledExecutor { runnable ->
      Thread(runnable, "aqua-outbox-poller").apply { isDaemon = true }
    }

  @PostConstruct
  fun start() {
    require(instance.isNotBlank()) {
      "outbox 已启用但实例身份为空:需配置 aqua.outbox.instance 或 aqua.mode"
    }
    logger.info(
      "Outbox 轮询启动: instance={}, destinations={}, pollIntervalMs={}",
      instance,
      properties.destinations,
      properties.pollIntervalMs,
    )
    scheduler.scheduleWithFixedDelay(
      { pollOnce() },
      0L,
      properties.pollIntervalMs,
      TimeUnit.MILLISECONDS,
    )
  }

  @PreDestroy
  fun stop() {
    scheduler.shutdownNow()
  }

  /**
   * 单周期:崩溃恢复 → 认领 → 顺序分发。全部异常内部兜底,保证周期任务不中断。
   * 内部可见(同模块测试直接驱动,不依赖真实时钟)。
   */
  internal fun pollOnce() {
    try {
      val recovered =
        outboxEventRepository.recoverStuck(
          LocalDateTime.now().minus(Duration.ofMillis(properties.visibilityTimeoutMs)),
        )
      if (recovered > 0) {
        logger.warn("复位超可见性超时的 PROCESSING 行: {}", recovered)
      }
      if (properties.destinations.isEmpty()) return

      val records =
        outboxEventRepository.claim(
          destinations = properties.destinations.toSet(),
          instance = instance,
          limit = properties.batchSize,
        )
      records.forEach { record -> dispatch(record) }
    } catch (e: Exception) {
      logger.error("outbox 轮询周期失败(下周期重试)", e)
    }
  }

  private fun dispatch(record: OutboxEventRecord) {
    val handler = handlerByDestination[record.destination]
    if (handler == null) {
      // 认领过滤已按订阅 destination,理论不会发生;防御性置 DEAD 避免死循环
      logger.error("destination 无处理器: {} (id={})", record.destination, record.id)
      outboxEventRepository.markFailed(
        id = record.id,
        attempts = properties.maxAttempts,
        errorMessage = "no handler registered for destination ${record.destination}",
        retryAt = LocalDateTime.now(),
        dead = true,
      )
      return
    }
    try {
      handler.handle(record)
      outboxEventRepository.markDone(record.id)
    } catch (e: Exception) {
      val attempts = record.retryCount + 1
      val dead = attempts >= properties.maxAttempts
      val retryAt =
        if (dead) {
          LocalDateTime.now()
        } else {
          LocalDateTime.now().plus(Duration.ofMillis(backoffMs(attempts)))
        }
      logger.error(
        "outbox 事件处理失败: id={}, destination={}, eventType={}, attempts={}/{}",
        record.id,
        record.destination,
        record.eventType,
        attempts,
        properties.maxAttempts,
        e,
      )
      outboxEventRepository.markFailed(
        id = record.id,
        attempts = attempts,
        errorMessage = e.message ?: e.toString(),
        retryAt = retryAt,
        dead = dead,
      )
    }
  }

  /** 第 n 次失败后的退避:n=1 → initial,n=2 → initial×multiplier,...,封顶 maxBackoffMs。 */
  internal fun backoffMs(attempts: Int): Long {
    val exponent = (attempts - 1).coerceAtLeast(0)
    val delay = properties.initialBackoffMs * Math.pow(properties.backoffMultiplier, exponent.toDouble())
    return delay.toLong().coerceAtMost(properties.maxBackoffMs)
  }
}
