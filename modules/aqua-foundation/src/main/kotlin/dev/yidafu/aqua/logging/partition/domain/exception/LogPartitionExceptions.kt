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

package dev.yidafu.aqua.logging.partition.domain.exception

import dev.yidafu.aqua.common.graphql.validation.ValidationException

/**
 * 日志分区离线备份异常(F-SYS-006)
 * 文案经全局 ValidationExceptionHandler 透出到 GraphQL errors。
 */

/**
 * 分区不存在或非月度分区
 */
class LogPartitionNotFoundException(
  partitionName: String,
) : ValidationException("日志分区不存在:$partitionName")

/**
 * 分区不可备份(当前月 / DEFAULT 兜底 / 未超期)
 */
class LogPartitionNotBackupableException(
  reason: String,
) : ValidationException("该分区不可备份:$reason")

/**
 * 该分区已有进行中或成功的备份记录
 */
class LogPartitionAlreadyBackedUpException(
  partitionName: String,
) : ValidationException("该分区已备份或正在备份:$partitionName")

/**
 * 分区正被其他会话备份(advisory lock 获取失败)
 */
class LogPartitionBackupInProgressException(
  partitionName: String,
) : ValidationException("该分区正在备份中,请稍后重试:$partitionName")

/**
 * 当前环境不支持分区操作(非 PostgreSQL,如测试 H2)
 */
class LogPartitionUnsupportedEnvironmentException(
  message: String,
) : ValidationException(message)

/**
 * 备份执行失败(数据未丢失,分区仍在热表)
 */
class LogPartitionBackupFailedException(
  message: String,
  cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * 备份执行中断(内部异常,由 LogBackupService 捕获后统一记 FAILED 记录)
 * @param exportCompleted 导出文件是否已完整写出(决定失败后是否保留文件)
 */
class BackupExecutionException(
  val exportCompleted: Boolean,
  cause: Throwable,
) : RuntimeException(cause)
