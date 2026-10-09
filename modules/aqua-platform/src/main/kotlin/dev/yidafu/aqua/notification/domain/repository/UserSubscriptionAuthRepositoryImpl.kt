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

package dev.yidafu.aqua.notification.domain.repository

import dev.yidafu.aqua.common.domain.model.UserSubscriptionAuthModel
import dev.yidafu.aqua.common.domain.table.UserSubscriptionAuthsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/** 微信订阅消息授权仓储实现(JPA→Exposed 迁移 P3)。 */
@Repository
class UserSubscriptionAuthRepositoryImpl(
  private val database: Database,
) : UserSubscriptionAuthRepository {
  private fun toModel(row: ResultRow): UserSubscriptionAuthModel =
    UserSubscriptionAuthModel(
      id = row[UserSubscriptionAuthsTable.id],
      userId = row[UserSubscriptionAuthsTable.userId],
      templateId = row[UserSubscriptionAuthsTable.templateId],
      remainingCount = row[UserSubscriptionAuthsTable.remainingCount],
      createdAt = row[UserSubscriptionAuthsTable.createdAt],
      updatedAt = row[UserSubscriptionAuthsTable.updatedAt],
    )

  override fun findByUserIdAndTemplateId(
    userId: Long,
    templateId: String,
  ): Optional<UserSubscriptionAuthModel> =
    Optional.ofNullable(
      transaction(database) {
        UserSubscriptionAuthsTable
          .selectAll()
          .where {
            UserSubscriptionAuthsTable.activeOnly {
              (UserSubscriptionAuthsTable.userId eq userId) and
                (UserSubscriptionAuthsTable.templateId eq templateId)
            }
          }.limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByUserId(userId: Long): List<UserSubscriptionAuthModel> =
    transaction(database) {
      UserSubscriptionAuthsTable
        .selectAll()
        .where { UserSubscriptionAuthsTable.activeOnly { UserSubscriptionAuthsTable.userId eq userId } }
        .toList()
    }.map(::toModel)

  override fun save(entity: UserSubscriptionAuthModel): UserSubscriptionAuthModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        UserSubscriptionAuthsTable
          .selectAll()
          .where { UserSubscriptionAuthsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        UserSubscriptionAuthsTable.update({ UserSubscriptionAuthsTable.id eq id }) {
          it[remainingCount] = entity.remainingCount
          it[updatedAt] = now
        }
      } else {
        UserSubscriptionAuthsTable.insert {
          it[UserSubscriptionAuthsTable.id] = id
          it[UserSubscriptionAuthsTable.userId] = entity.userId
          it[UserSubscriptionAuthsTable.templateId] = entity.templateId
          it[UserSubscriptionAuthsTable.remainingCount] = entity.remainingCount
          it[UserSubscriptionAuthsTable.createdAt] = entity.createdAt
          it[UserSubscriptionAuthsTable.updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
