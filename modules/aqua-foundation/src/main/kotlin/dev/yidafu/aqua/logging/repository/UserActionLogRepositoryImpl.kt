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

package dev.yidafu.aqua.logging.repository

import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import dev.yidafu.aqua.common.domain.table.UserActionLogsTable
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 用户行为日志仓储实现(JPA→Exposed 迁移 P5,替代原 QueryDSL 实现)。
 *
 * 与迁移前语义一致:
 * - [findByFilters] 动态条件(username 模糊) + created_at 倒序(补 id 倒序破平);
 * - [countDailyLoginsByDateRange] 原用 `DATE(created_at)` 分组,这里用 Exposed 的
 *   `createdAt.date()`(等价 SQL `CAST(created_at AS DATE)`),按日去重用户数;
 * - `user_action_logs` 无软删列(实体无 `@SoftDelete`),查询不做 `is_deleted` 过滤,
 *   [deleteAll] 为硬删。
 */
@Repository
class UserActionLogRepositoryImpl(
  private val database: Database,
) : UserActionLogRepository {
  private fun toModel(row: ResultRow): UserActionLogModel =
    UserActionLogModel(
      id = row[UserActionLogsTable.id],
      userId = row[UserActionLogsTable.userId],
      username = row[UserActionLogsTable.username],
      actionType = row[UserActionLogsTable.actionType],
      target = row[UserActionLogsTable.target],
      pageUrl = row[UserActionLogsTable.pageUrl],
      elementId = row[UserActionLogsTable.elementId],
      elementType = row[UserActionLogsTable.elementType],
      elementText = row[UserActionLogsTable.elementText],
      clientIp = row[UserActionLogsTable.clientIp],
      userAgent = row[UserActionLogsTable.userAgent],
      properties = row[UserActionLogsTable.properties],
      createdAt = row[UserActionLogsTable.createdAt],
    )

  override fun save(entity: UserActionLogModel): UserActionLogModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val exists =
        UserActionLogsTable
          .selectAll()
          .where { UserActionLogsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        UserActionLogsTable.update({ UserActionLogsTable.id eq id }) {
          it[userId] = entity.userId
          it[username] = entity.username
          it[actionType] = entity.actionType
          it[target] = entity.target
          it[pageUrl] = entity.pageUrl
          it[elementId] = entity.elementId
          it[elementType] = entity.elementType
          it[elementText] = entity.elementText
          it[clientIp] = entity.clientIp
          it[userAgent] = entity.userAgent
          it[properties] = entity.properties
        }
      } else {
        UserActionLogsTable.insert {
          it[UserActionLogsTable.id] = id
          it[userId] = entity.userId
          it[username] = entity.username
          it[actionType] = entity.actionType
          it[target] = entity.target
          it[pageUrl] = entity.pageUrl
          it[elementId] = entity.elementId
          it[elementType] = entity.elementType
          it[elementText] = entity.elementText
          it[clientIp] = entity.clientIp
          it[userAgent] = entity.userAgent
          it[properties] = entity.properties
          it[createdAt] = entity.createdAt
        }
      }
      entity
    }

  override fun count(): Long =
    transaction(database) {
      UserActionLogsTable.selectAll().count()
    }

  override fun deleteAll(): Int =
    transaction(database) {
      UserActionLogsTable.deleteAll()
    }

  override fun findByUserId(userId: String): List<UserActionLogModel> =
    transaction(database) {
      UserActionLogsTable
        .selectAll()
        .where { UserActionLogsTable.userId eq userId }
        .orderBy(UserActionLogsTable.createdAt to SortOrder.ASC, UserActionLogsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByActionType(actionType: String): List<UserActionLogModel> =
    transaction(database) {
      UserActionLogsTable
        .selectAll()
        .where { UserActionLogsTable.actionType eq actionType }
        .orderBy(UserActionLogsTable.createdAt to SortOrder.ASC, UserActionLogsTable.id to SortOrder.ASC)
        .toList()
    }.map(::toModel)

  override fun findByFilters(
    userId: String?,
    actionType: String?,
    username: String?,
    target: String?,
    startTime: LocalDateTime?,
    endTime: LocalDateTime?,
    pageable: Pageable,
  ): Page<UserActionLogModel> {
    val conditions =
      buildList {
        userId?.let { add(UserActionLogsTable.userId eq it) }
        actionType?.let { add(UserActionLogsTable.actionType eq it) }
        username?.let { add(UserActionLogsTable.username like "%$it%") }
        target?.let { add(UserActionLogsTable.target eq it) }
        startTime?.let { add(UserActionLogsTable.createdAt greaterEq it) }
        endTime?.let { add(UserActionLogsTable.createdAt lessEq it) }
      }
    val condition = conditions.reduceOrNull { a, b -> a and b } ?: Op.TRUE
    val (content, total) =
      transaction(database) {
        val content =
          UserActionLogsTable
            .selectAll()
            .where { condition }
            .orderBy(UserActionLogsTable.createdAt to SortOrder.DESC, UserActionLogsTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to UserActionLogsTable.selectAll().where { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun countDistinctUsersByActionTypeAndDateRange(
    actionType: String,
    startTime: LocalDateTime,
    endTime: LocalDateTime,
  ): Long =
    transaction(database) {
      UserActionLogsTable
        .select(UserActionLogsTable.userId.countDistinct())
        .where {
          (UserActionLogsTable.actionType eq actionType) and
            (UserActionLogsTable.createdAt greaterEq startTime) and
            (UserActionLogsTable.createdAt lessEq endTime)
        }.first()[UserActionLogsTable.userId.countDistinct()]
    }

  override fun countDailyLoginsByDateRange(
    startTime: LocalDateTime,
    endTime: LocalDateTime,
  ): Map<LocalDate, Long> {
    val dayExpr = UserActionLogsTable.createdAt.date()
    return transaction(database) {
      UserActionLogsTable
        .select(dayExpr, UserActionLogsTable.userId.countDistinct())
        .where {
          (UserActionLogsTable.actionType eq "USER_LOGIN") and
            (UserActionLogsTable.createdAt greaterEq startTime) and
            (UserActionLogsTable.createdAt lessEq endTime)
        }.groupBy(dayExpr)
        .associate { row -> row[dayExpr] to row[UserActionLogsTable.userId.countDistinct()] }
    }
  }
}
