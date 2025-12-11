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
import org.springframework.security.core.context.SecurityContextHolder

/**
 * GraphQL 查询验证帮助类
 */
object GraphQLValidator {
  private val logger = LoggerFactory.getLogger(GraphQLValidator::class.java)

  /**
   * 检查当前用户是否已认证
   * @return UserPrincipal 如果已认证，null 如果未认证
   */
  fun getCurrentUser(): UserPrincipal? {
    return try {
      val authentication = SecurityContextHolder.getContext().authentication
      if (authentication?.principal is UserPrincipal) {
        authentication.principal as UserPrincipal
      } else {
        logger.warn("User not authenticated or invalid principal type")
        null
      }
    } catch (e: Exception) {
      logger.error("Error getting current user", e)
      null
    }
  }

  /**
   * 验证用户是否已认证，如果未认证则抛出异常
   * @param userPrincipal 注入的用户主体
   * @return UserPrincipal 认证的用户
   * @throws SecurityException 如果用户未认证
   */
  fun requireAuthenticated(userPrincipal: UserPrincipal?): UserPrincipal {
    if (userPrincipal == null) {
      logger.warn("Unauthorized access attempt - no user principal found")
      throw SecurityException("用户未认证，请先登录")
    }
    return userPrincipal
  }

  /**
   * 验证用户是否有指定的角色
   * @param userPrincipal 用户主体
   * @param requiredRole 需要的角色
   * @return 是否有权限
   */
  fun hasRole(
    userPrincipal: UserPrincipal?,
    requiredRole: String,
  ): Boolean {
    if (userPrincipal == null) return false
    return userPrincipal.authorities.any { authority -> authority.authority == requiredRole }
  }

  /**
   * 验证用户是否有指定的角色，如果没有则抛出异常
   * @param userPrincipal 用户主体
   * @param requiredRole 需要的角色
   * @throws SecurityException 如果用户没有指定角色
   */
  fun requireRole(
    userPrincipal: UserPrincipal?,
    requiredRole: String,
  ) {
    if (!hasRole(userPrincipal, requiredRole)) {
      logger.warn("Access denied - user {} lacks required role {}", userPrincipal?.id, requiredRole)
      throw SecurityException("权限不足，需要角色: $requiredRole")
    }
  }

  /**
   * 检查是否为开发环境，用于调试权限控制
   * @return 是否为开发环境
   */
  fun isDevelopmentEnvironment(): Boolean {
    // 检查系统属性和环境变量中的Spring profiles配置
    val activeProfiles = System.getProperty("spring.profiles.active") ?: ""
    val environmentProfiles = System.getenv("SPRING_PROFILES_ACTIVE") ?: ""

    return activeProfiles.contains("dev") || activeProfiles.contains("development") ||
      environmentProfiles.contains("dev") || environmentProfiles.contains("development")
  }
}
