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

package dev.yidafu.aqua.order.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 自动确认收货扫描配置(R-ORD-11 用户确认收货与自动签收):
 * 定时扫表为兜底安全网(仅 admin 服务运行,见 [dev.yidafu.aqua.common.annotation.AdminService]);
 * 自动确认天数取系统配置 auto_confirm_receipt_days(默认 7),此处仅配置扫描节奏与批量。
 */
@ConfigurationProperties(prefix = "aqua.order.receipt-confirm")
data class ReceiptAutoConfirmProperties(
  /** 扫描间隔(ms,默认 5 分钟) */
  var scanIntervalMs: Long = 300_000,
  /** 扫描首次启动延迟(ms) */
  var scanInitialDelayMs: Long = 60_000,
  /** 每批扫描行数(分批游标处理,避免全表查询,CLAUDE.md 规则 13) */
  var scanBatchSize: Int = 100,
)
