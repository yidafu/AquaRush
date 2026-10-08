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

package dev.yidafu.aqua.logging.util

import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import dev.yidafu.aqua.logging.service.BusinessLogService
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * 业务日志记录器，提供便捷的后端业务操作记录方法
 *
 * 注意：此类仅用于记录后端业务操作（如订单创建、支付、审核等）
 * 前端用户行为请使用 [dev.yidafu.aqua.logging.util.UserActionLogger]
 */
@Component
class BizLogger(
  private val businessLogService: BusinessLogService,
) {
  private val logger = LoggerFactory.getLogger("dev.yidafu.aqua.business")

  /**
   * 记录业务操作
   */
  fun logOperation(
    operation: String,
    module: String,
    result: String,
    target: String,
    userId: Long? = null,
    username: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val correlationId = CorrelationIdHolder.getCorrelationId()
    val message =
      buildString {
        append("Operation: $operation, Module: $module, Result: $result, Target: $target")
        if (additionalData.isNotEmpty()) {
          append(", Additional: $additionalData")
        }
      }

    // 保存到数据库
    businessLogService.saveBusinessLog(
      correlationId = correlationId,
      level = if (result == "SUCCESS") "INFO" else "ERROR",
      loggerName = module,
      message = message,
      userId = userId,
      username = username,
      operation = operation,
    )

    // 同时输出到日志
    when (result) {
      "SUCCESS" -> logger.info(message)
      "FAILURE", "ERROR" -> logger.error(message)
      else -> logger.info(message)
    }
  }

  /**
   * 记录订单操作
   */
  fun logOrderOperation(
    orderId: Long,
    operation: String,
    result: String,
    userId: Long? = null,
    username: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    logOperation(
      operation = operation,
      module = "ORDER",
      result = result,
      target = orderId.toString(),
      userId = userId,
      username = username,
      additionalData = additionalData,
    )
  }

  /**
   * 记录支付操作
   */
  fun logPaymentOperation(
    paymentId: Long,
    operation: String,
    result: String,
    userId: Long? = null,
    username: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    logOperation(
      operation = operation,
      module = "PAYMENT",
      result = result,
      target = paymentId.toString(),
      userId = userId,
      username = username,
      additionalData = additionalData,
    )
  }

  /**
   * 记录配送操作
   */
  fun logDeliveryOperation(
    deliveryId: Long,
    operation: String,
    result: String,
    userId: Long? = null,
    username: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    logOperation(
      operation = operation,
      module = "DELIVERY",
      result = result,
      target = deliveryId.toString(),
      userId = userId,
      username = username,
      additionalData = additionalData,
    )
  }

  /**
   * 记录用户管理操作
   */
  fun logUserOperation(
    targetUserId: Long,
    operation: String,
    result: String,
    operatorId: Long? = null,
    operatorName: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    logOperation(
      operation = operation,
      module = "USER",
      result = result,
      target = targetUserId.toString(),
      userId = operatorId,
      username = operatorName,
      additionalData = additionalData,
    )
  }

  /**
   * 记录用户登录操作
   * @param operation 操作类型:USER_LOGIN(小程序用户)/ADMIN_LOGIN(管理员)/LOGIN_FAILED(登录失败),默认 USER_LOGIN
   */
  fun logLogin(
    userId: String,
    username: String,
    loginMethod: String,
    success: Boolean,
    operation: String = "USER_LOGIN",
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    val result = if (success) "SUCCESS" else "FAILURE"
    val target = userId
    logOperation(
      operation = operation,
      module = "AUTH",
      result = result,
      target = target,
      userId = userId.toLongOrNull(),
      username = username,
      additionalData = additionalData + mapOf("loginMethod" to loginMethod),
    )
  }

  /**
   * 记录产品操作(PRODUCT 域)
   */
  fun logProductOperation(
    productId: Long,
    operation: String,
    result: String,
    userId: Long? = null,
    username: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    logOperation(
      operation = operation,
      module = "PRODUCT",
      result = result,
      target = productId.toString(),
      userId = userId,
      username = username,
      additionalData = additionalData,
    )
  }

  /**
   * 记录营销操作(MARKETING 域:优惠券/客户群/活动/水票/分组定价等)
   */
  fun logMarketingOperation(
    targetId: Long,
    operation: String,
    result: String,
    userId: Long? = null,
    username: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    logOperation(
      operation = operation,
      module = "MARKETING",
      result = result,
      target = targetId.toString(),
      userId = userId,
      username = username,
      additionalData = additionalData,
    )
  }

  /**
   * 记录运营配置操作(OPS 域;与 user_action_logs 的 before/after 审计并存,R-OPS-01)
   */
  fun logOpsOperation(
    targetType: String,
    targetId: Long?,
    operation: String,
    result: String,
    operatorId: Long? = null,
    operatorName: String? = null,
    additionalData: Map<String, Any> = emptyMap(),
  ) {
    logOperation(
      operation = operation,
      module = "OPS",
      result = result,
      target = if (targetId != null) "$targetType:$targetId" else targetType,
      userId = operatorId,
      username = operatorName,
      additionalData = additionalData,
    )
  }

  /**
   * 从 SecurityContext 解析当前操作者(admin/client 通用)。
   * 定时任务/事件消费线程无安全上下文时返回 null,日志仅缺失操作人,不阻断业务。
   */
  fun resolveOperator(): Pair<Long?, String?> {
    val authentication = SecurityContextHolder.getContext().authentication ?: return null to null
    val principal = authentication.principal
    return if (principal is UserPrincipal) principal.id to principal.username else null to null
  }
}
