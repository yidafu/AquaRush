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

package dev.yidafu.aqua.storage.repository

import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import dev.yidafu.aqua.storage.domain.enums.FileType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

/**
 * Custom repository interface for FileMetadata entity with QueryDSL implementations
 */
interface FileMetadataRepositoryCustom {
  /**
   * Find files by creation date range
   * @param startTime the start time
   * @param endTime the end time
   * @return list of files created within the date range
   */
  fun findByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<FileMetadata>

  /**
   * Count files by file type grouped by type
   * @return array of arrays containing file type and count
   */
  fun countByFileType(): Array<Array<Any>>

  /**
   * Get total file size for a specific owner
   * @param ownerId the owner ID (can be null for system files)
   * @return total file size in bytes
   */
  fun getTotalFileSizeByOwner(ownerId: Long?): Long

  /**
   * Find duplicate files based on checksum
   * @return list of files that have duplicates
   */
  fun findDuplicateFiles(): List<FileMetadata>

  /**
   * Find files by multiple conditions (file type, owner, public status)
   * @param fileType the file type (optional)
   * @param ownerId the owner ID (optional)
   * @param isPublic the public status (optional)
   * @return list of files matching the conditions
   */
  fun findByMultipleConditions(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?
  ): List<FileMetadata>

  /**
   * Find files by multiple conditions (file type, owner, public status) with pagination
   * @param fileType the file type (optional)
   * @param ownerId the owner ID (optional)
   * @param isPublic the public status (optional)
   * @param pageable pagination information
   * @return page of files matching the conditions
   */
  fun findByMultipleConditions(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?,
    pageable: Pageable
  ): Page<FileMetadata>
}