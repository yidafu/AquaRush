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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDateTime

/** [OutboxPartitionRules] 半区边界/命名/超期判定纯函数单测。 */
class OutboxPartitionRulesTest :
  FunSpec({
    test("halfStartOf:1-6 月归当年 H1,7-12 月归当年 H2,边界含端点") {
      OutboxPartitionRules.halfStartOf(LocalDateTime.of(2026, 1, 15, 10, 0)) shouldBe
        LocalDateTime.of(2026, 1, 1, 0, 0)
      OutboxPartitionRules.halfStartOf(LocalDateTime.of(2026, 6, 30, 23, 59)) shouldBe
        LocalDateTime.of(2026, 1, 1, 0, 0)
      OutboxPartitionRules.halfStartOf(LocalDateTime.of(2026, 7, 1, 0, 0)) shouldBe
        LocalDateTime.of(2026, 7, 1, 0, 0)
      OutboxPartitionRules.halfStartOf(LocalDateTime.of(2026, 12, 31, 23, 59)) shouldBe
        LocalDateTime.of(2026, 7, 1, 0, 0)
    }

    test("halfEndOf:等于下一半区起点(跨年 6 个月)") {
      OutboxPartitionRules.halfEndOf(LocalDateTime.of(2026, 1, 15, 0, 0)) shouldBe
        LocalDateTime.of(2026, 7, 1, 0, 0)
      OutboxPartitionRules.halfEndOf(LocalDateTime.of(2026, 7, 1, 0, 0)) shouldBe
        LocalDateTime.of(2027, 1, 1, 0, 0)
      OutboxPartitionRules.halfEndOf(LocalDateTime.of(2026, 12, 31, 23, 59)) shouldBe
        LocalDateTime.of(2027, 1, 1, 0, 0)
    }

    test("分区名/归档名:YYYYHn 后缀(小写,与 PG 标识符折叠一致),前缀替换") {
      val h2 = LocalDateTime.of(2026, 7, 1, 0, 0)
      val h1 = LocalDateTime.of(2026, 1, 1, 0, 0)
      OutboxPartitionRules.suffixOf(h1) shouldBe "2026h1"
      OutboxPartitionRules.suffixOf(h2) shouldBe "2026h2"
      OutboxPartitionRules.partitionName(h2) shouldBe "aqua_outbox_events_2026h2"
      OutboxPartitionRules.archiveName(h2) shouldBe "aqua_outbox_events_archive_2026h2"
      OutboxPartitionRules.archiveNameOf("aqua_outbox_events_2025h1") shouldBe "aqua_outbox_events_archive_2025h1"
      OutboxPartitionRules.archiveNameOf("aqua_api_logs_2026_01") shouldBe null
    }

    test("isExpired:上界 ≤ now - 保留天数 即超期(含等于)") {
      val now = LocalDateTime.of(2026, 10, 10, 4, 40)
      // cutoff = 2026-09-10 04:40
      OutboxPartitionRules.isExpired(LocalDateTime.of(2026, 9, 9, 0, 0), 30, now) shouldBe true
      OutboxPartitionRules.isExpired(LocalDateTime.of(2026, 9, 10, 4, 40), 30, now) shouldBe true
      OutboxPartitionRules.isExpired(LocalDateTime.of(2026, 9, 10, 4, 41), 30, now) shouldBe false
      OutboxPartitionRules.isExpired(LocalDateTime.of(2027, 1, 1, 0, 0), 30, now) shouldBe false
    }
  })
