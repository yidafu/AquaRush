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

import dev.yidafu.aqua.common.id.DefaultIdGenerator
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.common.messaging.publisher.EventDestinationRouting
import dev.yidafu.aqua.common.messaging.publisher.EventPublisher
import dev.yidafu.aqua.common.messaging.publisher.EventQueueConst
import dev.yidafu.aqua.common.messaging.publisher.EventTypePublisherType
import org.springframework.stereotype.Component
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Transactional Outbox 事件发布器(替代 ArtemisEventPublisher,spec/tech/design/outbox-messaging.md)。
 *
 * 与旧实现的关键语义差异:
 * - 发布 = 向 outbox_events 同步 INSERT,与调用方业务事务同库同事务(原子性);
 *   序列化或 INSERT 失败向上传播 → 业务事务回滚,不再吞掉异常返回 false
 *   (旧的 catch→false 存在是因为 broker 是独立故障域;outbox 与业务同库,
 *   INSERT 失败即事务本身已不可用)。
 * - 延迟投递 = 写未来 next_run_at,由轮询器到期认领(取代 Artemis _AMQ_SCHED_DELIVERY)。
 * - 广播 = [publishToSubscribers] 为每个订阅服务各写一行。
 */
@Component
class OutboxEventPublisher(
  private val outboxEventRepository: OutboxEventRepository,
) : EventPublisher {
  private val objectMapper = jacksonObjectMapper()

  override suspend fun publish(event: DomainEvent): Boolean = publishSync(event)

  override fun publishSync(event: DomainEvent): Boolean = insertEvent(event, nextRunAt = LocalDateTime.now(), subscriber = null)

  override fun publishDelayed(
    event: DomainEvent,
    deliveryTimeMillis: Long,
  ): Boolean =
    insertEvent(
      event,
      nextRunAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(deliveryTimeMillis), ZoneId.systemDefault()),
      subscriber = null,
    )

  /**
   * 广播扇出:向 [subscribers] 中每个订阅服务各写一行(subscriber 非空,归属隔离),
   * 各服务轮询时只认领自己名下的行。
   */
  fun publishToSubscribers(
    eventType: String,
    payload: String,
    subscribers: List<String>,
  ): Boolean {
    require(subscribers.isNotEmpty()) { "广播扇出 subscribers 不能为空" }
    subscribers.forEach { subscriber ->
      outboxEventRepository.insert(
        id = DefaultIdGenerator().generate(),
        eventType = eventType,
        destination = EventQueueConst.BROADCAST_TOPIC_NAME,
        subscriber = subscriber,
        payload = payload,
        nextRunAt = LocalDateTime.now(),
      )
    }
    return true
  }

  private fun insertEvent(
    event: DomainEvent,
    nextRunAt: LocalDateTime,
    subscriber: String?,
  ): Boolean {
    val id = event.id ?: DefaultIdGenerator().generate()
    // 序列化先于 INSERT:失败同样向上传播(回滚业务事务)
    val payload = objectMapper.writeValueAsString(event)
    outboxEventRepository.insert(
      id = id,
      eventType = event.eventType,
      destination = EventDestinationRouting.destinationFor(event.eventType),
      subscriber = subscriber,
      payload = payload,
      nextRunAt = nextRunAt,
    )
    return true
  }

  override fun getName(): String = "OutboxEventPublisher"

  override fun isAvailable(): Boolean = true

  override fun getType(): EventTypePublisherType = EventTypePublisherType.OUTBOX

  override fun getPriority(): Int = 1
}
