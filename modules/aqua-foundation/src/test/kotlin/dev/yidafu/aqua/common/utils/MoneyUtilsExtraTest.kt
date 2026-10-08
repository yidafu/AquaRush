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

package dev.yidafu.aqua.common.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

/**
 * 补充 MoneyUtilsTest 未覆盖的边界分支。
 */
class MoneyUtilsExtraTest :
  FunSpec({

    test("canConvertToCents should return false when overflow occurs") {
      MoneyUtils.canConvertToCents(BigDecimal("92233720368547758.08")) shouldBe false
    }
    test("canConvertToCents should return false when scale exceeds limit") {
      MoneyUtils.canConvertToCents(BigDecimal("1.001")) shouldBe false
    }
    test("subtractCents should throw on overflow") {
      shouldThrow<ArithmeticException> {
        MoneyUtils.subtractCents(Long.MAX_VALUE, -1L)
      }
    }
    test("subtractCents should allow equal values") {
      MoneyUtils.subtractCents(100L, 100L) shouldBe 0L
    }
    test("calculatePercentage with zero cents should return zero") {
      MoneyUtils.calculatePercentage(0L, BigDecimal("50.0")) shouldBe 0L
    }
    test("multiplyCents with zero cents should return zero") {
      MoneyUtils.multiplyCents(0L, BigDecimal("3.5")) shouldBe 0L
    }
  })
