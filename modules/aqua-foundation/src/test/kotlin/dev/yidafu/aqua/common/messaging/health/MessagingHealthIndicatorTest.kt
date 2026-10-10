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

package dev.yidafu.aqua.common.messaging.health

import dev.yidafu.aqua.common.messaging.outbox.OutboxEventRepository
import dev.yidafu.aqua.common.messaging.outbox.OutboxStats
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

/**
 * outbox 健康指标测试:DEAD > 0 → DOWN(告警出口);否则 UP 且携带积压明细。
 */
class MessagingHealthIndicatorTest :
  FunSpec({
    val repository: OutboxEventRepository = mockk()

    test("无 DEAD 时 UP 且携带积压明细") {
      every { repository.stats() } returns
        OutboxStats(pendingDue = 3L, processing = 1L, dead = 0L, oldestPendingAgeMs = 250L)

      val health = MessagingHealthIndicator(repository).health()

      health.status.code shouldBe "UP"
      health.details["pendingDue"] shouldBe 3L
      health.details["processing"] shouldBe 1L
      health.details["dead"] shouldBe 0L
      health.details["oldestPendingAgeMs"] shouldBe 250L
    }

    test("存在 DEAD 事件时 DOWN(健康告警)") {
      every { repository.stats() } returns
        OutboxStats(pendingDue = 0L, processing = 0L, dead = 2L, oldestPendingAgeMs = 0L)

      val health = MessagingHealthIndicator(repository).health()

      health.status.code shouldBe "DOWN"
      health.details["dead"] shouldBe 2L
    }

    test("统计查询异常时 DOWN 且带错误") {
      every { repository.stats() } throws RuntimeException("db down")

      val health = MessagingHealthIndicator(repository).health()

      health.status.code shouldBe "DOWN"
    }
  })
