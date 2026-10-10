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

package dev.yidafu.aqua.common.graphql

import dev.yidafu.aqua.common.graphql.generated.BizLogLevel
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * GraphQL 生成枚举伴生扩展 `fromValue` 契约(codegen 产物不可改,以扩展提供解析入口):
 * - 每个枚举值 roundtrip —— 覆盖全部 when 分支;
 * - 未知值 fail-fast 抛 [IllegalArgumentException]。
 */
class GraphQLEnumParsingTest :
  FunSpec({
    test("fromValue roundtrip: 每个枚举值可无损解析") {
      RefundStatus.entries.forEach { RefundStatus.fromValue(it.name) shouldBe it }
      RefundType.entries.forEach { RefundType.fromValue(it.name) shouldBe it }
      UserStatus.entries.forEach { UserStatus.fromValue(it.name) shouldBe it }
      UserRole.entries.forEach { UserRole.fromValue(it.name) shouldBe it }
      BizLogLevel.entries.forEach { BizLogLevel.fromValue(it.name) shouldBe it }
    }

    test("fromValue 未知值抛 IllegalArgumentException(fail-fast)") {
      shouldThrow<IllegalArgumentException> { RefundStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { RefundType.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { UserStatus.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { UserRole.fromValue("__NO_SUCH_VALUE__") }
      shouldThrow<IllegalArgumentException> { BizLogLevel.fromValue("__NO_SUCH_VALUE__") }
    }
  })
