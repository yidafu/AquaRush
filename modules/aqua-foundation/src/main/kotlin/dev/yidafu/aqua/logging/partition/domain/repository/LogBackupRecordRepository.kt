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

package dev.yidafu.aqua.logging.partition.domain.repository

import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 日志离线备份记录 Repository(F-SYS-006)
 */
@Repository
interface LogBackupRecordRepository : JpaRepository<LogBackupRecordModel, Long> {
  /**
   * 按日志表分页查询备份记录(恒 startedAt 倒序)
   */
  fun findByLogTableOrderByStartedAtDesc(
    logTable: LogTableType,
    pageable: Pageable,
  ): Page<LogBackupRecordModel>

  /**
   * 查询某分区是否存在进行中/成功的备份记录(用于可备份校验与并发防重)
   */
  fun existsByPartitionNameAndStatusIn(
    partitionName: String,
    statuses: List<LogBackupStatus>,
  ): Boolean

  /**
   * 查询某分区最近一次备份记录(幂等/去重判断)
   */
  fun findFirstByPartitionNameOrderByStartedAtDesc(partitionName: String): LogBackupRecordModel?
}
