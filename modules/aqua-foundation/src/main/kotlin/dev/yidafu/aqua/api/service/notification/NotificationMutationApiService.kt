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

package dev.yidafu.aqua.api.service.notification

import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType

/**
 * 通知写操作服务接口(PRD 13-notification.md R-NTF-04)
 * 已读标记幂等;本人消息隔离:receiverType + receiverId 由登录主体强制注入(防越权)。
 */
interface NotificationMutationApiService {
  /**
   * 单条已读(幂等:已读再标记不报错),返回是否命中本人消息
   */
  fun markRead(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    notificationId: Long,
  ): Boolean

  /**
   * 全部已读,返回置读条数
   */
  fun markAllRead(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Int

  /**
   * 记录微信订阅消息授权(R-NTF-01,一次性模板:授权一次可发一次)
   * 仅记录用户实际接受的模板;已存在的模板追加剩余次数。
   */
  fun recordSubscribeAuth(
    userId: Long,
    templateIds: List<String>,
  ): Int

  /**
   * 手动重发失败/待发送的微信订阅消息(F-NTF-006,后台超管):
   * 仅接收人为 USER 且已持久化模板场景、通道状态为 FAILED/PENDING 的消息可重发;
   * 重置为 PENDING 后复用微信订阅消息全部门控(R-NTF-01)重发,返回更新后的消息;
   * 不可重发返回 null。
   */
  fun resendNotification(notificationId: Long): NotificationModel?
}
