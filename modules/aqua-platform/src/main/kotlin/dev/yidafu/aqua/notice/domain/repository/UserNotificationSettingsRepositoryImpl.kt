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

package dev.yidafu.aqua.notice.domain.repository

import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.common.domain.table.UserNotificationSettingsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/** 用户通知开关仓储实现(JPA→Exposed 迁移 P3)。 */
@Repository
class UserNotificationSettingsRepositoryImpl(
  private val database: Database,
) : UserNotificationSettingsRepository {
  private fun toModel(row: org.jetbrains.exposed.v1.core.ResultRow): UserNotificationSettingsModel =
    UserNotificationSettingsModel(
      id = row[UserNotificationSettingsTable.id],
      userId = row[UserNotificationSettingsTable.userId],
      orderUpdates = row[UserNotificationSettingsTable.orderUpdates],
      paymentNotifications = row[UserNotificationSettingsTable.paymentNotifications],
      deliveryNotifications = row[UserNotificationSettingsTable.deliveryNotifications],
      promotionalNotifications = row[UserNotificationSettingsTable.promotionalNotifications],
      createdAt = row[UserNotificationSettingsTable.createdAt],
      updatedAt = row[UserNotificationSettingsTable.updatedAt],
    )

  override fun findByUserId(userId: Long): Optional<UserNotificationSettingsModel> =
    Optional.ofNullable(
      transaction(database) {
        UserNotificationSettingsTable
          .selectAll()
          .andWhere { UserNotificationSettingsTable.userId eq userId }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun existsByUserId(userId: Long): Boolean = findByUserId(userId).isPresent

  override fun deleteByUserId(userId: Long): Int =
    transaction(database) {
      val now = LocalDateTime.now()
      UserNotificationSettingsTable.update(
        {
          UserNotificationSettingsTable.activeOnly { UserNotificationSettingsTable.userId eq userId }
        },
      ) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }

  override fun save(entity: UserNotificationSettingsModel): UserNotificationSettingsModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        UserNotificationSettingsTable
          .selectAllIncludingDeleted()
          .where { UserNotificationSettingsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        UserNotificationSettingsTable.update({ UserNotificationSettingsTable.id eq id }) {
          it[orderUpdates] = entity.orderUpdates
          it[paymentNotifications] = entity.paymentNotifications
          it[deliveryNotifications] = entity.deliveryNotifications
          it[promotionalNotifications] = entity.promotionalNotifications
          it[updatedAt] = now
        }
      } else {
        UserNotificationSettingsTable.insert {
          it[UserNotificationSettingsTable.id] = id
          it[UserNotificationSettingsTable.userId] = entity.userId
          it[UserNotificationSettingsTable.orderUpdates] = entity.orderUpdates
          it[UserNotificationSettingsTable.paymentNotifications] = entity.paymentNotifications
          it[UserNotificationSettingsTable.deliveryNotifications] = entity.deliveryNotifications
          it[UserNotificationSettingsTable.promotionalNotifications] = entity.promotionalNotifications
          it[createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
