/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
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

package dev.yidafu.aqua.logging.partition.domain.model.enums

import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * 日志分区枚举的字符串解析入口 `fromValue` 契约:
 * - 每个枚举值 roundtrip(`fromValue(name)` 必须还原自身) —— 覆盖全部 when 分支;
 * - 未知值 fail-fast 抛 [IllegalArgumentException](与原 valueOf 行为一致,不静默兜底)。
 */
class EnumFromValueTest :
  FunSpec({
    test("fromValue roundtrip: 每个枚举值可无损解析") {
      LogBackupStatus.entries.forEach { LogBackupStatus.fromValue(it.name) shouldBe it }
      LogTableType.entries.forEach { LogTableType.fromValue(it.name) shouldBe it }
    }

    test("fromValue 未知值抛 IllegalArgumentException(fail-fast)") {
      shouldThrow<IllegalArgumentException> { LogBackupStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { LogTableType.fromValue("__NO_SUCH_VALUE__") }
    }
  })
