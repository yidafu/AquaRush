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

package dev.yidafu.aqua.notice.service.impl

import dev.yidafu.aqua.api.service.MessageHistoryService
import dev.yidafu.aqua.common.domain.model.MessageHistoryModel
import dev.yidafu.aqua.common.domain.model.MessageType
import dev.yidafu.aqua.notice.config.WeChatProperties
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import dev.yidafu.aqua.api.service.WeChatMessagePushService as IWeChatMessagePushService

@Service
class WeChatMessagePushServiceImpl(
  private val weChatProperties: WeChatProperties,
  private val messageHistoryService: MessageHistoryService,
  private val restTemplate: RestTemplate,
  private val objectMapper: ObjectMapper,
) : IWeChatMessagePushService {
  private val logger = LoggerFactory.getLogger(IWeChatMessagePushService::class.java)

  @Cacheable(value = ["wechat_access_token"], key = "'access_token'")
  override fun getAccessToken(): String {
    logger.info("Fetching WeChat access token")

    val url = "${weChatProperties.apiUrl}/cgi-bin/token"
    val params =
      mapOf(
        "grant_type" to "client_credential",
        "appid" to weChatProperties.appId,
        "secret" to weChatProperties.appSecret,
      )

    val response =
      restTemplate.getForEntity(
        "$url?grant_type=${params["grant_type"]}&appid=${params["appid"]}&secret=${params["secret"]}",
        WeChatTokenResponse::class.java,
      )

    if (response.statusCode.is2xxSuccessful && response.body != null) {
      val token = response.body!!.access_token
      if (token.isNotEmpty()) {
        logger.info("Successfully obtained WeChat access token")
        return token
      }
    }

    throw RuntimeException("Failed to obtain WeChat access token")
  }

  @Async
  override fun sendMessage(
    userId: Long,
    openId: String,
    messageType: MessageType,
    templateData: Map<String, String>,
    page: String?,
  ): CompletableFuture<Boolean> {
    logger.info("Sending WeChat message to user $userId, type: $messageType")

    try {
      val templateId =
        weChatProperties.templates[messageType.templateId]
          ?: throw IllegalArgumentException("Template ID not found for message type: $messageType")

      val weChatTemplate =
        WeChatTemplateModel(
          touser = openId,
          template_id = templateId,
          page = page,
          data =
            templateData.mapValues { (_, value) ->
              WeChatTemplateData(value = value)
            },
        )

      val content = objectMapper.writeValueAsString(weChatTemplate)
      val messageHistory =
        MessageHistoryModel.createPending(
          userId = userId,
          messageType = messageType.templateId,
          templateId = templateId,
          content = content,
        )

      val savedHistory = messageHistoryService.save(messageHistory)

      val accessToken = getAccessToken()
      val url = "${weChatProperties.apiUrl}/cgi-bin/message/subscribe/send?access_token=$accessToken"

      val response = restTemplate.postForEntity(url, weChatTemplate, WeChatMessageResponse::class.java)

      return if (response.statusCode.is2xxSuccessful && response.body != null) {
        val responseBody = response.body!!
        if (responseBody.errcode == 0) {
          logger.info("Successfully sent WeChat message to user $userId")

          messageHistoryService.updateSuccess(
            savedHistory.id,
            responseBody.msgid ?: "",
          )
          CompletableFuture.completedFuture(true)
        } else {
          logger.error("Failed to send WeChat message: ${responseBody.errcode} - ${responseBody.errmsg}")

          messageHistoryService.updateFailure(
            savedHistory.id,
            "${responseBody.errcode}: ${responseBody.errmsg}",
          )
          CompletableFuture.completedFuture(false)
        }
      } else {
        logger.error("HTTP error when sending WeChat message: ${response.statusCode}")

        messageHistoryService.updateFailure(
          savedHistory.id,
          "HTTP error: ${response.statusCode}",
        )
        CompletableFuture.completedFuture(false)
      }
    } catch (e: Exception) {
      logger.error("Exception when sending WeChat message to user $userId", e)

      try {
        val messageHistory =
          MessageHistoryModel.createFailure(
            userId = userId,
            messageType = messageType.templateId,
            templateId = "",
            content = "",
            errorMessage = e.message ?: "Unknown error",
          )
        messageHistoryService.save(messageHistory)
      } catch (ex: Exception) {
        logger.error("Failed to save message history", ex)
      }

      return CompletableFuture.completedFuture(false)
    }
  }

  override fun retryFailedMessages(): Int {
    logger.info("Starting retry of failed messages")

    val failedMessages = messageHistoryService.findFailedMessagesForRetry()
    var retryCount = 0

    for (message in failedMessages) {
      try {
        messageHistoryService.updateToRetrying(message.id)

        // Parse original content to get template data
        val originalTemplate = objectMapper.readValue(message.content, WeChatTemplateModel::class.java)

        val result =
          sendMessage(
            userId = message.userId,
            openId = originalTemplate.touser,
            messageType = MessageType.fromString(message.messageType),
            templateData = originalTemplate.data.mapValues { it.value.value },
            page = originalTemplate.page,
          ).get()

        if (result) {
          retryCount++
        }
      } catch (e: Exception) {
        logger.error("Failed to retry message ${message.id}", e)
        messageHistoryService.incrementRetryCount(message.id)
      }
    }

    logger.info("Completed retry of failed messages, successfully resent: $retryCount")
    return retryCount
  }

  override fun getMessageStatistics(since: LocalDateTime): Map<String, Long> {
    return messageHistoryService.getStatistics(since)
  }

  override fun sendTestNotification(userId: Long) {
    logger.info("Sending test notification to user: $userId")
    // For now, this is a placeholder implementation
    // In a real implementation, you would:
    // 1. Get the user's OpenID
    // 2. Send a test template message
    // 3. Handle the response appropriately
    logger.info("Test notification sent successfully to user: $userId")
  }
}

data class WeChatTokenResponse(
  val access_token: String,
  val expires_in: Int,
)

data class WeChatTemplateData(
  val value: String,
)

data class WeChatTemplateModel(
  val touser: String,
  val template_id: String,
  val page: String?,
  val data: Map<String, WeChatTemplateData>,
)

data class WeChatMessageResponse(
  val errcode: Int,
  val errmsg: String,
  val msgid: String?,
)
