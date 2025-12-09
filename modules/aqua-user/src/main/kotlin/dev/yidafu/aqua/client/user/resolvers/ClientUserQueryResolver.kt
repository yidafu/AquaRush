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

import dev.yidafu.aqua.client.user.resolvers.ClientUserQueryResolver.Companion.UserBalanceInfo
import dev.yidafu.aqua.client.user.resolvers.ClientUserQueryResolver.Companion.UserOrderStats
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.User
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.model.UserModel
import dev.yidafu.aqua.user.domain.repository.UserRepository
import dev.yidafu.aqua.user.mapper.UserMapper
import dev.yidafu.aqua.user.service.UserInfo
import dev.yidafu.aqua.user.service.UserService
import dev.yidafu.aqua.user.service.WeChatAuthService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
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

    /**
     * 获取当前用户信息
     */
    @PreAuthorize("isAuthenticated()")
    fun me(@AuthenticationPrincipal userPrincipal: UserPrincipal): User {
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

    /**
     * 获取用户余额信息
     */
    @PreAuthorize("isAuthenticated()")
    fun getUserBalance(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): UserBalanceInfo {
        val currentBalance = BigDecimal(userRepository.getUserBalance(userPrincipal.id))

        return UserBalanceInfo(
            currentBalance = currentBalance,
            availableBalance = currentBalance, // 假设所有余额都可用
            frozenAmount = BigDecimal.ZERO,
            totalRecharge = BigDecimal.ZERO, // TODO: 从服务获取实际数据
            totalSpent = BigDecimal(userRepository.getUserTotalSpent(userPrincipal.id)),
            lastUpdatedAt = LocalDateTime.now()
        )
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
     * 获取用户消费记录
     */
    @PreAuthorize("isAuthenticated()")
    fun getUserSpendingHistory(
        page: Int = 0,
        size: Int = 20,
        dateFrom: LocalDateTime? = null,
        dateTo: LocalDateTime? = null,
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Page<SpendingRecord> {
        // TODO: 实现从服务获取用户消费记录
        // 目前返回空列表
        val pageable = PageRequest.of(page, size)
        return Page.empty(pageable)
    }

    /**
     * 获取用户收货地址数量
     */
    @PreAuthorize("isAuthenticated()")
    fun getUserAddressCount(
        @AuthenticationPrincipal userPrincipal: UserPrincipal
    ): Int {
        return userRepository.getUserAddressCount(userPrincipal.id)
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
        /**
         * 用户相关类型定义
         */
        data class UserBalanceInfo(
            val currentBalance: BigDecimal,
            val availableBalance: BigDecimal,
            val frozenAmount: BigDecimal,
            val totalRecharge: BigDecimal,
            val totalSpent: BigDecimal,
            val lastUpdatedAt: LocalDateTime
        )

        data class UserOrderStats(
            val totalOrders: Long,
            val completedOrders: Long,
            val cancelledOrders: Long,
            val totalAmount: BigDecimal,
            val averageOrderAmount: BigDecimal,
            val lastOrderDate: LocalDateTime?,
            val favoriteProduct: String?
        )

        data class SpendingRecord(
            val id: Long,
            val orderId: Long,
            val amount: BigDecimal,
            val type: String, // PURCHASE, REFUND, RECHARGE
            val description: String,
            val createdAt: LocalDateTime,
            val relatedOrderId: Long?
        )

        data class UserPreferences(
            val language: String,
            val timezone: String,
            val currency: String,
            val notifications: Map<String, Boolean>
        )
    }
}
