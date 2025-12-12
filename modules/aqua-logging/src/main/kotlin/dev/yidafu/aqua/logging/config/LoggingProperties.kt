/*
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

package dev.yidafu.aqua.logging.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 日志模块配置属性
 */
@ConfigurationProperties(prefix = "aqua.logging")
data class LoggingProperties(
  val structured: StructuredProperties = StructuredProperties(),
  val correlation: CorrelationProperties = CorrelationProperties(),
  val performance: PerformanceProperties = PerformanceProperties(),
  val audit: AuditProperties = AuditProperties(),
  val async: AsyncProperties = AsyncProperties(),
  val userAction: UserActionProperties = UserActionProperties(),
  val enabled: Boolean = true,
)

/**
 * 结构化日志配置
 */
data class StructuredProperties(
  val enabled: Boolean = true,
  val format: LogFormat = LogFormat.JSON,
  val includeStackTrace: Boolean = false,
  val fieldExclusions: List<String> = emptyList(),
  val maxLength: Int = 8192,
)

/**
 * 关联ID配置
 */
data class CorrelationProperties(
  val enabled: Boolean = true,
  val headerName: String = "X-Correlation-ID",
  val generateIfMissing: Boolean = true,
  val includeInResponse: Boolean = true,
)

/**
 * 性能监控配置
 */
data class PerformanceProperties(
  val enabled: Boolean = true,
  val slowThresholdMs: Long = 1000,
  val verySlowThresholdMs: Long = 5000,
  val logSlowQueries: Boolean = true,
  val trackMethodExecution: Boolean = true,
)

/**
 * 审计日志配置
 */
data class AuditProperties(
  val enabled: Boolean = true,
  val logSecurityEvents: Boolean = true,
  val logBusinessOperations: Boolean = true,
  val logDataAccess: Boolean = false,
  val retentionDays: Int = 90,
)

/**
 * 异步日志配置
 */
data class AsyncProperties(
  val enabled: Boolean = true,
  val queueSize: Int = 256,
  val discardingThreshold: Int = 0,
  val includeCallerData: Boolean = true,
  val workerThreads: Int = 1,
)

/**
 * 用户操作日志配置
 */
data class UserActionProperties(
  val enabled: Boolean = true,
  val logPageViews: Boolean = true,
  val logClicks: Boolean = true,
  val logInputs: Boolean = false, // 默认关闭输入日志以减少日志量
  val logBackendOps: Boolean = true,
  val logScrolls: Boolean = false, // 默认关闭滚动日志以减少日志量
  val logFormSubmits: Boolean = true,
  val logFileOperations: Boolean = true,
  val logSearches: Boolean = true,
  val logShares: Boolean = true,
  val retentionDays: Int = 30, // 保留天数
  val batchSize: Int = 100, // 批量处理大小
  val flushInterval: Long = 5000, // 刷新间隔（毫秒）
  val sanitizeSensitiveData: Boolean = true, // 是否对敏感数据进行脱敏
  val maxInputLength: Int = 200, // 输入内容最大记录长度
  val excludedElements: List<String> = emptyList(), // 排除的元素ID列表
  val includedOperations: List<String> = emptyList(), // 包含的操作类型列表，空表示全部
  val asyncProcessing: Boolean = true, // 是否异步处理
  val enableHotColdSeparation: Boolean = true, // 是否启用冷热数据分离
  val hotDataRetentionDays: Int = 180, // 热数据保留天数
)

/**
 * 日志格式枚举
 */
enum class LogFormat {
  JSON,
  PLAIN,
}
