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
 * 对账任务状态(SKIPPED:未配置真实商户参数(假支付模式)跳过,R-PAY-04/F-ORD-031)
 */
enum class ReconciliationTaskStatus {
  PENDING, // 等待执行
  RUNNING, // 执行中
  SUCCESS, // 执行成功(差异数 > 0 仍为成功:对账完成 ≠ 无差异,R-PAY-04)
  FAILED, // 执行失败(异常;差异不导致失败)
  SKIPPED, // 跳过(未配置真实商户参数)
  ;

  companion object {
    fun fromValue(value: String): ReconciliationTaskStatus =
      when (value) {
        "PENDING" -> PENDING
        "RUNNING" -> RUNNING
        "SUCCESS" -> SUCCESS
        "FAILED" -> FAILED
        "SKIPPED" -> SKIPPED
        else -> throw IllegalArgumentException("Unknown ReconciliationTaskStatus value: '" + value + "'")
      }
  }
}
