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

package dev.yidafu.aqua.api.service.payment

/**
 * 支付服务接口(PRD 04-order.md §F-ORD-005/R-PAY-01~03)
 */
interface PaymentService {
  /**
   * 创建微信小程序支付订单(JSAPI):
   * 测试环境(假支付模式)返回 mock 参数并自动完成支付;真实模式返回 Taro.requestPayment 参数。
   */
  fun createWechatJsapiPay(
    orderId: Long,
    amountCents: Long,
    description: String,
    openId: String,
  ): Map<String, Any>

  /**
   * 处理微信支付回调:
   * rawBody 为回调原始报文(验签必需),headers 携带 wechatpay-serial/timestamp/nonce/signature;
   * 验签/金额校验失败返回 false(微信将重试),成功返回 true。
   */
  fun handleWechatPayCallback(
    rawBody: String,
    headers: Map<String, String>,
  ): Boolean

  /**
   * 查询支付状态(SUCCESS/CLOSED/UNKNOWN)
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

  /**
   * 创建桶水票购票支付订单(PRD 10-marketing.md §F-MKT-007):
   * orderNo 为购票单号(微信 out_trade_no);购票即预收款项,不支持退款。
   */
  fun createWechatJsapiPayForTicket(
    ticketPurchaseId: Long,
    orderNo: String,
    amountCents: Long,
    description: String,
    openId: String,
  ): Map<String, Any>

  // 保持原有接口兼容性
  fun createWechatPayOrder(
    orderId: Long,
    amountCents: Long,
    description: String,
  ): Map<String, Any>

  fun refund(
    transactionId: String,
    refundAmountCents: Long,
    totalAmountCents: Long,
  ): Boolean
}
