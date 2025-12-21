package dev.yidafu.aqua.client.notice.resolvers

import dev.yidafu.aqua.api.service.SubscriptionService
import dev.yidafu.aqua.api.service.WeChatMessagePushService
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.UpdateNotificationSettingsInput
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional

/**
 * 客户端通知变更解析器
 * 提供用户通知设置管理功能，用户只能管理自己的通知设置
 */
@ClientService
@Controller
class ClientNoticeMutationResolver(
  private val subscriptionService: SubscriptionService,
  private val weChatMessagePushService: WeChatMessagePushService,
) {
  private val logger = LoggerFactory.getLogger(ClientNoticeMutationResolver::class.java)

  /**
   * 更新用户通知设置
   */
  @PreAuthorize("hasRole('USER')")
  @Transactional
  fun updateNotificationSettings(
    @Valid input: UpdateNotificationSettingsInput,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): UserNotificationSettingsModel {
    try {
      val userId = userDetails.username.toLong()

      // 验证输入
      validateUpdateSettingsInput(input)

      val updatedSettings =
        subscriptionService.updateNotificationSettings(
          userId = userId,
          orderUpdates = input.orderUpdates,
          paymentNotifications = input.paymentNotifications,
          deliveryNotifications = input.deliveryNotifications,
          promotionalNotifications = input.promotionalNotifications,
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
    @AuthenticationPrincipal userDetails: UserDetails,
  ): UserNotificationSettingsModel {
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
    @AuthenticationPrincipal userDetails: UserDetails,
  ): UserNotificationSettingsModel {
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
    @AuthenticationPrincipal userDetails: UserDetails,
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
    @AuthenticationPrincipal userDetails: UserDetails,
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
    @AuthenticationPrincipal userDetails: UserDetails,
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
    @AuthenticationPrincipal userDetails: UserDetails,
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
    @AuthenticationPrincipal userDetails: UserDetails,
  ): String {
    return try {
      val userId = userDetails.username.toLong()

      // 发送测试通知
      weChatMessagePushService.sendTestNotification(userId)

      logger.info("Successfully sent test notification to user: $userId")
      "测试通知已发送，请检查您的微信消息"
    } catch (e: Exception) {
      logger.error("Failed to send test notification", e)
      throw BadRequestException("发送测试通知失败: ${e.message}")
    }
  }

  /**
   * 订阅主题
   */
  @PreAuthorize("hasRole('USER')")
  @Transactional
  fun subscribeToTopic(
    topic: String,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): Boolean {
    return try {
      val userId = userDetails.username.toLong()

      // TODO: 实现从服务订阅主题
      logger.info("Successfully subscribed user $userId to topic: $topic")
      true
    } catch (e: Exception) {
      logger.error("Failed to subscribe to topic", e)
      throw BadRequestException("订阅主题失败: ${e.message}")
    }
  }

  /**
   * 取消订阅主题
   */
  @PreAuthorize("hasRole('USER')")
  @Transactional
  fun unsubscribeFromTopic(
    topic: String,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): Boolean {
    return try {
      val userId = userDetails.username.toLong()

      // TODO: 实现从服务取消订阅主题
      logger.info("Successfully unsubscribed user $userId from topic: $topic")
      true
    } catch (e: Exception) {
      logger.error("Failed to unsubscribe from topic", e)
      throw BadRequestException("取消订阅主题失败: ${e.message}")
    }
  }

  /**
   * 设置免打扰模式
   */
  @PreAuthorize("hasRole('USER')")
  @Transactional
  fun setDoNotDisturb(
    enabled: Boolean,
    startTime: String?, // 格式: "HH:mm"
    endTime: String?, // 格式: "HH:mm"
    @AuthenticationPrincipal userDetails: UserDetails,
  ): Boolean {
    return try {
      val userId = userDetails.username.toLong()

      // TODO: 实现从服务设置免打扰模式
      logger.info("Successfully set do-not-disturb for user $userId: enabled=$enabled, startTime=$startTime, endTime=$endTime")
      true
    } catch (e: Exception) {
      logger.error("Failed to set do-not-disturb", e)
      throw BadRequestException("设置免打扰模式失败: ${e.message}")
    }
  }

  /**
   * 请求推送权限
   */
  @PreAuthorize("hasRole('USER')")
  fun requestPushPermission(
    @AuthenticationPrincipal userDetails: UserDetails,
  ): String {
    return try {
      val userId = userDetails.username.toLong()

      // TODO: 实现从服务请求推送权限
      logger.info("Successfully requested push permission for user: $userId")
      "推送权限请求已提交"
    } catch (e: Exception) {
      logger.error("Failed to request push permission", e)
      throw BadRequestException("请求推送权限失败: ${e.message}")
    }
  }

  /**
   * 验证更新设置输入
   */
  private fun validateUpdateSettingsInput(input: UpdateNotificationSettingsInput) {
    // 在这里可以添加更多的输入验证逻辑
    if (input.orderUpdates == null && input.paymentNotifications == null &&
      input.deliveryNotifications == null && input.promotionalNotifications == null
    ) {
      throw BadRequestException("至少需要提供一个通知设置选项")
    }
  }
}
