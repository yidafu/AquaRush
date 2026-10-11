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

package dev.yidafu.aqua.common.domain.table

import org.jetbrains.exposed.v1.javatime.datetime

// platform 域本地表(JPA→Exposed 迁移 P3 收尾)。
// 列定义逐字对齐 changelog DDL(V100InitComprehensiveSchema 的 file_metadata/
// user_notification_settings/message_history + V150AddNotificationTables 的
// notifications/user_subscription_auths + V260AddNotificationSendSupport 的
// template_* 三列)。
// 五表在 DDL 层都带 is_deleted/deleted_at/deleted_by(V100 的 file_metadata 虽无
// @SoftDelete 注解但 DDL 同样有三列),统一继承 SoftDeletableTable;查询显式过滤
// isDeleted,file_metadata 行为与 JPA 版一致(该实体无软删字段,行恒为 false)。
// 主键由 SnowflakeIdGenerator 生成(与 JPA @SnowflakeIdGenerator 语义一致)。

/** 通知表(`notifications`,R-NTF)。 */
object NotificationsTable : SoftDeletableTable("aqua_notifications") {
  val id = long("id")
  val receiverType = varchar("receiver_type", 20)
  val userId = long("user_id").nullable()
  val workerId = long("worker_id").nullable()
  val messageType = varchar("message_type", 20)
  val scene = varchar("scene", 50)
  val title = varchar("title", 200)
  val summary = varchar("summary", 500).nullable()
  val relatedNo = varchar("related_no", 64).nullable()
  val idempotencyKey = varchar("idempotency_key", 128)
  val templateScene = varchar("template_scene", 50).nullable()
  val templatePage = varchar("template_page", 255).nullable()
  val templateFields = text("template_fields").nullable()
  val channelStatus = varchar("channel_status", 20).clientDefault { "PENDING" }
  val wxMessageId = varchar("wx_message_id", 100).nullable()
  val errorMessage = text("error_message").nullable()
  val sentAt = datetime("sent_at").nullable()
  val readAt = datetime("read_at").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
}

/** 微信订阅消息授权表(`user_subscription_auths`,R-NTF-01 一次性模板)。 */
object UserSubscriptionAuthsTable : SoftDeletableTable("aqua_user_subscription_auths") {
  val id = long("id")
  val userId = long("user_id")
  val templateId = varchar("template_id", 100)
  val remainingCount = integer("remaining_count").clientDefault { 1 }
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
}

/** 用户通知开关表(`user_notification_settings`)。 */
object UserNotificationSettingsTable : SoftDeletableTable("aqua_user_notification_settings") {
  val id = long("id")
  val userId = long("user_id")
  val orderUpdates = bool("order_updates").clientDefault { true }
  val paymentNotifications = bool("payment_notifications").clientDefault { true }
  val deliveryNotifications = bool("delivery_notifications").clientDefault { true }
  val promotionalNotifications = bool("promotional_notifications").clientDefault { false }
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
}

/**
 * 站内信/微信消息发送历史表(`message_history`)。
 * 注意:当前平台主代码已无调用方(死代码保留域),仓储随 P3 收尾迁移以对齐 DDL。
 */
object MessageHistoriesTable : SoftDeletableTable("aqua_message_history") {
  val id = long("id")
  val userId = long("user_id")
  val messageType = varchar("message_type", 50)
  val templateId = varchar("template_id", 100)
  val content = text("content").nullable()
  val status = varchar("status", 20).clientDefault { "PENDING" }
  val sentAt = datetime("sent_at").nullable()
  val wxMessageId = varchar("wx_message_id", 100).nullable()
  val errorMessage = text("error_message").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
  val retryCount = integer("retry_count").clientDefault { 0 }
}

/** 文件元数据表(`file_metadata`,storage 域)。 */
object FileMetadataTable : SoftDeletableTable("aqua_file_metadata") {
  val id = long("id")
  val fileName = varchar("file_name", 255)
  val storagePath = varchar("storage_path", 500)
  val fileType = varchar("file_type", 20)
  val fileSize = long("file_size")
  val mimeType = varchar("mime_type", 100)
  val checksum = varchar("checksum", 64)
  val isPublic = bool("is_public").clientDefault { true }
  val description = varchar("description", 1000).nullable()
  val extension = varchar("extension", 10).nullable()
  val ownerId = long("owner_id").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")
}
