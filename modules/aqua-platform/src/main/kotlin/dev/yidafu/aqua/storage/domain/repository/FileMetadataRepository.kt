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

package dev.yidafu.aqua.storage.domain.repository

import dev.yidafu.aqua.storage.domain.model.FileMetadata
import dev.yidafu.aqua.storage.domain.model.enums.FileType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

/**
 * 文件元数据仓库接口
 */
@Repository
interface FileMetadataRepository : FileMetadataRepositoryCustom {
  fun findById(id: Long): Optional<FileMetadata>

  /** 实体无 @SoftDelete 注解 → 硬删(行 DELETE),与 JPA 版行为一致。 */
  fun delete(entity: FileMetadata)

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: FileMetadata): FileMetadata

  /** 存量接口遗留的无界查询(违反规则 10,由调用方 StorageService 内部使用;不新增调用)。 */
  fun findAll(): List<FileMetadata>

  fun findAll(pageable: Pageable): Page<FileMetadata>

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
  fun findByFileType(
    fileType: FileType,
    pageable: Pageable,
  ): Page<FileMetadata>

  /**
   * 根据所有者ID查找文件
   */
  fun findByOwnerId(ownerId: Long?): List<FileMetadata>

  /**
   * 根据所有者ID分页查找文件
   */
  fun findByOwnerId(
    ownerId: Long?,
    pageable: Pageable,
  ): Page<FileMetadata>

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
  fun findByFileNameContainingIgnoreCase(
    fileName: String,
    pageable: Pageable,
  ): Page<FileMetadata>

  /**
   * 根据文件扩展名查找文件
   */
  fun findByExtension(extension: String): List<FileMetadata>

  /**
   * 查找指定时间之后创建的文件
   */
  fun findByCreatedAtAfter(createdAt: LocalDateTime): List<FileMetadata>
}
