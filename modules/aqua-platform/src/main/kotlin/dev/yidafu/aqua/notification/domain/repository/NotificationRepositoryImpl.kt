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

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.api.dto.notification.NotificationListRequest
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.QNotificationModel.Companion.notificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 站内消息自定义查询实现(QueryDSL,PRD 13-notification.md)
 * 本人消息隔离:receiverType + receiverId 由调用方(登录主体)强制注入。
 */
@Repository
class NotificationRepositoryImpl : NotificationRepositoryCustom {
  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private val queryFactory: JPAQueryFactory by lazy {
    JPAQueryFactory(entityManager)
  }

  override fun findMyNotifications(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    messageType: NotificationMessageType?,
    read: Boolean?,
    pageable: Pageable,
  ): Page<NotificationModel> {
    val builder = BooleanBuilder(receiverPredicate(receiverType, receiverId))
    messageType?.let { builder.and(notificationModel.messageType.eq(it)) }
    read?.let { isRead ->
      if (isRead) {
        builder.and(notificationModel.readAt.isNotNull)
      } else {
        builder.and(notificationModel.readAt.isNull)
      }
    }

    val content =
      queryFactory
        .selectFrom(notificationModel)
        .where(builder)
        .orderBy(notificationModel.createdAt.desc())
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .fetch()
    val total =
      queryFactory
        .selectFrom(notificationModel)
        .where(builder)
        .fetchCount()

    return PageImpl(content, pageable, total)
  }

  override fun countUnread(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Long =
    queryFactory
      .selectFrom(notificationModel)
      .where(receiverPredicate(receiverType, receiverId).and(notificationModel.readAt.isNull))
      .fetchCount()

  override fun markAllRead(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Int =
    queryFactory
      .update(notificationModel)
      .set(notificationModel.readAt, LocalDateTime.now())
      .set(notificationModel.updatedAt, LocalDateTime.now())
      .where(receiverPredicate(receiverType, receiverId).and(notificationModel.readAt.isNull))
      .execute()
      .toInt()

  override fun findByFilters(
    request: NotificationListRequest,
    pageable: Pageable,
  ): Page<NotificationModel> {
    val builder = BooleanBuilder()
    request.messageType?.let { builder.and(notificationModel.messageType.eq(it)) }
    request.receiverType?.let { builder.and(notificationModel.receiverType.eq(it)) }
    request.read?.let { read ->
      if (read) {
        builder.and(notificationModel.readAt.isNotNull)
      } else {
        builder.and(notificationModel.readAt.isNull)
      }
    }
    request.startAt?.let { builder.and(notificationModel.createdAt.goe(it)) }
    request.endAt?.let { builder.and(notificationModel.createdAt.loe(it)) }
    request.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
      builder.and(
        notificationModel.relatedNo
          .containsIgnoreCase(keyword)
          .or(notificationModel.title.containsIgnoreCase(keyword))
          .or(notificationModel.summary.containsIgnoreCase(keyword)),
      )
    }

    val content =
      queryFactory
        .selectFrom(notificationModel)
        .where(builder)
        .orderBy(notificationModel.createdAt.desc())
        .offset(pageable.offset)
        .limit(pageable.pageSize.toLong())
        .fetch()
    val total =
      queryFactory
        .selectFrom(notificationModel)
        .where(builder)
        .fetchCount()

    return PageImpl(content, pageable, total)
  }

  override fun deleteExpired(before: LocalDateTime): Int =
    queryFactory
      .delete(notificationModel)
      .where(notificationModel.createdAt.before(before))
      .execute()
      .toInt()

  /** 接收人谓词:USER → user_id 匹配;WORKER → worker_id 匹配 */
  private fun receiverPredicate(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): BooleanExpression =
    when (receiverType) {
      NotificationReceiverType.USER ->
        notificationModel.receiverType
          .eq(NotificationReceiverType.USER)
          .and(notificationModel.userId.eq(receiverId))
      NotificationReceiverType.WORKER ->
        notificationModel.receiverType
          .eq(NotificationReceiverType.WORKER)
          .and(notificationModel.workerId.eq(receiverId))
    }
}
