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

package dev.yidafu.aqua.payment.service

/**
 * 微信支付网关抽象(支付域,PRD 04-order.md §F-ORD-005/R-PAY-01):
 * 与 wechatpay-java SDK 模型解耦,便于测试环境使用假支付(Mock)及单元测试注入。
 * 真实实现见 [dev.yidafu.aqua.payment.service.impl.SdkWechatPayGateway]。
 */
interface WechatPayGateway {
  /**
   * 是否已配置真实微信支付商户参数(商户号/私钥/证书序列号/APIv3 密钥/回调地址)。
   * 未配置时业务层应走假支付(测试环境)或提示支付未接入,不应调用真实接口。
   */
  val isConfigured: Boolean

  /**
   * 微信小程序支付下单(JSAPI):返回小程序 Taro.requestPayment 所需参数(含 paySign)。
   */
  fun prepayWithRequestPayment(request: WechatPrepayRequest): WechatPrepayParams

  /**
   * 按商户订单号(out_trade_no)查询微信支付交易;交易不存在返回 null。
   */
  fun queryTransaction(outTradeNo: String): WechatTransaction?

  /**
   * 验签并解密微信支付回调(APIv3 平台证书/RSA),解析出交易信息;
   * 验签失败或解密失败时抛出异常,调用方应拒绝放行(R-PAY-01)。
   */
  fun parseNotification(
    rawBody: String,
    serialNumber: String,
    timestamp: String,
    nonce: String,
    signature: String,
  ): WechatTransaction

  /**
   * 申请退款(V3),返回微信退款单号 refund_id。
   */
  fun createRefund(
    outRefundNo: String,
    transactionId: String,
    refundAmountCents: Long,
    totalAmountCents: Long,
    reason: String,
  ): String
}

/**
 * 微信 JSAPI 预支付请求参数(R-MON-01:金额一律为分)。
 */
data class WechatPrepayRequest(
  val appId: String,
  val mchId: String,
  val description: String,
  val outTradeNo: String,
  val amountCents: Long,
  val openId: String,
  val notifyUrl: String,
)

/**
 * 小程序支付参数(与 Taro.requestPayment 字段一一对应)。
 */
data class WechatPrepayParams(
  val appId: String,
  val timeStamp: String,
  val nonceStr: String,
  val packageVal: String,
  val signType: String,
  val paySign: String,
)

/**
 * 微信支付交易信息(查询/回调解析结果,仅暴露业务所需字段)。
 */
data class WechatTransaction(
  val outTradeNo: String,
  val transactionId: String,
  /** 交易状态:SUCCESS/NOTPAY/CLOSED/PAYERROR/USERPAYING/REVOKED 等 */
  val tradeState: String,
  /** 实付金额(分) */
  val amountCents: Long,
)
