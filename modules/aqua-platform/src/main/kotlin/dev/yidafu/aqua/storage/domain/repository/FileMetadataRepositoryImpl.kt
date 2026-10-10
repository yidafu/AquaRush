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

import dev.yidafu.aqua.common.domain.model.FileMetadata
import dev.yidafu.aqua.common.domain.model.enums.FileType
import dev.yidafu.aqua.common.domain.table.FileMetadataTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 文件元数据仓储实现(JPA→Exposed 迁移 P3)。
 * FileMetadata 实体无 @SoftDelete 注解 → delete 为硬删;查询仍过滤 is_deleted=false
 * (DDL 有该列且恒为 false,过滤与否等价,统一走查询入口保持一致)。
 */
@Repository
class FileMetadataRepositoryImpl(
  private val database: Database,
) : FileMetadataRepository {
  private fun toModel(row: ResultRow): FileMetadata =
    FileMetadata().apply {
      id = row[FileMetadataTable.id]
      fileName = row[FileMetadataTable.fileName]
      storagePath = row[FileMetadataTable.storagePath]
      fileType = FileType.valueOf(row[FileMetadataTable.fileType])
      fileSize = row[FileMetadataTable.fileSize]
      mimeType = row[FileMetadataTable.mimeType]
      checksum = row[FileMetadataTable.checksum]
      isPublic = row[FileMetadataTable.isPublic]
      description = row[FileMetadataTable.description]
      extension = row[FileMetadataTable.extension]
      ownerId = row[FileMetadataTable.ownerId]
      createdAt = row[FileMetadataTable.createdAt]
      updatedAt = row[FileMetadataTable.updatedAt]
    }

  private fun toEntityCondition(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?,
  ): Op<Boolean> {
    var condition: Op<Boolean> = Op.TRUE
    fileType?.let { condition = condition and (FileMetadataTable.fileType eq it.name) }
    ownerId?.let { condition = condition and (FileMetadataTable.ownerId eq it) }
    isPublic?.let { condition = condition and (FileMetadataTable.isPublic eq it) }
    return condition
  }

  private fun paged(
    condition: Op<Boolean>,
    pageable: Pageable,
  ): Page<FileMetadata> =
    transaction(database) {
      val content =
        FileMetadataTable
          .selectAll()
          .andWhere { condition }
          .orderBy(FileMetadataTable.createdAt to SortOrder.DESC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      val total =
        FileMetadataTable
          .selectAll()
          .andWhere { condition }
          .count()
      PageImpl(content, pageable, total)
    }

  override fun findById(id: Long): Optional<FileMetadata> =
    Optional.ofNullable(
      transaction(database) {
        FileMetadataTable
          .selectAll()
          .andWhere { FileMetadataTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun delete(entity: FileMetadata) {
    // 软删豁免:file_metadata 的 is_deleted 恒为 false(见 PlatformTables),业务上就是物理删除
    val id =
      requireNotNull(entity.id) { "FileMetadata.id 不能为空(硬删必须携带主键)" }
    transaction(database) {
      FileMetadataTable.deleteWhere { FileMetadataTable.id eq id }
    }
  }

  override fun save(entity: FileMetadata): FileMetadata =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        FileMetadataTable
          .selectAllIncludingDeleted()
          .where { FileMetadataTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        FileMetadataTable.update({ FileMetadataTable.id eq id }) {
          it[fileName] = entity.fileName
          it[storagePath] = entity.storagePath
          it[fileType] = entity.fileType.name
          it[fileSize] = entity.fileSize
          it[mimeType] = entity.mimeType
          it[checksum] = entity.checksum
          it[isPublic] = entity.isPublic
          it[description] = entity.description
          it[extension] = entity.extension
          it[ownerId] = entity.ownerId
          it[updatedAt] = now
        }
      } else {
        FileMetadataTable.insert {
          it[FileMetadataTable.id] = id
          it[FileMetadataTable.fileName] = entity.fileName
          it[FileMetadataTable.storagePath] = entity.storagePath
          it[FileMetadataTable.fileType] = entity.fileType.name
          it[FileMetadataTable.fileSize] = entity.fileSize
          it[FileMetadataTable.mimeType] = entity.mimeType
          it[FileMetadataTable.checksum] = entity.checksum
          it[FileMetadataTable.isPublic] = entity.isPublic
          it[FileMetadataTable.description] = entity.description
          it[FileMetadataTable.extension] = entity.extension
          it[FileMetadataTable.ownerId] = entity.ownerId
          it[FileMetadataTable.createdAt] = entity.createdAt
          it[FileMetadataTable.updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findAll(pageable: Pageable): Page<FileMetadata> = paged(Op.TRUE, pageable)

  override fun findByStoragePath(storagePath: String): Optional<FileMetadata> =
    Optional.ofNullable(
      transaction(database) {
        FileMetadataTable
          .selectAll()
          .andWhere { FileMetadataTable.storagePath eq storagePath }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByChecksum(checksum: String): Optional<FileMetadata> =
    Optional.ofNullable(
      transaction(database) {
        FileMetadataTable
          .selectAll()
          .andWhere { FileMetadataTable.checksum eq checksum }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByFileType(fileType: FileType): List<FileMetadata> =
    transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere { FileMetadataTable.fileType eq fileType.name }
        .orderBy(FileMetadataTable.createdAt to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun findByFileType(
    fileType: FileType,
    pageable: Pageable,
  ): Page<FileMetadata> = paged(Op.TRUE and (FileMetadataTable.fileType eq fileType.name), pageable)

  override fun findByOwnerId(ownerId: Long?): List<FileMetadata> =
    transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere {
          if (ownerId ==
            null
          ) {
            FileMetadataTable.ownerId.isNull()
          } else {
            FileMetadataTable.ownerId eq ownerId
          }
        }.orderBy(
          FileMetadataTable.createdAt to SortOrder.DESC,
        ).toList()
    }.map(::toModel)

  override fun findByOwnerId(
    ownerId: Long?,
    pageable: Pageable,
  ): Page<FileMetadata> =
    paged(
      if (ownerId == null) {
        Op.TRUE and FileMetadataTable.ownerId.isNull()
      } else {
        Op.TRUE and (FileMetadataTable.ownerId eq ownerId)
      },
      pageable,
    )

  override fun findByIsPublic(isPublic: Boolean): List<FileMetadata> =
    transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere { FileMetadataTable.isPublic eq isPublic }
        .orderBy(FileMetadataTable.createdAt to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun findByFileNameContainingIgnoreCase(fileName: String): List<FileMetadata> =
    transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere { FileMetadataTable.fileName.lowerCase() like "%${fileName.lowercase()}%" }
        .orderBy(FileMetadataTable.createdAt to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun findByFileNameContainingIgnoreCase(
    fileName: String,
    pageable: Pageable,
  ): Page<FileMetadata> = paged(Op.TRUE and (FileMetadataTable.fileName.lowerCase() like "%${fileName.lowercase()}%"), pageable)

  override fun findByExtension(extension: String): List<FileMetadata> =
    transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere { FileMetadataTable.extension eq extension }
        .orderBy(FileMetadataTable.createdAt to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun findByCreatedAtAfter(createdAt: LocalDateTime): List<FileMetadata> =
    transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere { FileMetadataTable.createdAt greater createdAt }
        .orderBy(FileMetadataTable.createdAt to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun findByCreatedAtBetween(
    startTime: LocalDateTime,
    endTime: LocalDateTime,
  ): List<FileMetadata> =
    transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere { (FileMetadataTable.createdAt greaterEq startTime) and (FileMetadataTable.createdAt lessEq endTime) }
        .orderBy(
          FileMetadataTable.createdAt to SortOrder.DESC,
        ).toList()
    }.map(::toModel)

  override fun countByFileType(): Array<Array<Any>> =
    transaction(database) {
      FileMetadataTable
        .select(FileMetadataTable.fileType, FileMetadataTable.id.count())
        .groupBy(FileMetadataTable.fileType)
        .toList()
    }.map { row ->
      // Array<Array<Any>> 契约与 JPA 版一致:[fileType(枚举或字符串), count(Long)]
      @Suppress("UNCHECKED_CAST")
      arrayOf<Any>(FileType.valueOf(row[FileMetadataTable.fileType]), row[FileMetadataTable.id.count()])
    }.toTypedArray()

  override fun getTotalFileSizeByOwner(ownerId: Long?): Long =
    transaction(database) {
      FileMetadataTable
        .select(FileMetadataTable.fileSize.sum())
        .andWhere { if (ownerId == null) FileMetadataTable.ownerId.isNull() else FileMetadataTable.ownerId eq ownerId }
        .first()
    }.let { row ->
      (row[FileMetadataTable.fileSize.sum()] as? Number)?.toLong() ?: 0L
    }

  override fun findDuplicateFiles(): List<FileMetadata> {
    val duplicateChecksums: List<String> =
      transaction(database) {
        FileMetadataTable
          .select(FileMetadataTable.checksum)
          .groupBy(FileMetadataTable.checksum)
          .having { FileMetadataTable.id.count() greater 1L }
          .toList()
      }.map { it[FileMetadataTable.checksum] }

    return transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere { FileMetadataTable.checksum inList duplicateChecksums }
        .orderBy(FileMetadataTable.checksum to SortOrder.ASC)
        .toList()
    }.map(::toModel)
  }

  override fun findByMultipleConditions(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?,
  ): List<FileMetadata> =
    transaction(database) {
      FileMetadataTable
        .selectAll()
        .andWhere { toEntityCondition(fileType, ownerId, isPublic) }
        .orderBy(FileMetadataTable.createdAt to SortOrder.DESC)
        .toList()
    }.map(::toModel)

  override fun findByMultipleConditions(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?,
    pageable: Pageable,
  ): Page<FileMetadata> = paged(toEntityCondition(fileType, ownerId, isPublic), pageable)
}
