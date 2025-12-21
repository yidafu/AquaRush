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

package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.UpdateProfileInput
import dev.yidafu.aqua.common.graphql.generated.User
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.user.domain.repository.UserRepository
import dev.yidafu.aqua.user.mapper.UserMapper
import dev.yidafu.aqua.api.service.UserService
import dev.yidafu.aqua.user.service.WeChatAuthService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 客户端用户查询解析器
 * 提供用户查询功能，用户只能查看自己的信息
 */
@ClientService
@Controller
class ClientUserQueryResolver(
    private val userService: UserService,
    private val weChatAuthService: WeChatAuthService,
    private val userRepository: UserRepository
) {
  private val logger = LoggerFactory.getLogger(ClientUserQueryResolver::class.java)
    /**
     * 获取当前用户信息
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun me(@AuthenticationPrincipal userPrincipal: UserPrincipal): User {
      logger.info("query me info ${userPrincipal.id}")
        return userRepository.findById(userPrincipal.id)
            .orElseThrow { IllegalArgumentException("User not found") }
          .let { UserMapper.map(it) }
    }

    /**
     * 获取用户详细信息（只能查看自己）
     */
    @PreAuthorize("isAuthenticated()")
    fun user(
        id: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): User? {
        // 验证只能查看自己的信息
        if (id != userPrincipal.id) {
            throw IllegalArgumentException("无权查看其他用户信息")
        }

        return userRepository.findById(id)
            .orElseThrow { IllegalArgumentException("User not found") }
          .let { UserMapper.map(it) }
    }

  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun updateProfile(
    @Argument @Valid input: UpdateProfileInput,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?,
  ): User {
    if (userPrincipal == null) {
      throw IllegalStateException("请先登录")
    }
    val userId = userPrincipal.id
    val updatedUser =  userService.updateUserInfo(userId, input.nickname, null, input.avatar)
    // 正常认证用户的处理
    return UserMapper.map(updatedUser)
  }

    /**
     * 获取用户订单统计
     */
    @PreAuthorize("isAuthenticated()")
    fun getUserOrderStatistics(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): UserOrderStats {
        // TODO: 实现从服务获取用户订单统计
        // 目前返回默认统计数据
        return UserOrderStats(
            totalOrders = 0L,
            completedOrders = 0L,
            cancelledOrders = 0L,
            totalAmount = BigDecimal.ZERO,
            averageOrderAmount = BigDecimal.ZERO,
            lastOrderDate = null,
            favoriteProduct = null
        )
    }

    /**
     * 检查用户是否可以评价
     */
    @PreAuthorize("isAuthenticated()")
    fun canUserReview(
        orderId: Long,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Boolean {
        return userRepository.canUserReview(userPrincipal.id, orderId)
    }

    /**
     * 获取用户偏好设置
     */
    @PreAuthorize("isAuthenticated()")
    fun getUserPreferences(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): UserPreferences {
        // TODO: 实现从服务获取用户偏好设置
        // 目前返回默认偏好
        return UserPreferences(
            language = "zh-CN",
            timezone = "Asia/Shanghai",
            currency = "CNY",
            notifications = mapOf(
                "email" to true,
                "sms" to false,
                "push" to true
            )
        )
    }

    companion object {
        data class UserOrderStats(
            val totalOrders: Long,
            val completedOrders: Long,
            val cancelledOrders: Long,
            val totalAmount: BigDecimal,
            val averageOrderAmount: BigDecimal,
            val lastOrderDate: LocalDateTime?,
            val favoriteProduct: String?
        )


        data class UserPreferences(
            val language: String,
            val timezone: String,
            val currency: String,
            val notifications: Map<String, Boolean>
        )
    }
}
