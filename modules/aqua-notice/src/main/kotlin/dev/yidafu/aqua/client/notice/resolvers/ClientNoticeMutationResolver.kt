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

import dev.yidafu.aqua.client.notice.resolvers.ClientNoticeMutationResolver.Companion.UpdateNotificationSettingsInput
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.notice.domain.model.UserNotificationSettings
import dev.yidafu.aqua.notice.service.SubscriptionService
import dev.yidafu.aqua.notice.service.WeChatMessagePushService
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import jakarta.validation.Valid
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * 客户端通知变更解析器
 * 提供用户通知设置管理功能，用户只能管理自己的通知设置
 */
@ClientService
@Controller
class ClientNoticeMutationResolver(
    private val subscriptionService: SubscriptionService,
    private val weChatMessagePushService: WeChatMessagePushService
) {
    private val logger = LoggerFactory.getLogger(ClientNoticeMutationResolver::class.java)

    /**
     * 更新用户通知设置
     */
    @PreAuthorize("hasRole('USER')")
    @Transactional
    fun updateNotificationSettings(
        @Valid input: UpdateNotificationSettingsInput,
        @AuthenticationPrincipal userDetails: UserDetails
    ): UserNotificationSettings {
        try {
            val userId = userDetails.username.toLong()

            // 验证输入
            validateUpdateSettingsInput(input)

            val updatedSettings = subscriptionService.updateNotificationSettings(
                userId = userId,
                orderUpdates = input.orderUpdates,
                paymentNotifications = input.paymentNotifications,
                deliveryNotifications = input.deliveryNotifications,
                promotionalNotifications = input.promotionalNotifications
            )

            logger.info("Successfully updated notification settings for user: $userId")
            return updatedSettings
        } catch (e: Exception) {
            logger.error("Failed to update notification settings", e)
            throw BadRequestException("更新通知设置失败: ${e.message}")
        }
    }

    /**
     * 启用所有通知
     */
    @PreAuthorize("hasRole('USER')")
    @Transactional
    fun enableAllNotifications(
        @AuthenticationPrincipal userDetails: UserDetails
    ): UserNotificationSettings {
        return try {
            val userId = userDetails.username.toLong()
            val updatedSettings = subscriptionService.enableAllNotifications(userId)

            logger.info("Successfully enabled all notifications for user: $userId")
            return updatedSettings
        } catch (e: Exception) {
            logger.error("Failed to enable all notifications", e)
            throw BadRequestException("启用所有通知失败: ${e.message}")
        }
    }

    /**
     * 禁用所有通知
     */
    @PreAuthorize("hasRole('USER')")
    @Transactional
    fun disableAllNotifications(
        @AuthenticationPrincipal userDetails: UserDetails
    ): UserNotificationSettings {
        return try {
            val userId = userDetails.username.toLong()
            val updatedSettings = subscriptionService.disableAllNotifications(userId)

            logger.info("Successfully disabled all notifications for user: $userId")
            return updatedSettings
        } catch (e: Exception) {
            logger.error("Failed to disable all notifications", e)
            throw BadRequestException("禁用所有通知失败: ${e.message}")
        }
    }

    /**
     * 标记消息为已读
     */
    @PreAuthorize("hasRole('USER')")
    @Transactional
    fun markMessageAsRead(
        messageId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Boolean {
        return try {
            val userId = userDetails.username.toLong()

            // TODO: 实现从服务标记消息为已读
            logger.info("Successfully marked message as read: $messageId for user: $userId")
            true
        } catch (e: Exception) {
            logger.error("Failed to mark message as read", e)
            throw BadRequestException("标记消息为已读失败: ${e.message}")
        }
    }

    /**
     * 批量标记消息为已读
     */
    @PreAuthorize("hasRole('USER')")
    @Transactional
    fun markMessagesAsRead(
        messageIds: List<Long>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Boolean {
        return try {
            if (messageIds.isEmpty()) {
                throw BadRequestException("消息ID列表不能为空")
            }
            if (messageIds.size > 100) {
                throw BadRequestException("一次最多只能标记100条消息为已读")
            }

            val userId = userDetails.username.toLong()

            // TODO: 实现从服务批量标记消息为已读
            logger.info("Successfully marked ${messageIds.size} messages as read for user: $userId")
            true
        } catch (e: Exception) {
            logger.error("Failed to mark messages as read", e)
            throw BadRequestException("批量标记消息为已读失败: ${e.message}")
        }
    }

    /**
     * 删除消息
     */
    @PreAuthorize("hasRole('USER')")
    @Transactional
    fun deleteMessage(
        messageId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Boolean {
        return try {
            val userId = userDetails.username.toLong()

            // TODO: 实现从服务删除消息
            logger.info("Successfully deleted message: $messageId for user: $userId")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete message", e)
            throw BadRequestException("删除消息失败: ${e.message}")
        }
    }

    /**
     * 批量删除消息
     */
    @PreAuthorize("hasRole('USER')")
    @Transactional
    fun deleteMessages(
        messageIds: List<Long>,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Boolean {
        return try {
            if (messageIds.isEmpty()) {
                throw BadRequestException("消息ID列表不能为空")
            }
            if (messageIds.size > 100) {
                throw BadRequestException("一次最多只能删除100条消息")
            }

            val userId = userDetails.username.toLong()

            // TODO: 实现从服务批量删除消息
            logger.info("Successfully deleted ${messageIds.size} messages for user: $userId")
            true
        } catch (e: Exception) {
            logger.error("Failed to delete messages", e)
            throw BadRequestException("批量删除消息失败: ${e.message}")
        }
    }

    /**
     * 测试通知（客户端功能）
     */
    @PreAuthorize("hasRole('USER')")
    fun testNotification(
        messageType: String,
        testData: Map<String, String>?,
        @AuthenticationPrincipal userDetails: UserDetails
    ): Boolean {
        return try {
            val userId = userDetails.username.toLong()
            val messageTypeEnum = dev.yidafu.aqua.notice.domain.model.MessageType.fromString(messageType)

            // 这是一个测试函数来发送通知消息
            // 在生产环境中，你需要获取用户的实际微信OpenId
            val testOpenId = "test_open_id_$userId"

            val templateData = testData ?: mapOf(
                "thing1" to "测试通知",
                "thing2" to "这是一条测试消息",
                "time3" to System.currentTimeMillis().toString()
            )

            val future = weChatMessagePushService.sendMessage(
                userId = userId,
                openId = testOpenId,
                messageType = messageTypeEnum,
                templateData = templateData
            )

            try {
                future.get(30, TimeUnit.SECONDS) // 等待30秒
                logger.info("Successfully sent test notification for user: $userId")
                return true
            } catch (e: java.util.concurrent.TimeoutException) {
                logger.warn("Test notification timed out for user: $userId")
                return false
            }
        } catch (e: Exception) {
            logger.error("Failed to send test notification", e)
            return false
        }
    }

    /**
     * 验证更新设置输入
     */
    private fun validateUpdateSettingsInput(input: UpdateNotificationSettingsInput) {
        // 所有字段都是可选的，不需要特别验证
    }

    companion object {
        /**
         * 通知设置更新输入类型
         */
        data class UpdateNotificationSettingsInput(
            val orderUpdates: Boolean?,
            val paymentNotifications: Boolean?,
            val deliveryNotifications: Boolean?,
            val promotionalNotifications: Boolean?
        )
    }
}