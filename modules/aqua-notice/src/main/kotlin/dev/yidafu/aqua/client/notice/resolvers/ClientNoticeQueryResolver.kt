/*
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.notice.resolvers

import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.dto.PageRequest as CustomPageRequest
import dev.yidafu.aqua.notice.domain.model.MessageHistory
import dev.yidafu.aqua.notice.domain.model.UserNotificationSettings
import dev.yidafu.aqua.notice.service.MessageHistoryService
import dev.yidafu.aqua.notice.service.SubscriptionService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 客户端通知查询解析器
 * 提供用户通知查询功能，用户只能查看自己的通知
 */
@ClientService
@Controller
class ClientNoticeQueryResolver(
    private val subscriptionService: SubscriptionService,
    private val messageHistoryService: MessageHistoryService
) {

    /**
     * 获取用户通知设置
     */
    @PreAuthorize("hasRole('USER')")
    fun getUserNotificationSettings(
        @AuthenticationPrincipal userDetails: UserDetails
    ): UserNotificationSettings {
        val userId = userDetails.username.toLong()
        return subscriptionService.getUserNotificationSettings(userId)
    }

    /**
     * 获取用户消息历史
     */
    @PreAuthorize("hasRole('USER')")
    fun getMessageHistory(
        pageRequest: CustomPageRequest?,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Page<ClientMessage> {
        val userId = userDetails.username.toLong()
        val pageable = if (pageRequest != null) {
            PageRequest.of(pageRequest.page, pageRequest.size)
        } else {
            PageRequest.of(0, 20)
        }
        val messageHistoryPage = messageHistoryService.findByUserId(userId, pageable)

        // Transform MessageHistory to ClientMessage
        val clientMessages = messageHistoryPage.content.map { messageHistory ->
            ClientMessage(
                id = messageHistory.id,
                messageType = messageHistory.messageType,
                title = null, // TODO: Extract title from message content or template
                content = messageHistory.content,
                sentAt = messageHistory.sentAt ?: messageHistory.createdAt,
                readAt = null, // TODO: Implement read status tracking
                isRead = false, // TODO: Implement read status tracking
                data = null // TODO: Parse additional data from message content
            )
        }

        return PageImpl(
            clientMessages,
            messageHistoryPage.pageable,
            messageHistoryPage.totalElements
        )
    }

    /**
     * 获取用户消息统计
     */
    @PreAuthorize("hasRole('USER')")
    fun getMessageStatistics(
        @AuthenticationPrincipal userDetails: UserDetails
    ): Map<String, Long> {
        val userId = userDetails.username.toLong()
        return messageHistoryService.getMessageStatistics(userId)
    }

    /**
     * 检查特定类型的通知是否启用
     */
    @PreAuthorize("hasRole('USER')")
    fun isNotificationEnabled(
        messageType: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Boolean {
        val userId = userDetails.username.toLong()
        val messageTypeEnum = dev.yidafu.aqua.notice.domain.model.MessageType.fromString(messageType)
        return subscriptionService.isNotificationEnabled(userId, messageTypeEnum)
    }

    /**
     * 获取未读消息数量
     */
    @PreAuthorize("hasRole('USER')")
    fun getUnreadMessageCount(
        @AuthenticationPrincipal userDetails: UserDetails
    ): Int {
        val userId = userDetails.username.toLong()
        return messageHistoryService.getUnreadCount(userId)
    }

    /**
     * 获取最近消息
     */
    @PreAuthorize("hasRole('USER')")
    fun getRecentMessages(
        limit: Int = 10,
        messageType: String? = null,
        @AuthenticationPrincipal userDetails: UserDetails
    ): List<ClientMessage> {
        val userId = userDetails.username.toLong()

        // TODO: 实现从服务获取最近消息
        // 目前返回空列表
        return emptyList()
    }

    /**
     * 获取消息详情
     */
    @PreAuthorize("hasRole('USER')")
    fun getMessageDetail(
        messageId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ClientMessage? {
        val userId = userDetails.username.toLong()

        // TODO: 实现从服务获取消息详情
        // 目前返回null
        return null
    }

    /**
     * 按类型获取消息
     */
    @PreAuthorize("hasRole('USER')")
    fun getMessagesByType(
        messageType: String,
        page: Int = 0,
        size: Int = 20,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Page<ClientMessage> {
        val userId = userDetails.username.toLong()
        val pageable = PageRequest.of(page, size)

        // TODO: 实现从服务按类型获取消息
        // 目前返回空列表
        return Page.empty(pageable)
    }

    /**
     * 搜索消息
     */
    @PreAuthorize("hasRole('USER')")
    fun searchMessages(
        keyword: String,
        page: Int = 0,
        size: Int = 20,
        dateFrom: LocalDateTime? = null,
        dateTo: LocalDateTime? = null,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Page<ClientMessage> {
        val userId = userDetails.username.toLong()
        val pageable = PageRequest.of(page, size)

        // TODO: 实现从服务搜索消息
        // 目前返回空列表
        return Page.empty(pageable)
    }

    /**
     * 获取消息类型列表
     */
    @PreAuthorize("hasRole('USER')")
    fun getMessageTypes(): List<MessageTypeInfo> {
        // 返回用户可用的消息类型
        return listOf(
            MessageTypeInfo(
                type = "ORDER_UPDATE",
                name = "订单更新",
                description = "订单状态变更通知"
            ),
            MessageTypeInfo(
                type = "PAYMENT_SUCCESS",
                name = "支付成功",
                description = "支付成功通知"
            ),
            MessageTypeInfo(
                type = "DELIVERY_UPDATE",
                name = "配送更新",
                description = "配送状态变更通知"
            ),
            MessageTypeInfo(
                type = "PROMOTIONAL",
                name = "促销活动",
                description = "促销和优惠活动通知"
            )
        )
    }

    companion object {
        /**
         * 客户端消息相关类型
         */
        data class ClientMessage(
            val id: Long,
            val messageType: String,
            val title: String?,
            val content: String,
            val sentAt: LocalDateTime,
            val readAt: LocalDateTime?,
            val isRead: Boolean,
            val data: Map<String, String>?
        )

        data class MessageTypeInfo(
            val type: String,
            val name: String,
            val description: String
        )
    }
}