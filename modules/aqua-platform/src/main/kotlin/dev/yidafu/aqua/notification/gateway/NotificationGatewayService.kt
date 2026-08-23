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

package dev.yidafu.aqua.notification.gateway

import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.common.messaging.event.DomainEvent
import dev.yidafu.aqua.notification.channel.wechat.WeChatSubscribeMessageService
import dev.yidafu.aqua.notification.channel.ws.NotificationPushEventPublisher
import dev.yidafu.aqua.notification.channel.ws.PushEvent
import dev.yidafu.aqua.notification.domain.repository.NotificationRepository
import dev.yidafu.aqua.notification.dto.NotificationEventData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.ZoneId

/**
 * 通知消息网关(PRD 13-notification.md F-NTF-002/003/004/006)
 *
 * 消费 notifications-events 队列的 NOTIFICATION_SEND 事件:
 * 1. 广播事件(新订单可抢单)→ 仅 WebSocket 广播推送,不落库;
 * 2. 定向事件 → 幂等落库(notifications.idempotency_key 唯一,重复事件跳过);
 * 3. 新插入才异步分发(事务提交后):WebSocket 定向推送 + 微信订阅消息发送。
 *
 * 站内消息以 notifications 表为权威;WebSocket/微信失败不回滚已落库消息(尽力而为推送,
 * 客户端断线重连后从消息中心补齐,R-NTF-02/03)。
 */
@Service
class NotificationGatewayService(
  private val notificationRepository: NotificationRepository,
  private val notificationPushEventPublisher: NotificationPushEventPublisher,
  private val weChatSubscribeMessageService: WeChatSubscribeMessageService,
  @Qualifier("notificationDispatchExecutor") private val dispatchExecutor: TaskExecutor,
) {
  private val logger = LoggerFactory.getLogger(NotificationGatewayService::class.java)
  private val objectMapper = jacksonObjectMapper()

  @Transactional
  fun handle(event: DomainEvent) {
    val data = NotificationEventData.fromPayload(event.payload)

    // 广播路径:仅 WebSocket 广播推送(如送水工新订单可抢单),不落库
    if (data.broadcast) {
      afterCommit {
        notificationPushEventPublisher.publish(
          PushEvent(receiverType = "WORKER", broadcast = true, payload = broadcastPayload(data)),
        )
      }
      return
    }

    val receiverType = NotificationReceiverType.valueOf(data.receiverType)
    val receiverId: Long
    when (receiverType) {
      NotificationReceiverType.USER -> {
        val userId = data.userId
        if (userId == null) {
          logger.warn("通知事件缺少 userId,跳过: {}", data.idempotencyKey)
          return
        }
        receiverId = userId
      }
      NotificationReceiverType.WORKER -> {
        val workerId = data.workerId
        if (workerId == null) {
          logger.warn("通知事件缺少 workerId,跳过: {}", data.idempotencyKey)
          return
        }
        receiverId = workerId
      }
    }

    // 幂等落库(R-NTF-02):重复事件跳过;并发竞态由唯一键兜底,冲突抛错触发重投
    if (notificationRepository.existsByIdempotencyKey(data.idempotencyKey)) {
      logger.debug("重复通知事件,跳过: {}", data.idempotencyKey)
      return
    }

    val saved =
      notificationRepository.saveAndFlush(
        NotificationModel(
          receiverType = receiverType,
          userId = data.userId,
          workerId = data.workerId,
          messageType = NotificationMessageType.valueOf(data.messageType),
          scene = data.scene,
          title = data.title,
          summary = data.summary,
          relatedNo = data.relatedNo,
          idempotencyKey = data.idempotencyKey,
          // 持久化微信订阅消息模板上下文(重发失败消息还原用,F-NTF-006)
          templateScene = data.templateScene,
          templatePage = data.page,
          templateFields =
            data.templateFields?.let { fields ->
              objectMapper.writeValueAsString(fields)
            },
        ),
      )

    // 事务提交后再异步分发(避免分发线程读到未提交数据)
    afterCommit {
      try {
        val unread = notificationRepository.countUnread(receiverType, receiverId)
        val payload = buildPushPayload(saved, unread)
        notificationPushEventPublisher.publish(
          PushEvent(
            receiverType = receiverType.name,
            receiverId = receiverId,
            broadcast = false,
            payload = payload,
          ),
        )
        if (receiverType == NotificationReceiverType.USER && data.templateScene != null) {
          weChatSubscribeMessageService.trySend(saved, data.templateScene, data.page, data.templateFields)
        }
      } catch (e: Exception) {
        logger.error("通知异步分发失败, notificationId={}", saved.id, e)
      }
    }
  }

  /** 事务提交后执行;无事务时立即执行 */
  private fun afterCommit(task: () -> Unit) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
        object : TransactionSynchronization {
          override fun afterCommit() {
            dispatchExecutor.execute(task)
          }
        },
      )
    } else {
      dispatchExecutor.execute(task)
    }
  }

  private fun buildPushPayload(
    notification: NotificationModel,
    unreadCount: Long,
  ): Map<String, Any?> =
    mapOf<String, Any?>(
      "id" to notification.id,
      "idempotencyKey" to notification.idempotencyKey,
      "type" to notification.messageType.name,
      "scene" to notification.scene,
      "title" to notification.title,
      "summary" to notification.summary,
      "relatedNo" to notification.relatedNo,
      "createdAt" to
        notification.createdAt
          .atZone(ZoneId.systemDefault())
          .toInstant()
          .toEpochMilli(),
      "unreadCount" to unreadCount,
    )

  private fun broadcastPayload(data: NotificationEventData): Map<String, Any?> =
    mapOf<String, Any?>(
      "idempotencyKey" to data.idempotencyKey,
      "scene" to data.scene,
      "title" to data.title,
      "summary" to data.summary,
      "relatedNo" to data.relatedNo,
      "createdAt" to data.eventTime,
    )
}
