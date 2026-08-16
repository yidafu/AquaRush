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

package dev.yidafu.aqua.notification.service

import dev.yidafu.aqua.api.service.notification.NotificationMutationApiService
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.UserSubscriptionAuthModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import dev.yidafu.aqua.notification.domain.repository.NotificationRepository
import dev.yidafu.aqua.notification.domain.repository.UserSubscriptionAuthRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 通知写操作服务实现(PRD 13-notification.md R-NTF-01/04)
 * 已读标记幂等;本人消息隔离(接收人类型 + 接收人 id 由登录主体注入)。
 */
@Service
class NotificationMutationApiServiceImpl(
  private val notificationRepository: NotificationRepository,
  private val userSubscriptionAuthRepository: UserSubscriptionAuthRepository,
) : NotificationMutationApiService {
  @Transactional
  override fun markRead(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    notificationId: Long,
  ): Boolean {
    val notification = notificationRepository.findById(notificationId).orElse(null) ?: return false
    if (!matchesReceiver(notification, receiverType, receiverId)) {
      return false
    }
    if (notification.readAt == null) {
      notification.markRead()
      notificationRepository.save(notification)
    }
    return true
  }

  @Transactional
  override fun markAllRead(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Int = notificationRepository.markAllRead(receiverType, receiverId)

  @Transactional
  override fun recordSubscribeAuth(
    userId: Long,
    templateIds: List<String>,
  ): Int {
    var count = 0
    templateIds.distinct().forEach { templateId ->
      val existing = userSubscriptionAuthRepository.findByUserIdAndTemplateId(userId, templateId).orElse(null)
      if (existing != null) {
        existing.remainingCount += 1
        userSubscriptionAuthRepository.save(existing)
      } else {
        userSubscriptionAuthRepository.save(
          UserSubscriptionAuthModel(
            userId = userId,
            templateId = templateId,
            remainingCount = 1,
          ),
        )
      }
      count++
    }
    return count
  }

  private fun matchesReceiver(
    notification: NotificationModel,
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Boolean =
    when (receiverType) {
      NotificationReceiverType.USER ->
        notification.receiverType == NotificationReceiverType.USER && notification.userId == receiverId
      NotificationReceiverType.WORKER ->
        notification.receiverType == NotificationReceiverType.WORKER && notification.workerId == receiverId
    }
}
