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

import dev.yidafu.aqua.common.domain.model.NotificationSettingsModel
import dev.yidafu.aqua.common.domain.table.NotificationSettingsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/** 用户通知开关仓储实现(user 域;JPA→Exposed 迁移 P3 收尾)。 */
@Repository
class NotificationSettingsRepositoryImpl(
  private val database: Database,
) : NotificationSettingsRepository {
  private fun toModel(row: ResultRow): NotificationSettingsModel =
    NotificationSettingsModel(
      id = row[NotificationSettingsTable.id],
      userId = row[NotificationSettingsTable.userId],
      orderUpdates = row[NotificationSettingsTable.orderUpdates],
      paymentNotifications = row[NotificationSettingsTable.paymentNotifications],
      deliveryNotifications = row[NotificationSettingsTable.deliveryNotifications],
      promotionalNotifications = row[NotificationSettingsTable.promotionalNotifications],
      createdAt = row[NotificationSettingsTable.createdAt],
      updatedAt = row[NotificationSettingsTable.updatedAt],
    )

  override fun findByUserId(userId: Long): Optional<NotificationSettingsModel> =
    Optional.ofNullable(
      transaction(database) {
        NotificationSettingsTable
          .selectAll()
          .where {
            NotificationSettingsTable.activeOnly { NotificationSettingsTable.userId eq userId }
          }.limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun deleteByUserId(userId: Long) {
    transaction(database) {
      val now = LocalDateTime.now()
      NotificationSettingsTable.update(
        {
          NotificationSettingsTable.activeOnly { NotificationSettingsTable.userId eq userId }
        },
      ) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }
  }

  override fun save(entity: NotificationSettingsModel): NotificationSettingsModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        NotificationSettingsTable
          .selectAll()
          .where { NotificationSettingsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        NotificationSettingsTable.update({ NotificationSettingsTable.id eq id }) {
          it[orderUpdates] = entity.orderUpdates
          it[paymentNotifications] = entity.paymentNotifications
          it[deliveryNotifications] = entity.deliveryNotifications
          it[promotionalNotifications] = entity.promotionalNotifications
          it[updatedAt] = now
        }
      } else {
        NotificationSettingsTable.insert {
          it[NotificationSettingsTable.id] = id
          it[NotificationSettingsTable.userId] = entity.userId
          it[NotificationSettingsTable.orderUpdates] = entity.orderUpdates
          it[NotificationSettingsTable.paymentNotifications] = entity.paymentNotifications
          it[NotificationSettingsTable.deliveryNotifications] = entity.deliveryNotifications
          it[NotificationSettingsTable.promotionalNotifications] = entity.promotionalNotifications
          it[NotificationSettingsTable.createdAt] = entity.createdAt
          it[NotificationSettingsTable.updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
