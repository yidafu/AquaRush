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

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.Tuple
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.storage.domain.entity.FileMetadata
import dev.yidafu.aqua.storage.domain.entity.QFileMetadata
import dev.yidafu.aqua.storage.domain.enums.FileType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Custom repository implementation for FileMetadata entity using QueryDSL
 */
@Repository
class FileMetadataRepositoryImpl : FileMetadataRepositoryCustom {

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  private val qFileMetadata = QFileMetadata.fileMetadata

  override fun findByCreatedAtBetween(startTime: LocalDateTime, endTime: LocalDateTime): List<FileMetadata> {
    return queryFactory.selectFrom(qFileMetadata)
      .where(qFileMetadata.createdAt.between(startTime, endTime))
      .orderBy(qFileMetadata.createdAt.desc())
      .fetch()
  }

  override fun countByFileType(): Array<Array<Any>> {
    val results: List<Tuple> = queryFactory
      .select(qFileMetadata.fileType, qFileMetadata.count())
      .from(qFileMetadata)
      .groupBy(qFileMetadata.fileType)
      .fetch()

    return results.map { tuple ->
      arrayOf<Any>(
        tuple.get(qFileMetadata.fileType) ?: 0,
        tuple.get(qFileMetadata.count()) ?: 0L
      )
    }.toTypedArray()
  }

  override fun getTotalFileSizeByOwner(ownerId: Long?): Long {
    return if (ownerId == null) {
      queryFactory.query()
        .from(qFileMetadata)
        .where(qFileMetadata.ownerId.isNull)
        .select(qFileMetadata.fileSize.sum().coalesce(0L))
        .fetchOne() ?: 0L
    } else {
      queryFactory.query()
        .from(qFileMetadata)
        .where(qFileMetadata.ownerId.eq(ownerId))
        .select(qFileMetadata.fileSize.sum().coalesce(0L))
        .fetchOne() ?: 0L
    }
  }

  override fun findDuplicateFiles(): List<FileMetadata> {
    // Find checksums that appear more than once
    val duplicateChecksums = queryFactory
      .select(qFileMetadata.checksum)
      .from(qFileMetadata)
      .groupBy(qFileMetadata.checksum)
      .having(qFileMetadata.count().gt(1L))
      .fetch()

    // Find files with duplicate checksums
    return queryFactory.selectFrom(qFileMetadata)
      .where(qFileMetadata.checksum.`in`(duplicateChecksums))
      .orderBy(qFileMetadata.checksum.asc())
      .fetch()
  }

  override fun findByMultipleConditions(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?
  ): List<FileMetadata> {
    val builder = BooleanBuilder()

    fileType?.let { builder.and(qFileMetadata.fileType.eq(it)) }
    ownerId?.let { builder.and(qFileMetadata.ownerId.eq(it)) }
    isPublic?.let { builder.and(qFileMetadata.isPublic.eq(it)) }

    return queryFactory.selectFrom(qFileMetadata)
      .where(builder)
      .orderBy(qFileMetadata.createdAt.desc())
      .fetch()
  }

  override fun findByMultipleConditions(
    fileType: FileType?,
    ownerId: Long?,
    isPublic: Boolean?,
    pageable: Pageable
  ): Page<FileMetadata> {
    val builder = BooleanBuilder()

    fileType?.let { builder.and(qFileMetadata.fileType.eq(it)) }
    ownerId?.let { builder.and(qFileMetadata.ownerId.eq(it)) }
    isPublic?.let { builder.and(qFileMetadata.isPublic.eq(it)) }

    // Count query
    val totalCount = queryFactory.query()
      .from(qFileMetadata)
      .where(builder)
      .fetchCount()

    // Main query with pagination
    val results = queryFactory.selectFrom(qFileMetadata)
      .where(builder)
      .orderBy(qFileMetadata.createdAt.desc())
      .offset(pageable.offset)
      .limit(pageable.pageSize.toLong())
      .fetch()

    return PageImpl(results, pageable, totalCount)
  }
}
