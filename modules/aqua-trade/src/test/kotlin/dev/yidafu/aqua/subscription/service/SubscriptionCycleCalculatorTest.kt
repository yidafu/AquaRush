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

package dev.yidafu.aqua.subscription.service

import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 周期订购配送日期计算(R-ORD-13):每周/每两周奇偶周锚点/每月月末钳制/星期串解析。
 * 固定锚点日期避免依赖"今天"。
 */
class SubscriptionCycleCalculatorTest :
  FunSpec({
    // 2026-01-05 是周一
    val monday = LocalDate.of(2026, 1, 5)

    test("weekly first run on start date when weekday matches") {
      // 生效日就是周三
      val first =
        SubscriptionCycleCalculator.firstRun(
          startDate = LocalDate.of(2026, 1, 7),
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = listOf(DayOfWeek.WEDNESDAY.value),
          monthDay = null,
        )
      first shouldBe LocalDate.of(2026, 1, 7)
    }

    test("weekly first run skips forward to matching weekday") {
      val first =
        SubscriptionCycleCalculator.firstRun(
          startDate = monday, // 周一
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = listOf(DayOfWeek.FRIDAY.value), // 周五
          monthDay = null,
        )
      first shouldBe LocalDate.of(2026, 1, 9)
    }

    test("weekly next run picks earliest matching weekday after due") {
      val next =
        SubscriptionCycleCalculator.nextRun(
          after = monday, // 周一
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = listOf(DayOfWeek.MONDAY.value, DayOfWeek.THURSDAY.value),
          monthDay = null,
          startDate = monday,
        )
      next shouldBe LocalDate.of(2026, 1, 8) // 周四
    }

    test("weekly next run wraps to next week") {
      val next =
        SubscriptionCycleCalculator.nextRun(
          after = LocalDate.of(2026, 1, 9), // 周五
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = listOf(DayOfWeek.MONDAY.value),
          monthDay = null,
          startDate = monday,
        )
      next shouldBe LocalDate.of(2026, 1, 12) // 下周一
    }

    test("biweekly alternates weeks anchored on start week") {
      // 生效周一 2026-01-05,目标周五
      val friday = DayOfWeek.FRIDAY.value
      val first =
        SubscriptionCycleCalculator.firstRun(
          startDate = monday,
          cycleType = SubscriptionCycleType.BIWEEKLY,
          weekdays = listOf(friday),
          monthDay = null,
        )
      first shouldBe LocalDate.of(2026, 1, 9) // 生效周的周五
      // 下一期:跳过下一周(奇偶周不同),取再下一周的周五
      val second =
        SubscriptionCycleCalculator.nextRun(
          after = first,
          cycleType = SubscriptionCycleType.BIWEEKLY,
          weekdays = listOf(friday),
          monthDay = null,
          startDate = monday,
        )
      second shouldBe LocalDate.of(2026, 1, 23)
      val third =
        SubscriptionCycleCalculator.nextRun(
          after = second,
          cycleType = SubscriptionCycleType.BIWEEKLY,
          weekdays = listOf(friday),
          monthDay = null,
          startDate = monday,
        )
      third shouldBe LocalDate.of(2026, 2, 6)
    }

    test("monthly next run in same month when day not yet passed") {
      val next =
        SubscriptionCycleCalculator.nextRun(
          after = LocalDate.of(2026, 1, 5),
          cycleType = SubscriptionCycleType.MONTHLY,
          weekdays = null,
          monthDay = 10,
          startDate = LocalDate.of(2026, 1, 1),
        )
      next shouldBe LocalDate.of(2026, 1, 10)
    }

    test("monthly next run rolls to next month") {
      val next =
        SubscriptionCycleCalculator.nextRun(
          after = LocalDate.of(2026, 1, 10),
          cycleType = SubscriptionCycleType.MONTHLY,
          weekdays = null,
          monthDay = 10,
          startDate = LocalDate.of(2026, 1, 1),
        )
      next shouldBe LocalDate.of(2026, 2, 10)
    }

    test("monthly clamps day 31 to short month end") {
      val next =
        SubscriptionCycleCalculator.nextRun(
          after = LocalDate.of(2026, 1, 31),
          cycleType = SubscriptionCycleType.MONTHLY,
          weekdays = null,
          monthDay = 31,
          startDate = LocalDate.of(2026, 1, 1),
        )
      // 2026-02 无 31 日 → 2 月最后一天
      next shouldBe LocalDate.of(2026, 2, 28)
    }

    test("monthly first run clamps within start month") {
      val first =
        SubscriptionCycleCalculator.firstRun(
          startDate = LocalDate.of(2026, 3, 15),
          cycleType = SubscriptionCycleType.MONTHLY,
          weekdays = null,
          monthDay = 10, // 3 月 10 日已过 → 下月
        )
      first shouldBe LocalDate.of(2026, 4, 10)
    }

    test("parse and format weekdays round trip") {
      SubscriptionCycleCalculator.parseWeekdays("1,3,5") shouldBe listOf(1, 3, 5)
      SubscriptionCycleCalculator.parseWeekdays(null) shouldBe emptyList()
      SubscriptionCycleCalculator.parseWeekdays("") shouldBe emptyList()
      SubscriptionCycleCalculator.parseWeekdays("8,x") shouldBe emptyList()
      SubscriptionCycleCalculator.formatWeekdays(listOf(5, 1, 3)) shouldBe "1,3,5"
    }

    test("missing cycle params rejected") {
      shouldThrow<IllegalArgumentException> {
        SubscriptionCycleCalculator.firstRun(monday, SubscriptionCycleType.WEEKLY, null, null)
      }
      shouldThrow<IllegalArgumentException> {
        SubscriptionCycleCalculator.firstRun(monday, SubscriptionCycleType.MONTHLY, null, 0)
      }
    }
  })
