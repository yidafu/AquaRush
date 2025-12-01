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

package dev.yidafu.aqua.admin.notice.resolvers

import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.notice.domain.model.MessageType
import dev.yidafu.aqua.notice.service.WeChatMessagePushService
import org.springframework.data.domain.Page
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 管理端通知查询解析器
 * 提供通知管理的查询功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminNoticeQueryResolver(
    private val weChatMessagePushService: WeChatMessagePushService
) {

    /**
     * 获取系统消息统计信息（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun getSystemMessageStatistics(since: LocalDateTime? = null): SystemMessageStats {
        val sinceDate = since ?: LocalDateTime.now().minusDays(7)
        val stats = weChatMessagePushService.getMessageStatistics(sinceDate)

        val totalSent = stats.values.sum()
        val successCount = stats.filterKeys { it.contains("_SUCCESS") }.values.sum()
        val failureCount = stats.filterKeys { it.contains("_FAILURE") }.values.sum()
        val successRate = if (totalSent > 0) (successCount.toDouble() / totalSent) * 100 else 0.0

        return SystemMessageStats(
            totalSent = totalSent,
            successCount = successCount,
            failureCount = failureCount,
            successRate = successRate,
            messagesByType = stats.mapKeys { it.key.removeSuffix("_SUCCESS").removeSuffix("_FAILURE") },
            messagesByHour = emptyMap(), // TODO: Implement hourly statistics
            topFailureReasons = emptyList() // TODO: Implement failure reason tracking
        )
    }

    /**
     * 获取系统消息模板列表（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun getSystemMessageTemplates(
        page: Int = 0,
        size: Int = 20,
        messageType: String? = null
    ): Page<NotificationTemplate> {
        // TODO: 实现从服务获取系统消息模板
        // 目前返回空列表
        return Page.empty(org.springframework.data.domain.PageRequest.of(page, size))
    }

    /**
     * 根据ID获取通知模板详情（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun getNotificationTemplate(id: Long): NotificationTemplate? {
        // TODO: 实现从服务获取特定通知模板
        // 目前返回null
        return null
    }

    /**
     * 获取消息发送历史（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun getMessageHistory(
        page: Int = 0,
        size: Int = 20,
        userId: Long? = null,
        messageType: String? = null,
        status: String? = null,
        dateFrom: LocalDateTime? = null,
        dateTo: LocalDateTime? = null
    ): Page<SystemMessageHistory> {
        // TODO: 实现从服务获取消息发送历史
        // 目前返回空列表
        return Page.empty(org.springframework.data.domain.PageRequest.of(page, size))
    }

    /**
     * 获取消息失败统计（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun getFailedMessageStatistics(
        dateFrom: LocalDateTime? = null,
        dateTo: LocalDateTime? = null
    ): FailedMessageStats {
        // TODO: 实现从服务获取消息失败统计
        // 目前返回默认统计数据
        return FailedMessageStats(
            totalFailed = 0L,
            failureRate = 0.0,
            commonFailureReasons = emptyList(),
            failureByHour = emptyMap()
        )
    }

    /**
     * 获取用户通知设置统计（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserNotificationSettingsStatistics(): NotificationSettingsStats {
        // TODO: 实现从服务获取用户通知设置统计
        // 目前返回默认统计数据
        return NotificationSettingsStats(
            totalUsers = 0L,
            usersWithOrderUpdates = 0L,
            usersWithPaymentNotifications = 0L,
            usersWithDeliveryNotifications = 0L,
            usersWithPromotionalNotifications = 0L,
            optOutRates = mapOf(
                "orderUpdates" to 0.0,
                "paymentNotifications" to 0.0,
                "deliveryNotifications" to 0.0,
                "promotionalNotifications" to 0.0
            )
        )
    }

    /**
     * 获取批量发送状态（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun getBatchSendStatus(batchId: String): BatchSendStatus? {
        // TODO: 实现从服务获取批量发送状态
        // 目前返回null
        return null
    }

    /**
     * 搜索通知模板（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun searchNotificationTemplates(
        keyword: String,
        messageType: String? = null,
        page: Int = 0,
        size: Int = 20
    ): Page<NotificationTemplate> {
        // TODO: 实现从服务搜索通知模板
        // 目前返回空列表
        return Page.empty(org.springframework.data.domain.PageRequest.of(page, size))
    }

    companion object {
        /**
         * 系统消息相关类型
         */
        data class SystemMessageStats(
            val totalSent: Long,
            val successCount: Long,
            val failureCount: Long,
            val successRate: Double,
            val messagesByType: Map<String, Long>,
            val messagesByHour: Map<String, Long>,
            val topFailureReasons: List<String>
        )

        data class NotificationTemplate(
            val id: Long,
            val name: String,
            val messageType: MessageType,
            val template: String,
            val variables: List<String>,
            val description: String,
            val isActive: Boolean,
            val createdAt: LocalDateTime,
            val updatedAt: LocalDateTime
        )

        data class SystemMessageHistory(
            val id: Long,
            val userId: Long,
            val messageType: MessageType,
            val content: String,
            val status: String,
            val sentAt: LocalDateTime,
            val deliveredAt: LocalDateTime?,
            val failureReason: String?,
            val templateId: Long?
        )

        data class FailedMessageStats(
            val totalFailed: Long,
            val failureRate: Double,
            val commonFailureReasons: List<String>,
            val failureByHour: Map<String, Long>
        )

        data class NotificationSettingsStats(
            val totalUsers: Long,
            val usersWithOrderUpdates: Long,
            val usersWithPaymentNotifications: Long,
            val usersWithDeliveryNotifications: Long,
            val usersWithPromotionalNotifications: Long,
            val optOutRates: Map<String, Double>
        )

        data class BatchSendStatus(
            val batchId: String,
            val totalRecipients: Int,
            val sentCount: Int,
            val successCount: Int,
            val failureCount: Int,
            val status: String,
            val startedAt: LocalDateTime,
            val completedAt: LocalDateTime?
        )
    }
}
