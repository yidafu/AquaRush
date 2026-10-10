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

import dev.yidafu.aqua.common.domain.table.OutboxEventsTable
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.Duration
import java.time.LocalDateTime

/**
 * outbox 表仓储(PostgreSQL,Transactional Outbox)。
 *
 * 事务性:与 [dev.yidafu.aqua.common.logging.partition.repository.LogPartitionRepositoryImpl]
 * 同约定 —— 主事务管理器是 Exposed `SpringTransactionManager`,发布侧 `insert` 在
 * `@Transactional` 内执行时加入业务事务(原子性);轮询侧 [claim] 自开事务,
 * `FOR UPDATE SKIP LOCKED` 锁在该事务提交(认领完成)时释放,两服务并发认领不重复。
 */
@Repository
class OutboxEventRepository(
  private val database: Database,
) {
  /** 插入一行待投递事件(与调用方业务事务同库同事务)。 */
  fun insert(
    id: Long,
    eventType: String,
    destination: String,
    subscriber: String?,
    payload: String,
    nextRunAt: LocalDateTime,
  ) {
    val timestamp = LocalDateTime.now()
    transaction(database) {
      OutboxEventsTable.insert {
        it[OutboxEventsTable.id] = id
        it[OutboxEventsTable.eventType] = eventType
        it[OutboxEventsTable.destination] = destination
        it[OutboxEventsTable.subscriber] = subscriber
        it[OutboxEventsTable.payload] = payload
        it[OutboxEventsTable.nextRunAt] = nextRunAt
        it[OutboxEventsTable.status] = OutboxStatus.PENDING
        it[OutboxEventsTable.retryCount] = 0
        it[OutboxEventsTable.createdAt] = timestamp
        it[OutboxEventsTable.updatedAt] = timestamp
      }
    }
  }

  /**
   * 认领一批到期事件:`SELECT ... FOR UPDATE SKIP LOCKED` 选行并加锁,
   * 同事务内置为 PROCESSING(记录归属),返回认领快照。
   * 点对点行(subscriber NULL)任一订阅服务可抢;广播行(subscriber 非空)仅归属服务可抢。
   */
  fun claim(
    destinations: Set<String>,
    instance: String,
    limit: Int,
  ): List<OutboxEventRecord> =
    transaction(database) {
      val now = LocalDateTime.now()
      val placeholders = destinations.joinToString(", ") { "?" }
      val args =
        mutableListOf<Pair<org.jetbrains.exposed.v1.core.IColumnType<*>, Any?>>()
      args += JavaLocalDateTimeColumnType() to now
      destinations.forEach { args += VarCharColumnType() to it }
      args += VarCharColumnType() to instance
      args += IntegerColumnType() to limit

      val records =
        exec(
          """
          SELECT id, event_type, destination, subscriber, payload, next_run_at, status, retry_count
          FROM outbox_events
          WHERE status = 'PENDING'
            AND next_run_at <= ?
            AND destination IN ($placeholders)
            AND (subscriber IS NULL OR subscriber = ?)
          ORDER BY next_run_at, id
          LIMIT ?
          FOR UPDATE SKIP LOCKED
          """.trimIndent(),
          args,
        ) { rs ->
          buildList {
            while (rs.next()) {
              add(
                OutboxEventRecord(
                  id = rs.getLong("id"),
                  eventType = rs.getString("event_type"),
                  destination = rs.getString("destination"),
                  subscriber = rs.getString("subscriber"),
                  payload = rs.getString("payload"),
                  nextRunAt = rs.getTimestamp("next_run_at").toLocalDateTime(),
                  status = rs.getString("status"),
                  retryCount = rs.getInt("retry_count"),
                ),
              )
            }
          }
        }.orEmpty()

      if (records.isNotEmpty()) {
        OutboxEventsTable.update({ OutboxEventsTable.id inList records.map { it.id } }) {
          it[OutboxEventsTable.status] = OutboxStatus.PROCESSING
          it[OutboxEventsTable.lockedBy] = instance
          it[OutboxEventsTable.lockedAt] = now
          it[OutboxEventsTable.updatedAt] = now
        }
      }
      records.map { it.copy(status = OutboxStatus.PROCESSING) }
    }

  /** 处理成功:置 DONE(保留至清理任务按保留期删除)。 */
  fun markDone(id: Long) {
    transaction(database) {
      OutboxEventsTable.update({ OutboxEventsTable.id eq id }) {
        it[OutboxEventsTable.status] = OutboxStatus.DONE
        it[OutboxEventsTable.lockedBy] = null
        it[OutboxEventsTable.lockedAt] = null
        it[OutboxEventsTable.updatedAt] = LocalDateTime.now()
      }
    }
  }

  /**
   * 处理失败:累计重试次数;未达上限回 PENDING 并按 [retryAt] 退避重试,
   * 达上限置 DEAD(保留 payload/error_message 供人工排查;[retryAt] 对 DEAD 行无效果,
   * claim 仅取 PENDING)。
   */
  fun markFailed(
    id: Long,
    attempts: Int,
    errorMessage: String,
    retryAt: LocalDateTime,
    dead: Boolean,
  ) {
    transaction(database) {
      OutboxEventsTable.update({ OutboxEventsTable.id eq id }) {
        it[OutboxEventsTable.retryCount] = attempts
        it[OutboxEventsTable.errorMessage] = errorMessage.take(ERROR_MESSAGE_MAX_LENGTH)
        it[OutboxEventsTable.status] =
          if (dead) {
            OutboxStatus.DEAD
          } else {
            OutboxStatus.PENDING
          }
        it[OutboxEventsTable.nextRunAt] = retryAt
        it[OutboxEventsTable.lockedBy] = null
        it[OutboxEventsTable.lockedAt] = null
        it[OutboxEventsTable.updatedAt] = LocalDateTime.now()
      }
    }
  }

  /** 崩溃恢复:PROCESSING 超过可见性超时的行(进程被杀遗留)复位为 PENDING。 */
  fun recoverStuck(lockedBefore: LocalDateTime): Int =
    transaction(database) {
      OutboxEventsTable.update(
        {
          (OutboxEventsTable.status eq OutboxStatus.PROCESSING) and
            (OutboxEventsTable.lockedAt less lockedBefore)
        },
      ) {
        it[OutboxEventsTable.status] = OutboxStatus.PENDING
        it[OutboxEventsTable.lockedBy] = null
        it[OutboxEventsTable.lockedAt] = null
        it[OutboxEventsTable.nextRunAt] = LocalDateTime.now()
        it[OutboxEventsTable.updatedAt] = LocalDateTime.now()
      }
    }

  /** 清理:删除 [cutoff] 之前的 DONE 行(子查询限批,不做无界删除)。 */
  fun deleteDoneBefore(
    cutoff: LocalDateTime,
    limit: Int,
  ) {
    transaction(database) {
      exec(
        """
        DELETE FROM outbox_events
        WHERE id IN (
          SELECT id FROM outbox_events
          WHERE status = 'DONE' AND updated_at < ?
          ORDER BY updated_at
          LIMIT ?
        )
        """.trimIndent(),
        listOf(
          JavaLocalDateTimeColumnType() to cutoff,
          IntegerColumnType() to limit,
        ),
      )
    }
  }

  /** 健康统计(聚合计数)。 */
  fun stats(): OutboxStats {
    val table = OutboxEventsTable
    val now = LocalDateTime.now()
    return transaction(database) {
      val pendingDue =
        table
          .select(table.id)
          .where { (table.status eq OutboxStatus.PENDING) and (table.nextRunAt lessEq now) }
          .count()
      val processing =
        table
          .select(table.id)
          .where { table.status eq OutboxStatus.PROCESSING }
          .count()
      val dead =
        table
          .select(table.id)
          .where { table.status eq OutboxStatus.DEAD }
          .count()
      val oldestPendingCreatedAt =
        table
          .select(table.createdAt)
          .where { table.status eq OutboxStatus.PENDING }
          .orderBy(table.createdAt to SortOrder.ASC)
          .limit(1)
          .firstOrNull()
          ?.get(table.createdAt)
      val oldestPendingAgeMs =
        oldestPendingCreatedAt
          ?.let { Duration.between(it, now).toMillis() }
          ?: 0L
      OutboxStats(
        pendingDue = pendingDue,
        processing = processing,
        dead = dead,
        oldestPendingAgeMs = oldestPendingAgeMs,
      )
    }
  }

  companion object {
    private const val ERROR_MESSAGE_MAX_LENGTH = 2000
  }
}
