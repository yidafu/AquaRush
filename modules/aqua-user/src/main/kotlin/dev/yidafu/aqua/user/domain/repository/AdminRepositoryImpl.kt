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

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.table.AdminsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/** 账号/送水员仓储实现(JPA→Exposed 迁移 P3 收尾)。 */
@Repository
class AdminRepositoryImpl(
  private val database: Database,
) : AdminRepository {
  private fun toModel(row: ResultRow): AdminModel =
    AdminModel(
      id = row[AdminsTable.id],
      username = row[AdminsTable.username],
      passwordHash = row[AdminsTable.passwordHash],
      realName = row[AdminsTable.realName],
      phone = row[AdminsTable.phone],
      role = AdminRoleModel.valueOf(row[AdminsTable.role]),
      userId = row[AdminsTable.userId],
      accountStatus = AdminAccountStatus.valueOf(row[AdminsTable.accountStatus]),
      avatarUrl = row[AdminsTable.avatarUrl],
      onlineStatus = DeliverWorkerModelStatus.valueOf(row[AdminsTable.onlineStatus]),
      coordinates = row[AdminsTable.coordinates],
      currentLocation = row[AdminsTable.currentLocation],
      rating = row[AdminsTable.rating],
      totalOrders = row[AdminsTable.totalOrders],
      completedOrders = row[AdminsTable.completedOrders],
      averageRating = row[AdminsTable.averageRating],
      earningCents = row[AdminsTable.earningCents],
      isAvailable = row[AdminsTable.isAvailable],
      createdAt = row[AdminsTable.createdAt],
      lastLoginAt = row[AdminsTable.lastLoginAt],
      updatedAt = row[AdminsTable.updatedAt],
    )

  override fun findById(id: Long): Optional<AdminModel> =
    Optional.ofNullable(
      transaction(database) {
        AdminsTable
          .selectAll()
          .andWhere { AdminsTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun existsById(id: Long): Boolean = findById(id).isPresent

  override fun count(): Long = transaction(database) { AdminsTable.selectAll().count() }

  override fun deleteById(id: Long) {
    transaction(database) {
      val now = LocalDateTime.now()
      AdminsTable.update({ AdminsTable.id eq id }) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }
  }

  override fun deleteAll(): Int =
    transaction(database) {
      val now = LocalDateTime.now()
      AdminsTable.update({ Op.TRUE }) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }

  override fun save(entity: AdminModel): AdminModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        AdminsTable
          .selectAllIncludingDeleted()
          .where { AdminsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        AdminsTable.update({ AdminsTable.id eq id }) {
          it[username] = entity.username
          it[passwordHash] = entity.passwordHash
          it[realName] = entity.realName
          it[AdminsTable.phone] = entity.phone
          it[AdminsTable.role] = entity.role.name
          it[AdminsTable.userId] = entity.userId
          it[accountStatus] = entity.accountStatus.name
          it[avatarUrl] = entity.avatarUrl
          it[onlineStatus] = entity.onlineStatus.name
          it[coordinates] = entity.coordinates
          it[currentLocation] = entity.currentLocation
          it[AdminsTable.rating] = entity.rating
          it[totalOrders] = entity.totalOrders
          it[completedOrders] = entity.completedOrders
          it[averageRating] = entity.averageRating
          it[earningCents] = entity.earningCents
          it[isAvailable] = entity.isAvailable
          it[lastLoginAt] = entity.lastLoginAt
          it[updatedAt] = now
        }
      } else {
        AdminsTable.insert {
          it[AdminsTable.id] = id
          it[AdminsTable.username] = entity.username
          it[AdminsTable.passwordHash] = entity.passwordHash
          it[AdminsTable.realName] = entity.realName
          it[AdminsTable.phone] = entity.phone
          it[AdminsTable.role] = entity.role.name
          it[AdminsTable.userId] = entity.userId
          it[AdminsTable.accountStatus] = entity.accountStatus.name
          it[AdminsTable.avatarUrl] = entity.avatarUrl
          it[AdminsTable.onlineStatus] = entity.onlineStatus.name
          it[AdminsTable.coordinates] = entity.coordinates
          it[AdminsTable.currentLocation] = entity.currentLocation
          it[AdminsTable.rating] = entity.rating
          it[AdminsTable.totalOrders] = entity.totalOrders
          it[AdminsTable.completedOrders] = entity.completedOrders
          it[AdminsTable.averageRating] = entity.averageRating
          it[AdminsTable.earningCents] = entity.earningCents
          it[AdminsTable.isAvailable] = entity.isAvailable
          it[AdminsTable.createdAt] = entity.createdAt
          it[AdminsTable.lastLoginAt] = entity.lastLoginAt
          it[AdminsTable.updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun findByUsername(username: String): AdminModel? =
    transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { AdminsTable.username eq username }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByUserId(userId: Long): AdminModel? =
    transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { AdminsTable.userId eq userId }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByUsernameAndRole(
    username: String,
    role: AdminRoleModel,
  ): AdminModel? =
    transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { (AdminsTable.username eq username) and (AdminsTable.role eq role.name) }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByPhone(phone: String): AdminModel? =
    transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { AdminsTable.phone eq phone }
        .limit(1)
        .firstOrNull()
    }?.let(::toModel)

  override fun findByRole(role: AdminRoleModel): List<AdminModel> =
    transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { AdminsTable.role eq role.name }
        .toList()
    }.map(::toModel)

  override fun findAllByRoleIn(
    roles: Collection<AdminRoleModel>,
    keyword: String?,
    pageable: Pageable,
  ): Page<AdminModel> {
    if (roles.isEmpty()) return PageImpl(emptyList(), pageable, 0)
    return transaction(database) {
      val roleNames = roles.map { it.name }
      val trimmed = keyword?.trim().orEmpty()
      val condition: () -> Op<Boolean> = {
        val byRole = AdminsTable.role inList roleNames
        if (trimmed.isEmpty()) {
          byRole
        } else {
          val pattern = "%$trimmed%"
          byRole and
            ((AdminsTable.username like pattern) or (AdminsTable.realName like pattern) or (AdminsTable.phone like pattern))
        }
      }
      val content =
        AdminsTable
          .selectAll()
          .andWhere(condition)
          .orderBy(AdminsTable.id to SortOrder.ASC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      val total = AdminsTable.selectAll().andWhere(condition).count()
      PageImpl(content, pageable, total)
    }
  }

  override fun findAllByRoleIn(
    roles: Collection<AdminRoleModel>,
    limit: Int,
  ): List<AdminModel> {
    if (roles.isEmpty()) return emptyList()
    return transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { AdminsTable.role inList roles.map { it.name } }
        .orderBy(AdminsTable.id to SortOrder.ASC)
        .limit(limit)
        .toList()
    }.map(::toModel)
  }

  override fun countByRoleIn(roles: Collection<AdminRoleModel>): Long {
    if (roles.isEmpty()) return 0L
    return transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { AdminsTable.role inList roles.map { it.name } }
        .count()
    }
  }

  override fun findByOnlineStatus(
    status: DeliverWorkerModelStatus,
    limit: Int,
  ): List<AdminModel> =
    transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { AdminsTable.onlineStatus eq status.name }
        .orderBy(AdminsTable.id to SortOrder.ASC)
        .limit(limit)
        .toList()
    }.map(::toModel)

  override fun countByRole(role: AdminRoleModel): Long =
    transaction(database) {
      AdminsTable
        .selectAll()
        .andWhere { AdminsTable.role eq role.name }
        .count()
    }

  override fun existsByUsername(username: String): Boolean = findByUsername(username) != null

  override fun existsByPhone(phone: String): Boolean = findByPhone(phone) != null

  override fun findAllAdmins(
    keyword: String?,
    role: AdminRoleModel?,
    pageable: Pageable,
  ): Page<AdminModel> =
    transaction(database) {
      val trimmed = keyword?.trim().orEmpty()
      val keywordCondition: Op<Boolean> =
        if (trimmed.isEmpty()) {
          Op.TRUE
        } else {
          val pattern = "%$trimmed%"
          (AdminsTable.username like pattern) or
            (AdminsTable.realName like pattern) or
            (AdminsTable.phone like pattern)
        }
      val condition: () -> Op<Boolean> = {
        if (role == null) keywordCondition else keywordCondition and (AdminsTable.role eq role.name)
      }
      val content =
        AdminsTable
          .selectAll()
          .andWhere(condition)
          .orderBy(AdminsTable.id to SortOrder.ASC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      val total = AdminsTable.selectAll().andWhere(condition).count()
      PageImpl(content, pageable, total)
    }

  override fun findAdminById(id: Long): AdminModel? = findById(id).orElse(null)
}
