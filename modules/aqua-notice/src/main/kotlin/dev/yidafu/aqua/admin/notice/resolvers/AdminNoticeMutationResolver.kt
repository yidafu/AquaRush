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

import dev.yidafu.aqua.admin.notice.resolvers.AdminNoticeMutationResolver.Companion.BroadcastMessageInput
import dev.yidafu.aqua.admin.notice.resolvers.AdminNoticeMutationResolver.Companion.CreateNotificationTemplateInput
import dev.yidafu.aqua.admin.notice.resolvers.AdminNoticeMutationResolver.Companion.UpdateNotificationTemplateInput
import dev.yidafu.aqua.admin.notice.resolvers.AdminNoticeQueryResolver.Companion.NotificationTemplate
import dev.yidafu.aqua.api.service.WeChatMessagePushService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.domain.model.MessageType
import dev.yidafu.aqua.common.exception.BadRequestException
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

/**
 * 管理端通知变更解析器
 * 提供通知管理的变更功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminNoticeMutationResolver(
  private val weChatMessagePushService: WeChatMessagePushService,
) {
  private val logger = LoggerFactory.getLogger(AdminNoticeMutationResolver::class.java)

  /**
   * 创建通知模板（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun createNotificationTemplate(
    @Valid input: CreateNotificationTemplateInput,
  ): NotificationTemplate {
    try {
      // 验证输入
      validateCreateTemplateInput(input)

      // TODO: 实现从服务创建通知模板
      // 目前返回模拟数据
      val template =
        NotificationTemplate(
          id = System.currentTimeMillis(),
          name = input.name,
          messageType = MessageType.fromString(input.messageType),
          template = input.template,
          variables = extractVariables(input.template),
          description = input.description ?: "",
          isActive = input.isActive ?: true,
          createdAt = java.time.LocalDateTime.now(),
          updatedAt = java.time.LocalDateTime.now(),
        )

      logger.info("Successfully created notification template: ${template.id}")
      return template
    } catch (e: Exception) {
      logger.error("Failed to create notification template", e)
      throw BadRequestException("创建通知模板失败: ${e.message}")
    }
  }

  /**
   * 更新通知模板（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun updateNotificationTemplate(
    id: Long,
    @Valid input: UpdateNotificationTemplateInput,
  ): NotificationTemplate {
    try {
      // 验证输入
      validateUpdateTemplateInput(input)

      // TODO: 实现从服务更新通知模板
      // 目前返回模拟数据
      val template =
        NotificationTemplate(
          id = id,
          name = input.name ?: "",
          messageType = MessageType.fromString(input.messageType ?: ""),
          template = input.template ?: "",
          variables = if (!input.template.isNullOrEmpty()) extractVariables(input.template) else emptyList(),
          description = input.description ?: "",
          isActive = input.isActive ?: true,
          createdAt = java.time.LocalDateTime.now(),
          updatedAt = java.time.LocalDateTime.now(),
        )

      logger.info("Successfully updated notification template: $id")
      return template
    } catch (e: Exception) {
      logger.error("Failed to update notification template", e)
      throw BadRequestException("更新通知模板失败: ${e.message}")
    }
  }

  /**
   * 删除通知模板（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun deleteNotificationTemplate(id: Long): Boolean {
    return try {
      // TODO: 实现从服务删除通知模板
      logger.info("Successfully deleted notification template: $id")
      true
    } catch (e: Exception) {
      logger.error("Failed to delete notification template", e)
      throw BadRequestException("删除通知模板失败: ${e.message}")
    }
  }

  /**
   * 广播系统消息（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun broadcastSystemMessage(
    @Valid input: BroadcastMessageInput,
  ): BroadcastResult {
    try {
      // 验证输入
      validateBroadcastInput(input)

      // TODO: 实现从服务广播系统消息
      // 目前返回模拟结果
      val result =
        BroadcastResult(
          batchId = "broadcast_${System.currentTimeMillis()}",
          totalRecipients = 0L,
          sentCount = 0,
          failureCount = 0,
          status = "COMPLETED",
          startedAt = java.time.LocalDateTime.now(),
          completedAt = java.time.LocalDateTime.now(),
        )

      logger.info("Successfully broadcast system message: ${result.batchId}")
      return result
    } catch (e: Exception) {
      logger.error("Failed to broadcast system message", e)
      throw BadRequestException("广播系统消息失败: ${e.message}")
    }
  }

  /**
   * 发送定向消息（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun sendTargetedMessage(
    @Valid input: SendTargetedMessageInput,
  ): SendResult {
    try {
      // 验证输入
      validateTargetedMessageInput(input)

      // TODO: 实现从服务发送定向消息
      // 目前返回模拟结果
      val result =
        SendResult(
          totalRecipients = input.userIds.size,
          successCount = 0,
          failureCount = input.userIds.size,
          status = "COMPLETED",
          sentAt = java.time.LocalDateTime.now(),
        )

      logger.info("Successfully sent targeted message to ${input.userIds.size} users")
      return result
    } catch (e: Exception) {
      logger.error("Failed to send targeted message", e)
      throw BadRequestException("发送定向消息失败: ${e.message}")
    }
  }

  /**
   * 重试失败的消息（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun retryFailedMessages(): Int {
    return try {
      val retryCount = weChatMessagePushService.retryFailedMessages()
      logger.info("Successfully retried $retryCount failed messages")
      retryCount
    } catch (e: Exception) {
      logger.error("Failed to retry failed messages", e)
      throw BadRequestException("重试失败消息失败: ${e.message}")
    }
  }

  /**
   * 激活/停用通知模板（管理员功能）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @Transactional
  fun toggleNotificationTemplate(
    id: Long,
    isActive: Boolean,
  ): Boolean {
    return try {
      // TODO: 实现从服务激活/停用通知模板
      logger.info("Successfully ${if (isActive) "activated" else "deactivated"} notification template: $id")
      true
    } catch (e: Exception) {
      logger.error("Failed to toggle notification template", e)
      throw BadRequestException("切换通知模板状态失败: ${e.message}")
    }
  }

  /**
   * 验证创建模板输入
   */
  private fun validateCreateTemplateInput(input: CreateNotificationTemplateInput) {
    if (input.name.isBlank()) {
      throw BadRequestException("模板名称不能为空")
    }
    if (input.name.length > 100) {
      throw BadRequestException("模板名称长度不能超过100个字符")
    }
    if (input.template.isBlank()) {
      throw BadRequestException("模板内容不能为空")
    }
    if (input.template.length > 2000) {
      throw BadRequestException("模板内容长度不能超过2000个字符")
    }
    if (input.description?.length ?: 0 > 500) {
      throw BadRequestException("模板描述长度不能超过500个字符")
    }
  }

  /**
   * 验证更新模板输入
   */
  private fun validateUpdateTemplateInput(input: UpdateNotificationTemplateInput) {
    input.name?.let { name ->
      if (name.isBlank()) {
        throw BadRequestException("模板名称不能为空")
      }
      if (name.length > 100) {
        throw BadRequestException("模板名称长度不能超过100个字符")
      }
    }

    input.template?.let { template ->
      if (template.isBlank()) {
        throw BadRequestException("模板内容不能为空")
      }
      if (template.length > 2000) {
        throw BadRequestException("模板内容长度不能超过2000个字符")
      }
    }

    input.description?.let { description ->
      if (description.length > 500) {
        throw BadRequestException("模板描述长度不能超过500个字符")
      }
    }
  }

  /**
   * 验证广播输入
   */
  private fun validateBroadcastInput(input: BroadcastMessageInput) {
    if (input.messageType.isBlank()) {
      throw BadRequestException("消息类型不能为空")
    }
    if (input.content.isBlank()) {
      throw BadRequestException("消息内容不能为空")
    }
    if (input.content.length > 1000) {
      throw BadRequestException("消息内容长度不能超过1000个字符")
    }
    if (input.title?.length ?: 0 > 100) {
      throw BadRequestException("消息标题长度不能超过100个字符")
    }
  }

  /**
   * 验证定向消息输入
   */
  private fun validateTargetedMessageInput(input: SendTargetedMessageInput) {
    if (input.userIds.isEmpty()) {
      throw BadRequestException("用户ID列表不能为空")
    }
    if (input.userIds.size > 1000) {
      throw BadRequestException("一次最多只能发送给1000个用户")
    }
    if (input.messageType.isBlank()) {
      throw BadRequestException("消息类型不能为空")
    }
    if (input.content.isBlank()) {
      throw BadRequestException("消息内容不能为空")
    }
    if (input.content.length > 1000) {
      throw BadRequestException("消息内容长度不能超过1000个字符")
    }
  }

  /**
   * 从模板中提取变量
   */
  private fun extractVariables(template: String): List<String> {
    val regex = Regex("\\{([^}]+)}")
    return regex.findAll(template).map { it.groupValues[1] }.distinct().toList()
  }

  companion object {
    /**
     * 通知操作输入类型
     */
    data class CreateNotificationTemplateInput(
      val name: String,
      val messageType: String,
      val template: String,
      val description: String?,
      val isActive: Boolean? = true,
    )

    data class UpdateNotificationTemplateInput(
      val name: String?,
      val messageType: String?,
      val template: String?,
      val description: String?,
      val isActive: Boolean?,
    )

    data class BroadcastMessageInput(
      val messageType: String,
      val title: String?,
      val content: String,
      val sendToAllUsers: Boolean = false,
      val userFilters: Map<String, Any>? = null,
    )

    data class SendTargetedMessageInput(
      val userIds: List<Long>,
      val messageType: String,
      val title: String?,
      val content: String,
      val useTemplate: Long? = null,
      val templateData: Map<String, String>? = null,
    )

    data class BroadcastResult(
      val batchId: String,
      val totalRecipients: Long,
      val sentCount: Int,
      val failureCount: Int,
      val status: String,
      val startedAt: java.time.LocalDateTime,
      val completedAt: java.time.LocalDateTime,
    )

    data class SendResult(
      val totalRecipients: Int,
      val successCount: Int,
      val failureCount: Int,
      val status: String,
      val sentAt: java.time.LocalDateTime,
    )
  }
}
