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
package dev.yidafu.aqua.common.domain.model.enums

/**
 * 对账任务触发方式(F-ORD-031:调度自动 02:00/02:05 或管理端手动触发;详情页「触发方式」展示)
 */
enum class ReconciliationTriggerType {
  SCHEDULED, // 每日调度
  MANUAL, // 管理员手动触发
  ;

  companion object {
    fun fromValue(value: String): ReconciliationTriggerType =
      when (value) {
        "SCHEDULED" -> SCHEDULED
        "MANUAL" -> MANUAL
        else -> throw IllegalArgumentException("Unknown ReconciliationTriggerType value: '" + value + "'")
      }
  }
}

/**
 * 对账差异处置动作(R-PAY-04:解决 = 已核实无资金损失或已修复;忽略 = 跨日边界等可解释差异;备注必填)
 */
enum class DiscrepancyResolveAction {
  RESOLVE, // 解决
  IGNORE, // 忽略
}
