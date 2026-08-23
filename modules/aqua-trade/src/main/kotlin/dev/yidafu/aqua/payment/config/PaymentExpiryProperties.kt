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

package dev.yidafu.aqua.payment.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 支付超时处理配置(R-ORD-04 支付超时优化):
 * MQ 延迟消息为主触发,定时扫表为兜底(仅 admin 服务运行,见 [dev.yidafu.aqua.common.annotation.AdminService])。
 */
@ConfigurationProperties(prefix = "aqua.payment.expiry")
data class PaymentExpiryProperties(
  /** 兜底扫描间隔(ms,默认 5 分钟;延迟消息正常时无需如此频繁) */
  var scanIntervalMs: Long = 300_000,
  /** 兜底扫描首次启动延迟(ms) */
  var scanInitialDelayMs: Long = 30_000,
  /** 兜底扫描每批行数(分批游标处理,避免全表查询,CLAUDE.md 规则 13) */
  var scanBatchSize: Int = 100,
)
