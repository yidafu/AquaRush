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

package dev.yidafu.aqua.payment.service.impl

import com.wechat.pay.java.core.RSAAutoCertificateConfig
import com.wechat.pay.java.core.notification.NotificationParser
import com.wechat.pay.java.core.notification.RequestParam
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension
import com.wechat.pay.java.service.payments.jsapi.model.Amount
import com.wechat.pay.java.service.payments.jsapi.model.Payer
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest
import com.wechat.pay.java.service.payments.model.Transaction
import com.wechat.pay.java.service.refund.RefundService
import com.wechat.pay.java.service.refund.model.AmountReq
import com.wechat.pay.java.service.refund.model.CreateRequest
import dev.yidafu.aqua.payment.service.WechatPayGateway
import dev.yidafu.aqua.payment.service.WechatPrepayParams
import dev.yidafu.aqua.payment.service.WechatPrepayRequest
import dev.yidafu.aqua.payment.service.WechatTransaction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 微信支付 V3 网关真实实现(基于 wechatpay-java 0.2.17,PRD 04-order.md §F-ORD-005):
 * - 配置(商户号/商户私钥/证书序列号/APIv3 密钥)全部来自 wechat.pay.* 配置,密钥不落库;
 * - 平台证书由 SDK 自动下载(RSAAutoCertificateConfig),无需手工维护平台证书;
 * - 配置缺失/非法时仅在真实支付被调用时抛异常(惰性构建),测试环境假支付不受影响。
 */
@Service
class SdkWechatPayGateway(
  @Value("\${wechat.pay.appid:}") private val appId: String,
  @Value("\${wechat.pay.mchid:}") private val mchId: String,
  @Value("\${wechat.pay.private-key-path:}") private val privateKeyPath: String,
  @Value("\${wechat.pay.merchant-serial-number:}") private val merchantSerialNumber: String,
  @Value("\${wechat.pay.api-v3-key:}") private val apiV3Key: String,
  @Value("\${wechat.pay.notify-url:}") private val notifyUrl: String,
) : WechatPayGateway {
  private val logger = LoggerFactory.getLogger(SdkWechatPayGateway::class.java)

  override val isConfigured: Boolean =
    appId.isNotBlank() &&
      mchId.isNotBlank() &&
      privateKeyPath.isNotBlank() &&
      merchantSerialNumber.isNotBlank() &&
      apiV3Key.isNotBlank() &&
      notifyUrl.isNotBlank()

  private val config: RSAAutoCertificateConfig by lazy {
    logger.info("初始化微信支付 V3 配置(自动下载平台证书)...")
    RSAAutoCertificateConfig
      .Builder()
      .merchantId(mchId)
      .privateKeyFromPath(privateKeyPath)
      .merchantSerialNumber(merchantSerialNumber)
      .apiV3Key(apiV3Key)
      .build()
  }

  private val jsapiService: JsapiServiceExtension by lazy {
    JsapiServiceExtension.Builder().config(config).build()
  }

  private val refundService: RefundService by lazy {
    RefundService.Builder().config(config).build()
  }

  override fun prepayWithRequestPayment(request: WechatPrepayRequest): WechatPrepayParams {
    val prepayRequest =
      PrepayRequest().apply {
        appid = request.appId
        mchid = request.mchId
        description = request.description
        outTradeNo = request.outTradeNo
        notifyUrl = request.notifyUrl
        amount =
          Amount().apply {
            total = request.amountCents.toInt()
            currency = "CNY"
          }
        payer =
          Payer().apply {
            openid = request.openId
          }
      }
    val response = jsapiService.prepayWithRequestPayment(prepayRequest)
    return WechatPrepayParams(
      appId = response.appId,
      timeStamp = response.timeStamp,
      nonceStr = response.nonceStr,
      packageVal = response.packageVal,
      signType = response.signType,
      paySign = response.paySign,
    )
  }

  override fun parseNotification(
    rawBody: String,
    serialNumber: String,
    timestamp: String,
    nonce: String,
    signature: String,
  ): WechatTransaction {
    val requestParam =
      RequestParam
        .Builder()
        .serialNumber(serialNumber)
        .timestamp(timestamp)
        .nonce(nonce)
        .signature(signature)
        .body(rawBody)
        .build()
    val parser = NotificationParser(config)
    val transaction = parser.parse(requestParam, Transaction::class.java)
    return WechatTransaction(
      outTradeNo = transaction.outTradeNo,
      transactionId = transaction.transactionId,
      tradeState = transaction.tradeState?.name ?: "UNKNOWN",
      amountCents = transaction.amount?.total?.toLong() ?: 0L,
    )
  }

  override fun queryTransaction(outTradeNo: String): WechatTransaction? {
    val transaction =
      jsapiService.queryOrderByOutTradeNo(
        QueryOrderByOutTradeNoRequest().apply {
          mchid = mchId
          this.outTradeNo = outTradeNo
        },
      ) ?: return null
    return WechatTransaction(
      outTradeNo = transaction.outTradeNo,
      transactionId = transaction.transactionId,
      tradeState = transaction.tradeState?.name ?: "UNKNOWN",
      amountCents = transaction.amount?.total?.toLong() ?: 0L,
    )
  }

  override fun createRefund(
    outRefundNo: String,
    transactionId: String,
    refundAmountCents: Long,
    totalAmountCents: Long,
    reason: String,
  ): String {
    val request =
      CreateRequest().apply {
        this.outRefundNo = outRefundNo
        this.transactionId = transactionId
        this.reason = reason
        notifyUrl = notifyUrl
        amount =
          AmountReq().apply {
            refund = refundAmountCents
            total = totalAmountCents
            currency = "CNY"
          }
      }
    val refund = refundService.create(request)
    return refund.refundId
  }
}
