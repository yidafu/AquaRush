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

import dev.yidafu.aqua.api.dto.notification.NotificationListRequest
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationChannelStatus
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.domain.table.NotificationsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
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

/**
 * 站内消息仓储实现(JPA→Exposed 迁移 P3)。
 * 语义对齐 @SoftDelete:查询过滤 is_deleted=false;deleteExpired 为软删(Hibernate 下 DELETE 被改写)。
 * ResultRow 取值全部在事务闭包内(聚合表达式 hashCode 依赖事务上下文)。
 */
@Repository
class NotificationRepositoryImpl(
  private val database: Database,
) : NotificationRepository {
  private fun toModel(row: ResultRow): NotificationModel =
    NotificationModel(
      id = row[NotificationsTable.id],
      receiverType = NotificationReceiverType.valueOf(row[NotificationsTable.receiverType]),
      userId = row[NotificationsTable.userId],
      workerId = row[NotificationsTable.workerId],
      messageType = NotificationMessageType.valueOf(row[NotificationsTable.messageType]),
      scene = row[NotificationsTable.scene],
      title = row[NotificationsTable.title],
      summary = row[NotificationsTable.summary],
      relatedNo = row[NotificationsTable.relatedNo],
      idempotencyKey = row[NotificationsTable.idempotencyKey],
      templateScene = row[NotificationsTable.templateScene],
      templatePage = row[NotificationsTable.templatePage],
      templateFields = row[NotificationsTable.templateFields],
      channelStatus = NotificationChannelStatus.valueOf(row[NotificationsTable.channelStatus]),
      wxMessageId = row[NotificationsTable.wxMessageId],
      errorMessage = row[NotificationsTable.errorMessage],
      sentAt = row[NotificationsTable.sentAt],
      readAt = row[NotificationsTable.readAt],
      createdAt = row[NotificationsTable.createdAt],
      updatedAt = row[NotificationsTable.updatedAt],
    )

  /** 接收人条件:USER → user_id 匹配;WORKER → worker_id 匹配(与原 QueryDSL 谓词逐字对齐)。 */
  private fun receiverCondition(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Op<Boolean> =
    when (receiverType) {
      NotificationReceiverType.USER ->
        (NotificationsTable.receiverType eq receiverType.name) and (NotificationsTable.userId eq receiverId)
      NotificationReceiverType.WORKER ->
        (NotificationsTable.receiverType eq receiverType.name) and (NotificationsTable.workerId eq receiverId)
    }

  private fun contentAndCount(
    condition: Op<Boolean>,
    pageable: Pageable,
  ): Pair<List<NotificationModel>, Long> =
    transaction(database) {
      val content =
        NotificationsTable
          .selectAll()
          .andWhere { condition }
          .orderBy(NotificationsTable.createdAt to org.jetbrains.exposed.v1.core.SortOrder.DESC)
          .offset(pageable.offset)
          .limit(pageable.pageSize)
          .toList()
          .map(::toModel)
      val total =
        NotificationsTable
          .selectAll()
          .andWhere { condition }
          .count()
      content to total
    }

  override fun findMyNotifications(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    messageType: NotificationMessageType?,
    read: Boolean?,
    pageable: Pageable,
  ): Page<NotificationModel> {
    var condition = receiverCondition(receiverType, receiverId)
    messageType?.let { condition = condition and (NotificationsTable.messageType eq it.name) }
    read?.let { isRead ->
      condition =
        if (isRead) {
          condition and NotificationsTable.readAt.isNotNull()
        } else {
          condition and NotificationsTable.readAt.isNull()
        }
    }
    val (content, total) = contentAndCount(condition, pageable)
    return PageImpl(content, pageable, total)
  }

  override fun countUnread(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Long =
    transaction(database) {
      NotificationsTable
        .selectAll()
        .andWhere { receiverCondition(receiverType, receiverId) and NotificationsTable.readAt.isNull() }
        .count()
    }

  override fun markAllRead(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Int =
    transaction(database) {
      val now = LocalDateTime.now()
      NotificationsTable.update(
        {
          NotificationsTable.activeOnly {
            receiverCondition(receiverType, receiverId) and NotificationsTable.readAt.isNull()
          }
        },
      ) {
        it[readAt] = now
        it[updatedAt] = now
      }
    }

  override fun findByFilters(
    request: NotificationListRequest,
    pageable: Pageable,
  ): Page<NotificationModel> {
    var condition: Op<Boolean> = Op.TRUE
    request.messageType?.let { condition = condition and (NotificationsTable.messageType eq it.name) }
    request.receiverType?.let { condition = condition and (NotificationsTable.receiverType eq it.name) }
    request.read?.let { read ->
      condition =
        if (read) {
          condition and NotificationsTable.readAt.isNotNull()
        } else {
          condition and NotificationsTable.readAt.isNull()
        }
    }
    request.startAt?.let { condition = condition and (NotificationsTable.createdAt greaterEq it) }
    request.endAt?.let { condition = condition and (NotificationsTable.createdAt lessEq it) }
    request.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
      val pattern = "%$keyword%"
      condition =
        condition and
        (
          (NotificationsTable.relatedNo like pattern) or
            (NotificationsTable.title like pattern) or
            (NotificationsTable.summary like pattern)
        )
    }
    val (content, total) = contentAndCount(condition, pageable)
    return PageImpl(content, pageable, total)
  }

  /** R-NTF-04 保留策略:创建时间早于 before 的消息软删(语义对齐 @SoftDelete 下的 DELETE 改写)。 */
  override fun deleteExpired(before: LocalDateTime): Int =
    transaction(database) {
      val now = LocalDateTime.now()
      NotificationsTable.update(
        {
          NotificationsTable.activeOnly { NotificationsTable.createdAt less before }
        },
      ) {
        it[isDeleted] = true
        it[deletedAt] = now
        it[updatedAt] = now
      }
    }

  override fun existsByIdempotencyKey(idempotencyKey: String): Boolean =
    transaction(database) {
      NotificationsTable
        .selectAll()
        .andWhere { NotificationsTable.idempotencyKey eq idempotencyKey }
        .limit(1)
        .any()
    }

  override fun findById(id: Long): Optional<NotificationModel> =
    Optional.ofNullable(
      transaction(database) {
        NotificationsTable
          .selectAll()
          .andWhere { NotificationsTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun save(entity: NotificationModel): NotificationModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val now = LocalDateTime.now()
      val exists =
        NotificationsTable
          .selectAllIncludingDeleted()
          .where { NotificationsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        NotificationsTable.update({ NotificationsTable.id eq id }) {
          it[templateScene] = entity.templateScene
          it[templatePage] = entity.templatePage
          it[templateFields] = entity.templateFields
          it[channelStatus] = entity.channelStatus.name
          it[wxMessageId] = entity.wxMessageId
          it[errorMessage] = entity.errorMessage
          it[sentAt] = entity.sentAt
          it[readAt] = entity.readAt
          it[updatedAt] = now
        }
      } else {
        NotificationsTable.insert {
          it[NotificationsTable.id] = id
          it[NotificationsTable.receiverType] = entity.receiverType.name
          it[NotificationsTable.userId] = entity.userId
          it[NotificationsTable.workerId] = entity.workerId
          it[NotificationsTable.messageType] = entity.messageType.name
          it[NotificationsTable.scene] = entity.scene
          it[NotificationsTable.title] = entity.title
          it[NotificationsTable.summary] = entity.summary
          it[NotificationsTable.relatedNo] = entity.relatedNo
          it[NotificationsTable.idempotencyKey] = entity.idempotencyKey
          it[NotificationsTable.templateScene] = entity.templateScene
          it[NotificationsTable.templatePage] = entity.templatePage
          it[NotificationsTable.templateFields] = entity.templateFields
          it[NotificationsTable.channelStatus] = entity.channelStatus.name
          it[NotificationsTable.wxMessageId] = entity.wxMessageId
          it[NotificationsTable.errorMessage] = entity.errorMessage
          it[NotificationsTable.sentAt] = entity.sentAt
          it[NotificationsTable.readAt] = entity.readAt
          it[NotificationsTable.createdAt] = entity.createdAt
          it[NotificationsTable.updatedAt] = entity.updatedAt
        }
      }
      entity
    }
}
