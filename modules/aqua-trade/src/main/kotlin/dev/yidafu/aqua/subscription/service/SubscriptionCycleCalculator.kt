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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * 周期订购配送日期计算(R-ORD-13 纯函数,无状态,便于单测):
 * - WEEKLY:每周在指定星期(ISO 1=周一..7=周日,可多选)配送;
 * - BIWEEKLY:每两周指定星期,周次奇偶以生效日所在周(MONDAY 对齐)为锚点;
 * - MONTHLY:每月指定日,月末日(如 31 日在小月)取当月最后一天。
 */
object SubscriptionCycleCalculator {
  /**
   * 首次执行日期:生效日(含)之后第一个符合周期规则的日期。
   * 调用方负责校验周期参数(weekdays/monthDay 范围)。
   */
  fun firstRun(
    startDate: LocalDate,
    cycleType: SubscriptionCycleType,
    weekdays: List<Int>?,
    monthDay: Int?,
  ): LocalDate =
    when (cycleType) {
      SubscriptionCycleType.WEEKLY ->
        firstWeeklyOnOrAfter(startDate, requireWeekdays(weekdays))
      SubscriptionCycleType.BIWEEKLY ->
        generateSequence(firstWeeklyOnOrAfter(startDate, requireWeekdays(weekdays))) { it.plusWeeks(1) }
          .first { sameBiweek(it, startDate) }
      SubscriptionCycleType.MONTHLY ->
        firstMonthlyOnOrAfter(startDate, requireMonthDay(monthDay))
    }

  /**
   * 下一次执行日期(严格晚于 [after]):
   * 计划成功执行一期后从到期日推进;暂停恢复等场景由调用方以今天为锚点重算 [firstRun]。
   */
  fun nextRun(
    after: LocalDate,
    cycleType: SubscriptionCycleType,
    weekdays: List<Int>?,
    monthDay: Int?,
    startDate: LocalDate,
  ): LocalDate =
    when (cycleType) {
      SubscriptionCycleType.WEEKLY -> nextWeeklyAfter(after, requireWeekdays(weekdays))
      SubscriptionCycleType.BIWEEKLY ->
        generateSequence(nextWeeklyAfter(after, requireWeekdays(weekdays))) { it.plusWeeks(1) }
          .first { sameBiweek(it, startDate) }
      SubscriptionCycleType.MONTHLY -> nextMonthlyAfter(after, requireMonthDay(monthDay))
    }

  /** 解析逗号分隔星期串("1,3,5")为整数列表(空串 → 空列表) */
  fun parseWeekdays(raw: String?): List<Int> =
    raw
      ?.split(",")
      ?.mapNotNull { it.trim().toIntOrNull() }
      ?.filter { it in 1..7 }
      ?.distinct()
      ?: emptyList()

  /** 星期整数列表 → 存储串(升序逗号分隔) */
  fun formatWeekdays(weekdays: List<Int>): String = weekdays.distinct().sorted().joinToString(",")

  private fun requireWeekdays(weekdays: List<Int>?): Set<Int> =
    weekdays
      ?.filter { it in 1..7 }
      ?.toSet()
      ?.takeIf { it.isNotEmpty() }
      ?: throw IllegalArgumentException("每周/每两周周期必须指定配送星期")

  private fun requireMonthDay(monthDay: Int?): Int =
    monthDay?.takeIf { it in 1..31 }
      ?: throw IllegalArgumentException("每月周期必须指定配送日期(1-31)")

  private fun firstWeeklyOnOrAfter(
    from: LocalDate,
    weekdays: Set<Int>,
  ): LocalDate {
    var date = from
    // 最多回看一年:星期集合已校验非空,366 天内必命中
    repeat(366) {
      if (date.dayOfWeek.value in weekdays) return date
      date = date.plusDays(1)
    }
    throw IllegalStateException("无法计算每周配送日期")
  }

  private fun nextWeeklyAfter(
    after: LocalDate,
    weekdays: Set<Int>,
  ): LocalDate = firstWeeklyOnOrAfter(after.plusDays(1), weekdays)

  /** 同一"两周奇偶周":以各自所在周的周一为锚点,周差为偶数周 */
  private fun sameBiweek(
    a: LocalDate,
    b: LocalDate,
  ): Boolean {
    val weekA = a.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekB = b.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return ChronoUnit.WEEKS.between(weekA, weekB) % 2 == 0L
  }

  private fun firstMonthlyOnOrAfter(
    from: LocalDate,
    monthDay: Int,
  ): LocalDate {
    val candidate = monthlyCandidate(from, monthDay)
    return if (candidate.isBefore(from)) monthlyCandidate(from.plusMonths(1), monthDay) else candidate
  }

  private fun nextMonthlyAfter(
    after: LocalDate,
    monthDay: Int,
  ): LocalDate {
    val candidate = monthlyCandidate(after, monthDay)
    return if (!candidate.isAfter(after)) monthlyCandidate(after.plusMonths(1), monthDay) else candidate
  }

  /** 月内候选日:指定日超出当月天数时取当月最后一天(31 日 → 2 月 28/29 日) */
  private fun monthlyCandidate(
    month: LocalDate,
    monthDay: Int,
  ): LocalDate {
    val yearMonth = YearMonth.from(month)
    return yearMonth.atDay(minOf(monthDay, yearMonth.lengthOfMonth()))
  }
}
