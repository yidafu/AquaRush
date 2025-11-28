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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.notice.controller

import dev.yidafu.aqua.common.dto.PageRequest
import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.notice.domain.model.MessageHistory
import dev.yidafu.aqua.notice.domain.model.UserNotificationSettings
import dev.yidafu.aqua.notice.service.MessageHistoryService
import dev.yidafu.aqua.notice.service.SubscriptionService
import dev.yidafu.aqua.notice.service.WeChatMessagePushService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest as SpringPageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('USER')")
class NotificationController(
    private val subscriptionService: SubscriptionService,
    private val messageHistoryService: MessageHistoryService,
    private val weChatMessagePushService: WeChatMessagePushService
) {
    private val logger = LoggerFactory.getLogger(NotificationController::class.java)

    @GetMapping("/settings")
    fun getUserNotificationSettings(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserNotificationSettings>> {
        val userId = userDetails.username.toLong()
        val settings = subscriptionService.getUserNotificationSettings(userId)
        return ResponseEntity.ok(ApiResponse.success(settings))
    }

    @PutMapping("/settings")
    fun updateNotificationSettings(
        @Valid @RequestBody request: UpdateNotificationSettingsRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserNotificationSettings>> {
        val userId = userDetails.username.toLong()
        val settings = subscriptionService.updateNotificationSettings(
            userId = userId,
            orderUpdates = request.orderUpdates,
            paymentNotifications = request.paymentNotifications,
            deliveryNotifications = request.deliveryNotifications,
            promotionalNotifications = request.promotionalNotifications
        )
        return ResponseEntity.ok(ApiResponse.success(settings))
    }

    @PutMapping("/settings/enable-all")
    fun enableAllNotifications(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserNotificationSettings>> {
        val userId = userDetails.username.toLong()
        val settings = subscriptionService.enableAllNotifications(userId)
        return ResponseEntity.ok(ApiResponse.success(settings))
    }

    @PutMapping("/settings/disable-all")
    fun disableAllNotifications(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<UserNotificationSettings>> {
        val userId = userDetails.username.toLong()
        val settings = subscriptionService.disableAllNotifications(userId)
        return ResponseEntity.ok(ApiResponse.success(settings))
    }

    @GetMapping("/history")
    fun getMessageHistory(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Page<MessageHistory>>> {
        val userId = userDetails.username.toLong()
        val pageable = SpringPageRequest.of(page, size)
        val history = messageHistoryService.findByUserId(userId, pageable)
        return ResponseEntity.ok(ApiResponse.success(history))
    }

    @GetMapping("/statistics")
    fun getMessageStatistics(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val userId = userDetails.username.toLong()
        val statistics = messageHistoryService.getMessageStatistics(userId)
        return ResponseEntity.ok(ApiResponse.success(statistics))
    }

    @GetMapping("/check-enabled")
    fun isNotificationEnabled(
        @RequestParam messageType: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Boolean>> {
        val userId = userDetails.username.toLong()
        val messageTypeEnum = dev.yidafu.aqua.notice.domain.model.MessageType.fromString(messageType)
        val enabled = subscriptionService.isNotificationEnabled(userId, messageTypeEnum)
        return ResponseEntity.ok(ApiResponse.success(enabled))
    }

    @PostMapping("/test")
    fun testNotification(
        @RequestBody request: TestNotificationRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ApiResponse<Boolean>> {
        val userId = userDetails.username.toLong()

        return try {
            val messageTypeEnum = dev.yidafu.aqua.notice.domain.model.MessageType.fromString(request.messageType)
            val testOpenId = "test_open_id_${userId}"

            val future = weChatMessagePushService.sendMessage(
                userId = userId,
                openId = testOpenId,
                messageType = messageTypeEnum,
                templateData = request.templateData
            )

            val result = future.get()
            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: Exception) {
            logger.error("Failed to send test notification", e)
            ResponseEntity.ok(ApiResponse.success(false))
        }
    }
}

data class UpdateNotificationSettingsRequest(
    val orderUpdates: Boolean? = null,
    val paymentNotifications: Boolean? = null,
    val deliveryNotifications: Boolean? = null,
    val promotionalNotifications: Boolean? = null
)

data class TestNotificationRequest(
    val messageType: String,
    val templateData: Map<String, String>
)
