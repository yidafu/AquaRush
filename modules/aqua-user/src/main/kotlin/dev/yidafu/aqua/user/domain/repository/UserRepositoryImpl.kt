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

package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.table.AddressesTable
import dev.yidafu.aqua.common.domain.table.UsersTable
import dev.yidafu.aqua.common.graphql.fromValue
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import dev.yidafu.aqua.common.util.MoneyUtils
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
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
 * 用户仓储实现(JPA→Exposed 迁移 P3 收尾)。
 * 查询过滤 is_deleted=false;delete/deleteById 软删(语义对齐 @SoftDelete)。
 */
@Repository
class UserRepositoryImpl(
  private val database: Database,
) : UserRepository {
  private fun toModel(row: ResultRow): UserModel =
    UserModel(
      id = row[UsersTable.id],
      wechatOpenId = row[UsersTable.wechatOpenid],
      nickname = row[UsersTable.nickname],
      phone = row[UsersTable.phone],
      avatarUrl = row[UsersTable.avatarUrl],
      email = row[UsersTable.email],
      status = UserStatus.fromValue(row[UsersTable.status]),
      role =
        dev.yidafu.aqua.common.graphql.generated.UserRole
          .fromValue(row[UsersTable.role]),
      totalSpentCents = row[UsersTable.totalSpentCents],
      customerGroupId = row[UsersTable.customerGroupId],
      createdAt = row[UsersTable.createdAt],
      updatedAt = row[UsersTable.updatedAt],
      lastLoginAt = row[UsersTable.lastLoginAt],
    )

  override fun findById(id: Long): Optional<UserModel> =
    Optional.ofNullable(
      transaction(database) {
        UsersTable
          .selectAll()
          .andWhere { UsersTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findAll(pageable: Pageable): Page<UserModel> {
    val (content, total) =
      transaction(database) {
        val content =
          UsersTable
            .selectAll()
            .orderBy(UsersTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to UsersTable.selectAll().count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findAllById(ids: List<Long>): List<UserModel> {
    if (ids.isEmpty()) return emptyList()
    return transaction(database) {
      UsersTable
        .selectAll()
        .andWhere { UsersTable.id inList ids }
        .toList()
    }.map(::toModel)
  }

  override fun count(): Long =
    transaction(database) {
      UsersTable.selectAll().count()
    }

  override fun existsById(id: Long): Boolean = findById(id).isPresent

  override fun delete(entity: UserModel) {
    val id = requireNotNull(entity.id) { "UserModel.id 不能为空(软删必须携带主键)" }
    deleteById(id)
  }

  override fun deleteById(id: Long) {
    transaction(database) {
      val now = LocalDateTime.now()
      UsersTable.update({ UsersTable.id eq id }) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }
  }

  override fun save(entity: UserModel): UserModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        UsersTable
          .selectAllIncludingDeleted()
          .where { UsersTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        UsersTable.update({ UsersTable.id eq id }) {
          it[nickname] = entity.nickname
          it[phone] = entity.phone
          it[avatarUrl] = entity.avatarUrl
          it[email] = entity.email
          it[status] = entity.status.name
          it[customerGroupId] = entity.customerGroupId
          it[updatedAt] = now
          it[lastLoginAt] = entity.lastLoginAt
        }
      } else {
        UsersTable.insert {
          it[UsersTable.id] = id
          it[wechatOpenid] = entity.wechatOpenId
          it[UsersTable.nickname] = entity.nickname
          it[UsersTable.phone] = entity.phone
          it[UsersTable.avatarUrl] = entity.avatarUrl
          it[UsersTable.email] = entity.email
          it[UsersTable.status] = entity.status.name
          it[UsersTable.role] = entity.role.name
          it[UsersTable.totalSpentCents] = entity.totalSpentCents
          it[UsersTable.customerGroupId] = entity.customerGroupId
          it[UsersTable.createdAt] = entity.createdAt
          it[UsersTable.updatedAt] = entity.updatedAt
          it[UsersTable.lastLoginAt] = entity.lastLoginAt
        }
      }
      entity
    }

  override fun findByWechatOpenId(wechatOpenId: String): UserModel? =
    transaction(database) {
      UsersTable
        .selectAll()
        .andWhere { UsersTable.wechatOpenid eq wechatOpenId }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByPhone(phone: String): UserModel? =
    transaction(database) {
      UsersTable
        .selectAll()
        .andWhere { UsersTable.phone eq phone }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun existsByWechatOpenId(wechatOpenId: String): Boolean = findByWechatOpenId(wechatOpenId) != null

  override fun findByNicknameContainingIgnoreCaseOrPhoneContainingIgnoreCase(
    nickname: String,
    phone: String,
    pageable: Pageable,
  ): Page<UserModel> {
    val condition: Op<Boolean> =
      (UsersTable.nickname.lowerCase() like "%${nickname.lowercase()}%") or
        (UsersTable.phone.lowerCase() like "%${phone.lowercase()}%")
    val (content, total) =
      transaction(database) {
        val content =
          UsersTable
            .selectAll()
            .andWhere { condition }
            .orderBy(UsersTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to UsersTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findByStatus(
    status: UserStatus,
    pageable: Pageable,
  ): Page<UserModel> {
    val condition: Op<Boolean> = UsersTable.status eq status.name
    val (content, total) =
      transaction(database) {
        val content =
          UsersTable
            .selectAll()
            .andWhere { condition }
            .orderBy(UsersTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to UsersTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findByCustomerGroupId(groupId: Long): List<UserModel> =
    transaction(database) {
      UsersTable
        .selectAll()
        .andWhere { UsersTable.customerGroupId eq groupId }
        .toList()
    }.map(::toModel)

  override fun findByCustomerGroupId(
    groupId: Long,
    pageable: Pageable,
  ): Page<UserModel> {
    val condition: Op<Boolean> = UsersTable.customerGroupId eq groupId
    val (content, total) =
      transaction(database) {
        val content =
          UsersTable
            .selectAll()
            .andWhere { condition }
            .orderBy(UsersTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to UsersTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun countByCustomerGroupId(groupId: Long): Long =
    transaction(database) {
      UsersTable
        .selectAll()
        .andWhere { UsersTable.customerGroupId eq groupId }
        .count()
    }

  override fun findByCreatedAtGreaterThan(startDate: LocalDateTime): List<UserModel> =
    transaction(database) {
      UsersTable
        .selectAll()
        .andWhere { UsersTable.createdAt greater startDate }
        .toList()
    }.map(::toModel)

  override fun findByCreatedAtGreaterThanAndCreatedAtLessThan(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<UserModel> =
    transaction(database) {
      UsersTable
        .selectAll()
        .andWhere { (UsersTable.createdAt greater startDate) and (UsersTable.createdAt less endDate) }
        .toList()
    }.map(::toModel)

  override fun countByCreatedAtGreaterThanAndCreatedAtLessThan(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long =
    transaction(database) {
      UsersTable
        .selectAll()
        .andWhere { (UsersTable.createdAt greater startDate) and (UsersTable.createdAt less endDate) }
        .count()
    }

  override fun getUserTotalSpent(userId: Long): Double =
    transaction(database) {
      UsersTable
        .select(UsersTable.totalSpentCents)
        .andWhere { UsersTable.id eq userId }
        .firstOrNull()
    }?.get(UsersTable.totalSpentCents)?.let { MoneyUtils.fromCents(it).toDouble() } ?: 0.0

  override fun getUserAddressCount(userId: Long): Int =
    transaction(database) {
      AddressesTable
        .selectAll()
        .andWhere { AddressesTable.userId eq userId }
        .count()
    }.toInt()

  override fun canUserReview(
    userId: Long,
    orderId: Long,
  ): Boolean = findById(userId).isPresent

  override fun findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
    keyword: String,
    status: UserStatus,
    pageable: Pageable,
  ): Page<UserModel> {
    val lower = "%${keyword.lowercase()}%"
    val condition: Op<Boolean> =
      (UsersTable.status eq status.name) and
        ((UsersTable.nickname.lowerCase() like lower) or (UsersTable.phone.lowerCase() like lower))
    val (content, total) =
      transaction(database) {
        val content =
          UsersTable
            .selectAll()
            .andWhere { condition }
            .orderBy(UsersTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to UsersTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }
}
