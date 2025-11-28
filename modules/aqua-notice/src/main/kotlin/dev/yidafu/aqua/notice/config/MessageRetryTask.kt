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

package dev.yidafu.aqua.notice.config

import dev.yidafu.aqua.notice.service.WeChatMessagePushService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class MessageRetryTask(
    private val weChatMessagePushService: WeChatMessagePushService
) {
    private val logger = LoggerFactory.getLogger(MessageRetryTask::class.java)

    @Scheduled(fixedDelayString = "PT10M") // Run every 10 minutes
    fun retryFailedMessages() {
        logger.info("Starting scheduled retry of failed messages")

        try {
            val retryCount = weChatMessagePushService.retryFailedMessages()
            logger.info("Completed scheduled retry, successfully resent $retryCount messages")
        } catch (e: Exception) {
            logger.error("Failed to retry messages", e)
        }
    }
}
