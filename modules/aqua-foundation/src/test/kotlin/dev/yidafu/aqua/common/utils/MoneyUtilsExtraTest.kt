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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 补充 MoneyUtilsTest 未覆盖的边界分支。
 */
class MoneyUtilsExtraTest {
  @Test
  fun `canConvertToCents should return false when overflow occurs`() {
    assertFalse(MoneyUtils.canConvertToCents(BigDecimal("92233720368547758.08")))
  }

  @Test
  fun `canConvertToCents should return false when scale exceeds limit`() {
    assertFalse(MoneyUtils.canConvertToCents(BigDecimal("1.001")))
  }

  @Test
  fun `subtractCents should throw on overflow`() {
    assertThrows<ArithmeticException> {
      MoneyUtils.subtractCents(Long.MAX_VALUE, -1L)
    }
  }

  @Test
  fun `subtractCents should allow equal values`() {
    assertEquals(0L, MoneyUtils.subtractCents(100L, 100L))
  }

  @Test
  fun `calculatePercentage with zero cents should return zero`() {
    assertEquals(0L, MoneyUtils.calculatePercentage(0L, BigDecimal("50.0")))
  }

  @Test
  fun `multiplyCents with zero cents should return zero`() {
    assertEquals(0L, MoneyUtils.multiplyCents(0L, BigDecimal("3.5")))
  }
}
