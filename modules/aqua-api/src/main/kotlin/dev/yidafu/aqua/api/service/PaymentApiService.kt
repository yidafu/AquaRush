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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.api.common.PagedResponse
import dev.yidafu.aqua.api.dto.*
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import java.math.BigDecimal
import java.util.*

/**
 * 支付API服务接口
 */
interface PaymentApiService {
  /**
   * 创建支付订单
   */
  fun createPayment(
    orderId: Long,
    amount: BigDecimal,
    paymentMethod: PaymentMethod,
  ): PaymentDTO

  /**
   * 处理支付回调
   */
  fun processPaymentCallback(
    paymentId: Long,
    transactionId: String,
    status: PaymentStatus,
  ): PaymentDTO

  /**
   * 获取支付详情
   */
  fun getPaymentById(paymentId: Long): PaymentDTO?

  /**
   * 获取订单支付记录
   */
  fun getOrderPayments(orderId: Long): List<PaymentDTO>

  /**
   * 获取用户支付记录
   */
  fun getUserPayments(
    userId: Long,
    page: Int = 0,
    size: Int = 20,
  ): PagedResponse<PaymentDTO>

  /**
   * 申请退款
   */
  fun requestRefund(
    paymentId: Long,
    amount: BigDecimal,
    reason: String,
  ): PaymentRefundDTO

  /**
   * 处理退款
   */
  fun processRefund(
    refundId: Long,
    status: RefundStatus,
  ): PaymentRefundDTO

  /**
   * 获取退款详情
   */
  fun getRefundById(refundId: Long): PaymentRefundDTO?

  /**
   * 获取用户退款记录
   */
  fun getUserRefunds(
    userId: Long,
    page: Int = 0,
    size: Int = 20,
  ): PagedResponse<PaymentRefundDTO>

  /**
   * 获取支付统计
   */
  fun getPaymentStatistics(userId: Long): PaymentStatisticsDTO

  /**
   * 验证支付金额
   */
  fun validatePaymentAmount(
    orderId: Long,
    amount: BigDecimal,
  ): Boolean

  /**
   * 生成支付二维码
   */
  fun generatePaymentQRCode(paymentId: Long): String

  /**
   * 检查支付超时
   */
  fun checkPaymentTimeout(): Int
}

/**
 * 支付统计DTO
 */
data class PaymentStatisticsDTO(
  val totalPayments: Int = 0,
  val successfulPayments: Int = 0,
  val failedPayments: Int = 0,
  val totalRefunds: Int = 0,
  val totalAmount: BigDecimal = BigDecimal.ZERO,
  val totalRefundAmount: BigDecimal = BigDecimal.ZERO,
  val successRate: BigDecimal = BigDecimal.ZERO,
)
