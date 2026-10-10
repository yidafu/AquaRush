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

import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/**
 * [OutboxEventRepository] 的真实 PostgreSQL 集成测试(Testcontainers)。
 * DDL 逐字对齐 changelog `V320RecreateOutboxEvents`。
 *
 * 覆盖认领互斥(SKIP LOCKED 单次认领)、延迟(到期前不认领)、广播行订阅隔离、
 * 失败退避/DEAD、可见性超时复位、DONE 清理与健康统计。
 */
class OutboxEventRepositoryPgTest :
  FunSpec({
    val database = PostgresTestContainer.connect("aquafoundationoutbox")
    val repository = OutboxEventRepository(database)

    fun sql(statement: String) {
      transaction(database) { exec(statement) }
    }

    fun reset() {
      sql("DROP TABLE IF EXISTS outbox_events")
      sql(
        """
        CREATE TABLE outbox_events (
          id bigint NOT NULL,
          event_type varchar(100) NOT NULL,
          destination varchar(50) NOT NULL,
          subscriber varchar(20),
          payload text NOT NULL,
          next_run_at timestamp NOT NULL,
          status varchar(20) NOT NULL DEFAULT 'PENDING',
          retry_count integer NOT NULL DEFAULT 0,
          error_message text,
          locked_by varchar(64),
          locked_at timestamp,
          created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
          updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
          PRIMARY KEY (id)
        )
        """.trimIndent(),
      )
      sql("CREATE INDEX idx_outbox_status_next_run ON outbox_events (status, next_run_at)")
      sql("CREATE INDEX idx_outbox_destination ON outbox_events (destination)")
      sql("CREATE INDEX idx_outbox_subscriber_status ON outbox_events (subscriber, status)")
    }

    fun insertRow(
      id: Long,
      destination: String = "order-events",
      subscriber: String? = null,
      nextRunAt: LocalDateTime = LocalDateTime.now().minusSeconds(1),
      status: String = OutboxStatus.PENDING,
      retryCount: Int = 0,
      lockedBy: String? = null,
      lockedAt: LocalDateTime? = null,
    ) {
      repository.insert(
        id = id,
        eventType = "ORDER_CREATED",
        destination = destination,
        subscriber = subscriber,
        payload = """{"id":$id}""",
        nextRunAt = nextRunAt,
      )
      if (status != OutboxStatus.PENDING || retryCount != 0 || lockedBy != null) {
        sql(
          """
          UPDATE outbox_events
          SET status = '$status', retry_count = $retryCount,
              locked_by = ${lockedBy?.let { "'$it'" } ?: "NULL"},
              locked_at = ${lockedAt?.let { "'$it'" } ?: "NULL"}
          WHERE id = $id
          """.trimIndent(),
        )
      }
    }

    beforeTest { reset() }

    test("认领:到期的点对点行仅可被认领一次(第二次为空)") {
      insertRow(1L)

      val first = repository.claim(setOf("order-events"), instance = "admin", limit = 10)
      first shouldHaveSize 1
      first.single().id shouldBe 1L
      first.single().status shouldBe OutboxStatus.PROCESSING

      val second = repository.claim(setOf("order-events"), instance = "client", limit = 10)
      second.shouldBeEmpty()
    }

    test("认领:未到期(延迟)行不被认领") {
      insertRow(2L, nextRunAt = LocalDateTime.now().plusMinutes(30))

      repository.claim(setOf("order-events"), instance = "admin", limit = 10).shouldBeEmpty()
    }

    test("认领:按 destination 过滤,不订阅的队列不认领") {
      insertRow(3L, destination = "payment-expiry-events")

      repository.claim(setOf("order-events"), instance = "admin", limit = 10).shouldBeEmpty()
      repository.claim(setOf("payment-expiry-events"), instance = "admin", limit = 10) shouldHaveSize 1
    }

    test("认领:广播行按 subscriber 隔离,归属服务才可认领") {
      insertRow(4L, destination = "broadcast-events", subscriber = "admin")
      insertRow(5L, destination = "broadcast-events", subscriber = "client")

      val clientClaims = repository.claim(setOf("broadcast-events"), instance = "client", limit = 10)
      clientClaims shouldHaveSize 1
      clientClaims.single().id shouldBe 5L

      val adminClaims = repository.claim(setOf("broadcast-events"), instance = "admin", limit = 10)
      adminClaims shouldHaveSize 1
      adminClaims.single().id shouldBe 4L
    }

    test("认领:按 next_run_at 升序返回最老行") {
      insertRow(6L, nextRunAt = LocalDateTime.now().minusSeconds(1))
      insertRow(7L, nextRunAt = LocalDateTime.now().minusSeconds(5))

      val claimed = repository.claim(setOf("order-events"), instance = "admin", limit = 10)
      claimed shouldHaveSize 2
      claimed.first().id shouldBe 7L
      claimed.last().id shouldBe 6L
    }

    test("markDone 置 DONE 并清除锁") {
      insertRow(8L)
      repository.claim(setOf("order-events"), instance = "admin", limit = 10)
      repository.markDone(8L)

      repository.claim(setOf("order-events"), instance = "admin", limit = 10).shouldBeEmpty()
      repository.stats().pendingDue shouldBe 0L
    }

    test("markFailed 未达上限回 PENDING 并按 retryAt 退避,达上限置 DEAD") {
      insertRow(9L)
      repository.claim(setOf("order-events"), instance = "admin", limit = 10)

      val retryAt = LocalDateTime.now().plusSeconds(30)
      repository.markFailed(id = 9L, attempts = 1, errorMessage = "boom", retryAt = retryAt, dead = false)

      // 退避中(next_run_at 在未来)不可认领
      repository.claim(setOf("order-events"), instance = "admin", limit = 10).shouldBeEmpty()
      repository.stats().pendingDue shouldBe 0L

      repository.markFailed(id = 9L, attempts = 8, errorMessage = "boom", retryAt = LocalDateTime.now(), dead = true)
      repository.stats().dead shouldBe 1L
      repository.claim(setOf("order-events"), instance = "admin", limit = 10).shouldBeEmpty()
    }

    test("recoverStuck 将超可见性超时的 PROCESSING 行复位为 PENDING") {
      insertRow(10L)
      repository.claim(setOf("order-events"), instance = "admin", limit = 10)

      // locked_at 为现在,未超时 → 不复位
      repository.recoverStuck(LocalDateTime.now().minusMinutes(5)) shouldBe 0

      // 人为把 locked_at 拨回 10 分钟前 → 复位
      sql("UPDATE outbox_events SET locked_at = now() - interval '10 minutes' WHERE id = 10")
      repository.recoverStuck(LocalDateTime.now().minusMinutes(5)) shouldBe 1

      val reclaimed = repository.claim(setOf("order-events"), instance = "client", limit = 10)
      reclaimed shouldHaveSize 1
      reclaimed.single().id shouldBe 10L
    }

    test("deleteDoneBefore 仅删除保留期之外的 DONE 行") {
      insertRow(11L)
      insertRow(12L)
      repository.claim(setOf("order-events"), instance = "admin", limit = 10)
      repository.markDone(11L)
      repository.markDone(12L)
      sql("UPDATE outbox_events SET updated_at = now() - interval '10 days' WHERE id = 11")

      repository.deleteDoneBefore(cutoff = LocalDateTime.now().minusDays(7), limit = 100) shouldBe 1

      val remaining = repository.stats()
      // 11 被清理;12 仍在(未过保留期)—— stats 不计 DONE,直接查行数
      transaction(database) {
        exec("SELECT count(*) FROM outbox_events") { rs ->
          rs.next()
          rs.getInt(1)
        }
      } shouldBe 1
      remaining.dead shouldBe 0L
    }

    test("stats 聚合 pendingDue/processing/dead 与最老待处理年龄") {
      insertRow(13L)
      insertRow(14L, nextRunAt = LocalDateTime.now().plusMinutes(10))
      repository.claim(setOf("order-events"), instance = "admin", limit = 1) // 13 → PROCESSING

      val stats = repository.stats()
      stats.pendingDue shouldBe 0L
      stats.processing shouldBe 1L
      stats.dead shouldBe 0L
      stats.oldestPendingAgeMs shouldBeGreaterThanOrEqual 0L
    }
  })
