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

package dev.yidafu.aqua.common.exposed

import dev.yidafu.aqua.common.domain.table.FileMetadataTable
import dev.yidafu.aqua.common.domain.table.NotificationsTable
import dev.yidafu.aqua.common.domain.table.UserNotificationSettingsTable
import dev.yidafu.aqua.common.domain.table.UserSubscriptionAuthsTable
import dev.yidafu.aqua.common.testsupport.PostgresTestContainer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.LocalDateTime

/** platform 仓储实现测试的 Testcontainers 真实 PG 支撑(P3 收尾覆盖率配套,模式与 analytics/user 一致)。 */
object PlatformPgTestSupport {
  val database: Database = PostgresTestContainer.connect("aquaplatformrepo")

  private val allTables: Array<Table> =
    arrayOf(NotificationsTable, UserSubscriptionAuthsTable, UserNotificationSettingsTable, FileMetadataTable)

  @Volatile
  private var schemaCreated = false

  fun reset() {
    transaction(PlatformPgTestSupport.database) {
      if (schemaCreated) {
        allTables.forEach { it.deleteAll() }
      } else {
        SchemaUtils.create(*allTables)
        schemaCreated = true
      }
    }
  }

  private var nextId = 80000L

  fun id(): Long = nextId++

  fun notification(
    id: Long = id(),
    receiverType: String = "USER",
    userId: Long? = 1L,
    workerId: Long? = null,
    messageType: String = "SYSTEM",
    title: String = "标题$id",
    scene: String = "order",
    idempotencyKey: String = "idem-$id",
    readAt: LocalDateTime? = null,
    channelStatus: String = "SENT",
    createdAt: LocalDateTime = LocalDateTime.now(),
    deleted: Boolean = false,
  ): Long =
    transaction(PlatformPgTestSupport.database) {
      NotificationsTable.insert {
        it[NotificationsTable.id] = id
        it[NotificationsTable.receiverType] = receiverType
        it[NotificationsTable.userId] = userId
        it[NotificationsTable.workerId] = workerId
        it[NotificationsTable.messageType] = messageType
        it[NotificationsTable.scene] = scene
        it[NotificationsTable.title] = title
        it[NotificationsTable.idempotencyKey] = idempotencyKey
        it[NotificationsTable.channelStatus] = channelStatus
        it[NotificationsTable.readAt] = readAt
        it[NotificationsTable.createdAt] = createdAt
        it[updatedAt] = createdAt
        it[isDeleted] = deleted
      }
      id
    }

  fun subscriptionAuth(
    id: Long = id(),
    userId: Long = 1L,
    templateId: String = "tpl-1",
    remainingCount: Int = 1,
  ): Long =
    transaction(PlatformPgTestSupport.database) {
      UserSubscriptionAuthsTable.insert {
        it[UserSubscriptionAuthsTable.id] = id
        it[UserSubscriptionAuthsTable.userId] = userId
        it[UserSubscriptionAuthsTable.templateId] = templateId
        it[UserSubscriptionAuthsTable.remainingCount] = remainingCount
        it[UserSubscriptionAuthsTable.createdAt] = LocalDateTime.now()
        it[updatedAt] = LocalDateTime.now()
      }
      id
    }

  fun file(
    id: Long = id(),
    fileName: String = "f$id.png",
    storagePath: String = "s3://b/f$id.png",
    fileType: String = "IMAGE",
    fileSize: Long = 100L,
    checksum: String = "ck-$id",
    isPublic: Boolean = true,
    extension: String? = "png",
    ownerId: Long? = 7L,
    createdAt: LocalDateTime = LocalDateTime.now(),
  ): Long =
    transaction(PlatformPgTestSupport.database) {
      FileMetadataTable.insert {
        it[FileMetadataTable.id] = id
        it[FileMetadataTable.fileName] = fileName
        it[FileMetadataTable.storagePath] = storagePath
        it[FileMetadataTable.fileType] = fileType
        it[FileMetadataTable.fileSize] = fileSize
        it[mimeType] = "image/png"
        it[FileMetadataTable.checksum] = checksum
        it[FileMetadataTable.isPublic] = isPublic
        it[FileMetadataTable.extension] = extension
        it[FileMetadataTable.ownerId] = ownerId
        it[FileMetadataTable.createdAt] = createdAt
        it[updatedAt] = createdAt
      }
      id
    }
}
