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

package dev.yidafu.aqua.common.graphql

import dev.yidafu.aqua.common.security.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize

/**
 * GraphQL Resolver 基类，提供通用的验证逻辑
 */
abstract class BaseGraphQLResolver {
  private val logger = LoggerFactory.getLogger(this::class.java)

  /**
   * 获取当前认证用户
   * @param userPrincipal 注入的用户主体
   * @return 认证的用户，如果未认证且允许匿名访问则返回 null
   */
  protected fun getCurrentUser(userPrincipal: UserPrincipal?): UserPrincipal? {
    return try {
      userPrincipal ?: GraphQLValidator.getCurrentUser()
    } catch (e: Exception) {
      logger.error("Error getting current user in ${this::class.simpleName}", e)
      null
    }
  }

  /**
   * 要求用户必须认证
   * @param userPrincipal 注入的用户主体
   * @return 认证的用户
   * @throws SecurityException 如果用户未认证
   */
  protected fun requireAuthenticated(userPrincipal: UserPrincipal?): UserPrincipal {
    val currentUser = getCurrentUser(userPrincipal) ?: userPrincipal
    return GraphQLValidator.requireAuthenticated(currentUser)
  }

  /**
   * 检查用户权限（开发环境跳过验证）
   * @param userPrincipal 注入的用户主体
   * @param operation 操作名称，用于日志记录
   */
  protected fun checkPermission(userPrincipal: UserPrincipal?, operation: String) {
    // 在开发环境跳过权限检查，方便调试
    if (GraphQLValidator.isDevelopmentEnvironment()) {
      logger.debug("Development environment - skipping permission check for $operation")
      return
    }

    // 生产环境要求认证
    requireAuthenticated(userPrincipal)
  }

  /**
   * 记录 GraphQL 操作日志
   * @param userPrincipal 用户主体
   * @param operation 操作名称
   * @param parameters 参数（可选）
   */
  protected fun logOperation(
    userPrincipal: UserPrincipal?,
    operation: String,
    parameters: Map<String, Any>? = null
  ) {
    val userId = userPrincipal?.id ?: "anonymous"
    val paramsStr = parameters?.let {
      it.entries.joinToString(", ") { "${it.key}=${it.value}" }
    } ?: "no-parameters"

    logger.info(
      "GraphQL Operation: {} by User {} with parameters: {}",
      operation,
      userId,
      paramsStr
    )
  }
}