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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 文件元数据仓库接口
 */
@Repository
interface FileMetadataRepository : JpaRepository<FileMetadata, Long> {

    /**
     * 根据存储路径查找文件
     */
    fun findByStoragePath(storagePath: String): Optional<FileMetadata>

    /**
     * 根据校验和查找文件
     */
    fun findByChecksum(checksum: String): Optional<FileMetadata>

    /**
     * 根据文件类型查找文件
     */
    fun findByFileType(fileType: FileType): List<FileMetadata>

    /**
     * 根据文件类型分页查找文件
     */
    fun findByFileType(fileType: FileType, pageable: Pageable): Page<FileMetadata>

    /**
     * 根据所有者ID查找文件
     */
    fun findByOwnerId(ownerId: Long?): List<FileMetadata>

    /**
     * 根据所有者ID分页查找文件
     */
    fun findByOwnerId(ownerId: Long?, pageable: Pageable): Page<FileMetadata>

    /**
     * 查找公开文件
     */
    fun findByIsPublic(isPublic: Boolean): List<FileMetadata>

    /**
     * 根据文件名模糊搜索
     */
    fun findByFileNameContainingIgnoreCase(fileName: String): List<FileMetadata>

    /**
     * 根据文件名模糊搜索（分页）
     */
    fun findByFileNameContainingIgnoreCase(fileName: String, pageable: Pageable): Page<FileMetadata>

    /**
     * 根据文件扩展名查找文件
     */
    fun findByExtension(extension: String): List<FileMetadata>

    /**
     * 查找指定时间之后创建的文件
     */
    fun findByCreatedAtAfter(createdAt: LocalDateTime): List<FileMetadata>

    /**
     * 查找指定时间范围内创建的文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.createdAt BETWEEN :startTime AND :endTime")
    fun findByCreatedAtBetween(
        @Param("startTime") startTime: LocalDateTime,
        @Param("endTime") endTime: LocalDateTime
    ): List<FileMetadata>

    /**
     * 统计各类型文件的数量
     */
    @Query("SELECT f.fileType, COUNT(f) FROM FileMetadata f GROUP BY f.fileType")
    fun countByFileType(): Array<Array<Any>>

    /**
     * 统计指定所有者的文件总大小
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.ownerId = :ownerId")
    fun getTotalFileSizeByOwner(@Param("ownerId") ownerId: Long?): Long

    /**
     * 查找重复文件（根据校验和）
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.checksum IN " +
           "(SELECT f2.checksum FROM FileMetadata f2 GROUP BY f2.checksum HAVING COUNT(f2.checksum) > 1) " +
           "ORDER BY f.checksum")
    fun findDuplicateFiles(): List<FileMetadata>

    /**
     * 复合查询：根据文件类型、所有者和公开状态查找文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE " +
           "(:fileType IS NULL OR f.fileType = :fileType) AND " +
           "(:ownerId IS NULL OR f.ownerId = :ownerId) AND " +
           "(:isPublic IS NULL OR f.isPublic = :isPublic)")
    fun findByMultipleConditions(
        @Param("fileType") fileType: FileType?,
        @Param("ownerId") ownerId: Long?,
        @Param("isPublic") isPublic: Boolean?
    ): List<FileMetadata>

    /**
     * 复合查询：根据文件类型、所有者和公开状态查找文件（分页）
     */
    @Query("SELECT f FROM FileMetadata f WHERE " +
           "(:fileType IS NULL OR f.fileType = :fileType) AND " +
           "(:ownerId IS NULL OR f.ownerId = :ownerId) AND " +
           "(:isPublic IS NULL OR f.isPublic = :isPublic)")
    fun findByMultipleConditions(
        @Param("fileType") fileType: FileType?,
        @Param("ownerId") ownerId: Long?,
        @Param("isPublic") isPublic: Boolean?,
        pageable: Pageable
    ): Page<FileMetadata>
}
