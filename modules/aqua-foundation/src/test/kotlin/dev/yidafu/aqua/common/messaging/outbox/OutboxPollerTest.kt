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

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.core.env.Environment
import java.time.LocalDateTime

/**
 * [OutboxPoller] 分发语义单测:成功 DONE、失败退避/DEAD、无处理器防御、周期异常兜底。
 */
class OutboxPollerTest :
  FunSpec({
    val repository: OutboxEventRepository = mockk()
    val environment: Environment = mockk()

    var failedAttempts = slot<Int>()
    var failedDead = slot<Boolean>()
    var failedRetryAt = slot<LocalDateTime>()

    fun record(
      id: Long = 1L,
      destination: String = "order-events",
      retryCount: Int = 0,
    ) = OutboxEventRecord(
      id = id,
      eventType = "ORDER_CREATED",
      destination = destination,
      subscriber = null,
      payload = "{}",
      nextRunAt = LocalDateTime.now(),
      status = OutboxStatus.PROCESSING,
      retryCount = retryCount,
    )

    fun buildPoller(
      properties: OutboxProperties = OutboxProperties(enabled = true, destinations = listOf("order-events")),
      handlers: List<OutboxDestinationHandler>,
    ): OutboxPoller {
      every { environment.getProperty("aqua.mode") } returns "admin"
      return OutboxPoller(repository, properties, environment, handlers)
    }

    class CountingHandler(
      override val destinations: Set<String>,
      private val failure: RuntimeException? = null,
    ) : OutboxDestinationHandler {
      var handled = 0

      override fun handle(record: OutboxEventRecord) {
        handled++
        failure?.let { throw it }
      }
    }

    beforeTest {
      clearMocks(repository, environment, answers = false)
      failedAttempts = slot()
      failedDead = slot()
      failedRetryAt = slot()
      every { repository.recoverStuck(any()) } returns 0
      every { repository.markDone(any()) } returns Unit
      every {
        repository.markFailed(
          id = any(),
          attempts = capture(failedAttempts),
          errorMessage = any(),
          retryAt = capture(failedRetryAt),
          dead = capture(failedDead),
        )
      } returns Unit
    }

    test("成功分发置 DONE") {
      val handler = CountingHandler(setOf("order-events"))
      every { repository.claim(any(), any(), any()) } returns listOf(record())
      val poller = buildPoller(handlers = listOf(handler))

      poller.pollOnce()

      handler.handled shouldBe 1
      verify { repository.markDone(1L) }
      verify(exactly = 0) { repository.markFailed(any(), any(), any(), any(), any()) }
    }

    test("处理失败按退避重试(attempts=1,未 DEAD)") {
      val handler = CountingHandler(setOf("order-events"), RuntimeException("boom"))
      every { repository.claim(any(), any(), any()) } returns listOf(record(retryCount = 0))
      val poller = buildPoller(handlers = listOf(handler))

      poller.pollOnce()

      failedAttempts.captured shouldBe 1
      failedDead.captured shouldBe false
      failedRetryAt.captured.isAfter(LocalDateTime.now().minusSeconds(1)) shouldBe true
      verify(exactly = 0) { repository.markDone(any()) }
    }

    test("达最大尝试次数置 DEAD") {
      val handler = CountingHandler(setOf("order-events"), RuntimeException("boom"))
      every { repository.claim(any(), any(), any()) } returns listOf(record(retryCount = 7))
      val poller = buildPoller(handlers = listOf(handler))

      poller.pollOnce()

      failedAttempts.captured shouldBe 8
      failedDead.captured shouldBe true
    }

    test("destination 无处理器时防御性置 DEAD") {
      every { repository.claim(any(), any(), any()) } returns listOf(record(destination = "unknown-events"))
      val poller = buildPoller(handlers = listOf(CountingHandler(setOf("order-events"))))

      poller.pollOnce()

      failedDead.captured shouldBe true
      failedAttempts.captured shouldBe 8
      verify(exactly = 0) { repository.markDone(any()) }
    }

    test("claim 异常不中断周期任务") {
      every { repository.claim(any(), any(), any()) } throws RuntimeException("db down")
      val poller = buildPoller(handlers = listOf(CountingHandler(setOf("order-events"))))

      shouldNotThrowAny { poller.pollOnce() }
    }

    test("退避计算:指数增长且封顶") {
      val poller = buildPoller(handlers = listOf(CountingHandler(setOf("order-events"))))
      poller.backoffMs(1) shouldBe 1000L
      poller.backoffMs(2) shouldBe 2000L
      poller.backoffMs(3) shouldBe 4000L
      poller.backoffMs(20) shouldBe 60000L
    }
  })
