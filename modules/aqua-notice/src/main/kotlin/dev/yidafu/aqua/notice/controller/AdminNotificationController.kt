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

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.notice.service.WeChatMessagePushService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
class AdminNotificationController(
    private val weChatMessagePushService: WeChatMessagePushService
) {
    private val logger = LoggerFactory.getLogger(AdminNotificationController::class.java)

    @PostMapping("/retry-failed")
    fun retryFailedMessages(): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            val retryCount = weChatMessagePushService.retryFailedMessages()
            val result = mapOf(
                "retryCount" to retryCount,
                "timestamp" to LocalDateTime.now()
            )
            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: Exception) {
            logger.error("Failed to retry messages", e)
            ResponseEntity.internalServerError()
                .body(ApiResponse.error<Map<String, Any>>("Failed to retry messages: ${e.message}"))
        }
    }

    @GetMapping("/statistics")
    fun getSystemMessageStatistics(
        @RequestParam(required = false) since: LocalDateTime?
    ): ResponseEntity<ApiResponse<Map<String, Long>>> {
        val sinceDate = since ?: LocalDateTime.now().minusDays(7)
        val statistics = weChatMessagePushService.getMessageStatistics(sinceDate)
        return ResponseEntity.ok(ApiResponse.success(statistics))
    }

    @PostMapping("/broadcast")
    fun broadcastMessage(
        @Valid @RequestBody request: BroadcastMessageRequest
    ): ResponseEntity<ApiResponse<Map<String, Any>>> {
        return try {
            // This would typically send a message to all users or a specific segment
            // Implementation depends on your user management system
            logger.info("Broadcast message requested: type=${request.messageType}, templateData=${request.templateData}")

            // For now, return a placeholder response
            val result = mapOf(
                "messageType" to request.messageType,
                "userCount" to 0, // Would be populated based on actual user count
                "timestamp" to LocalDateTime.now()
            )
            ResponseEntity.ok(ApiResponse.success(result))
        } catch (e: Exception) {
            logger.error("Failed to broadcast message", e)
            ResponseEntity.internalServerError()
                .body(ApiResponse.error<Map<String, Any>>("Failed to broadcast message: ${e.message}"))
        }
    }
}

data class BroadcastMessageRequest(
    val messageType: String,
    val templateData: Map<String, String>,
    val userSegment: String? = null // Optional user segmentation
)
