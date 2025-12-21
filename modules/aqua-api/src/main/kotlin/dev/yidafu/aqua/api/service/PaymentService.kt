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

/**
 * 支付服务接口
 */
interface PaymentService {
  /**
   * 创建微信小程序支付订单
   */
  fun createWechatJsapiPay(
    orderId: Long,
    amountCents: Long,
    description: String,
    openId: String,
  ): Map<String, Any>

  /**
   * 处理微信支付回调
   */
  fun handleWechatPayCallback(
    callbackData: Map<String, Any>,
    headers: Map<String, String>,
  ): Boolean

  /**
   * 查询支付状态
   */
  fun queryPaymentStatus(orderId: Long): String

  /**
   * 申请退款
   */
  fun refund(
    transactionId: String,
    refundAmountCents: Long,
    totalAmountCents: Long,
    reason: String = "订单取消退款",
  ): Map<String, Any>

  /**
   * 处理支付超时
   */
  fun handlePaymentTimeout(orderId: Long)

  // 保持原有接口兼容性
  fun createWechatPayOrder(
    orderId: Long,
    amountCents: Long,
    description: String,
  ): Map<String, Any>

  fun handleWechatPayCallback(callbackData: Map<String, Any>): Boolean

  fun refund(
    transactionId: String,
    refundAmountCents: Long,
    totalAmountCents: Long,
  ): Boolean
}
