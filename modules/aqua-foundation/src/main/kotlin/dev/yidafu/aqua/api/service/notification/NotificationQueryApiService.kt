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

import dev.yidafu.aqua.api.dto.notification.NotificationListRequest
import dev.yidafu.aqua.api.dto.notification.SubscribeTemplateDTO
import dev.yidafu.aqua.common.domain.model.NotificationModel
import dev.yidafu.aqua.common.domain.model.enums.NotificationMessageType
import dev.yidafu.aqua.common.domain.model.enums.NotificationReceiverType
import org.springframework.data.domain.Page

/**
 * 通知查询服务接口(PRD 13-notification.md)
 * 用户端/送水工端消息中心(F-NTF-002/004)与后台消息列表(F-NTF-006)共用;
 * 本人消息隔离:receiverType + receiverId 由登录主体强制注入(防越权,R-AUTH-02)。
 */
interface NotificationQueryApiService {
  /**
   * 分页查询本人消息(仅本人,按创建时间倒序;messageType/read 为空 = 全部)
   */
  fun findMyNotifications(
    receiverType: NotificationReceiverType,
    receiverId: Long,
    messageType: NotificationMessageType?,
    read: Boolean?,
    page: Int,
    size: Int,
  ): Page<NotificationModel>

  /**
   * 本人未读数(入口角标)
   */
  fun countUnread(
    receiverType: NotificationReceiverType,
    receiverId: Long,
  ): Long

  /**
   * 已启用的订阅消息模板列表(下单确认页订阅授权用)
   */
  fun findEnabledSubscribeTemplates(): List<SubscribeTemplateDTO>

  /**
   * 后台消息列表(按筛选条件分页,近 30 天由页面层给定时间范围)
   */
  fun findNotifications(
    request: NotificationListRequest,
    page: Int,
    size: Int,
  ): Page<NotificationModel>

  /**
   * 后台消息详情
   */
  fun findById(id: Long): NotificationModel?
}
