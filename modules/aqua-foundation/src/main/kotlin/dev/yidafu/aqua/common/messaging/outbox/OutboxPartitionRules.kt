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

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * outbox 半年分区规则(纯函数,可单测;spec/tech/design/outbox-messaging.md 修订)。
 *
 * 半区边界:H1 = [YYYY-01-01, YYYY-07-01),H2 = [YYYY-07-01, 次年-01-01);
 * 分区名 `aqua_outbox_events_YYYYHn`,归档名 `aqua_outbox_events_archive_YYYYHn`。
 * PG 未加引号标识符折叠为小写,故规则直接产出小写名(如 `aqua_outbox_events_2026h2`),
 * 与 PG 实际存储的关系名一致,免去大小写比对问题。
 * 超期口径与日志分区 [dev.yidafu.aqua.logging.partition.domain.LogPartitionRules.isExpired] 一致:
 * 分区上界 ≤ now - 保留天数 即可卸载。
 */
object OutboxPartitionRules {
  /** 主表名。 */
  const val TABLE_NAME = "aqua_outbox_events"

  /** 归档表前缀(卸载后改名 `{ARCHIVE_PREFIX}_{YYYYHn}`)。 */
  const val ARCHIVE_PREFIX = "aqua_outbox_events_archive"

  /** 分区滚动维护的事务级 advisory 锁 key(双服务实例串行化)。 */
  const val MAINTENANCE_LOCK_KEY = "aquarush:outbox-partition-maintenance"

  /** [date] 所在半区起点(当日 00:00:00)。 */
  fun halfStartOf(date: LocalDateTime): LocalDateTime {
    val d = date.toLocalDate()
    val start: LocalDate = if (d.monthValue <= 6) LocalDate.of(d.year, 1, 1) else LocalDate.of(d.year, 7, 1)
    return start.atStartOfDay()
  }

  /** [date] 所在半区终点(= 下一半区起点,开区间)。 */
  fun halfEndOf(date: LocalDateTime): LocalDateTime = halfStartOf(date).plusMonths(6)

  /** 半区起点对应的后缀,如 `2026h2`(入参须为 H1/H2 起点,即 1 月 1 日或 7 月 1 日)。 */
  fun suffixOf(halfStart: LocalDateTime): String = if (halfStart.monthValue <= 6) "${halfStart.year}h1" else "${halfStart.year}h2"

  /** 半区起点对应的分区名,如 `aqua_outbox_events_2026h2`。 */
  fun partitionName(halfStart: LocalDateTime): String = "${TABLE_NAME}_${suffixOf(halfStart)}"

  /** 半区起点对应的归档表名,如 `aqua_outbox_events_archive_2026h2`。 */
  fun archiveName(halfStart: LocalDateTime): String = "${ARCHIVE_PREFIX}_${suffixOf(halfStart)}"

  /** 由分区名推导归档名(前缀替换);非 outbox 分区名返回 null。 */
  fun archiveNameOf(partitionName: String): String? =
    partitionName
      .takeIf { it.startsWith("${TABLE_NAME}_") }
      ?.let { "${ARCHIVE_PREFIX}_${it.removePrefix("${TABLE_NAME}_")}" }

  /**
   * 分区上界是否已超保留期:上界 ≤ now - [retentionDays]。
   * @param upperBound 分区上界(开区间,即下一半区起点)
   * @param now 判定基准时间(注入便于测试)
   */
  fun isExpired(
    upperBound: LocalDateTime,
    retentionDays: Long,
    now: LocalDateTime,
  ): Boolean = !upperBound.isAfter(now.minusDays(retentionDays))
}
