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
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse
import com.wechat.pay.java.service.payments.model.Transaction
import com.wechat.pay.java.service.payments.model.TransactionAmount
import com.wechat.pay.java.service.refund.RefundService
import com.wechat.pay.java.service.refund.model.Refund
import dev.yidafu.aqua.payment.service.WechatPrepayRequest
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.objenesis.ObjenesisStd
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 微信支付 V3 网关单元测试:
 * - isConfigured 配置完备性判定;
 * - 预支付/查询/回调解析/退款通过 MockK 注入 SDK 服务,不触发真实证书下载与网络调用。
 */
class SdkWechatPayGatewayTest {
  private fun gateway(
    appId: String = "wx-app",
    mchId: String = "mch-123",
    privateKeyPath: String = "/tmp/merchant.key",
    serial: String = "SERIAL-1",
    apiV3Key: String = "v3-key",
    notifyUrl: String = "https://example.com/notify",
  ) = SdkWechatPayGateway(appId, mchId, privateKeyPath, serial, apiV3Key, notifyUrl)

  private fun prepayRequest() =
    WechatPrepayRequest(
      appId = "wx-app",
      mchId = "mch-123",
      description = "桶装水订单",
      outTradeNo = "ORD-001",
      amountCents = 1200,
      openId = "openid-1",
      notifyUrl = "https://example.com/notify",
    )

  /** 替换私有 lazy 委托,注入 mock 服务(避免真实证书下载) */
  private fun <T : Any> injectLazy(
    target: Any,
    fieldName: String,
    value: T,
  ) {
    val field = target::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(target, lazyOf(value))
  }

  /** RSAAutoCertificateConfig 为 final 类,用 Objenesis 无构造创建占位实例(仅作参数传递) */
  private fun fakeConfig(): RSAAutoCertificateConfig = ObjenesisStd().newInstance(RSAAutoCertificateConfig::class.java)

  // ================================================================
  // isConfigured
  // ================================================================

  @Test
  @DisplayName("全部商户参数配置后 isConfigured 为 true")
  fun `isConfigured true when all params present`() {
    assertTrue(gateway().isConfigured)
  }

  @Test
  @DisplayName("任一参数缺失 isConfigured 为 false")
  fun `isConfigured false when any param missing`() {
    assertFalse(gateway(appId = "").isConfigured)
    assertFalse(gateway(mchId = "").isConfigured)
    assertFalse(gateway(privateKeyPath = "").isConfigured)
    assertFalse(gateway(serial = "").isConfigured)
    assertFalse(gateway(apiV3Key = "").isConfigured)
    assertFalse(gateway(notifyUrl = "").isConfigured)
    assertFalse(gateway(appId = "", mchId = "", privateKeyPath = "", serial = "", apiV3Key = "", notifyUrl = "").isConfigured)
  }

  // ================================================================
  // 预支付
  // ================================================================

  @Test
  @DisplayName("prepayWithRequestPayment 返回小程序支付参数")
  fun `prepay returns wechat prepay params`() {
    val jsapiService = mockk<JsapiServiceExtension>()
    val response =
      PrepayWithRequestPaymentResponse().apply {
        appId = "wx-app"
        timeStamp = "1750000000"
        nonceStr = "nonce"
        packageVal = "prepay_id=wx123"
        signType = "RSA"
        paySign = "sign"
      }
    every { jsapiService.prepayWithRequestPayment(any()) } returns response
    val gw = gateway()
    injectLazy(gw, "jsapiService\$delegate", jsapiService)
    injectLazy(gw, "config\$delegate", fakeConfig())

    val params = gw.prepayWithRequestPayment(prepayRequest())

    assertEquals("wx-app", params.appId)
    assertEquals("1750000000", params.timeStamp)
    assertEquals("nonce", params.nonceStr)
    assertEquals("prepay_id=wx123", params.packageVal)
    assertEquals("RSA", params.signType)
    assertEquals("sign", params.paySign)
  }

  // ================================================================
  // 交易查询
  // ================================================================

  @Test
  @DisplayName("queryTransaction 返回交易信息(含状态与金额)")
  fun `queryTransaction maps transaction fields`() {
    val jsapiService = mockk<JsapiServiceExtension>()
    val transaction =
      Transaction().apply {
        outTradeNo = "ORD-001"
        transactionId = "WX-TXN-001"
        tradeState = Transaction.TradeStateEnum.SUCCESS
        amount = TransactionAmount().apply { total = 1200 }
      }
    every { jsapiService.queryOrderByOutTradeNo(any()) } returns transaction
    val gw = gateway()
    injectLazy(gw, "jsapiService\$delegate", jsapiService)
    injectLazy(gw, "config\$delegate", fakeConfig())

    val result = gw.queryTransaction("ORD-001")

    assertEquals("ORD-001", result?.outTradeNo)
    assertEquals("WX-TXN-001", result?.transactionId)
    assertEquals("SUCCESS", result?.tradeState)
    assertEquals(1200L, result?.amountCents)
  }

  @Test
  @DisplayName("queryTransaction 交易不存在返回 null,缺失字段兜底")
  fun `queryTransaction handles null and missing fields`() {
    val jsapiService = mockk<JsapiServiceExtension>()
    every { jsapiService.queryOrderByOutTradeNo(any()) } returns null
    val gw = gateway()
    injectLazy(gw, "jsapiService\$delegate", jsapiService)
    injectLazy(gw, "config\$delegate", fakeConfig())

    assertNull(gw.queryTransaction("MISSING"))

    // 状态与金额缺失时兜底(真实模型 getter 对必填字段做非空校验,使用 mock)
    val txn = mockk<Transaction>()
    every { txn.outTradeNo } returns "X"
    every { txn.transactionId } returns "TXN"
    every { txn.tradeState } returns null
    every { txn.amount } returns null
    every { jsapiService.queryOrderByOutTradeNo(any()) } returns txn
    val result = gw.queryTransaction("X")
    assertEquals("UNKNOWN", result?.tradeState)
    assertEquals(0L, result?.amountCents)
  }

  // ================================================================
  // 退款
  // ================================================================

  @Test
  @DisplayName("createRefund 调用微信退款并返回退款单号")
  fun `createRefund returns refund id`() {
    val refundService = mockk<RefundService>()
    val refund = Refund().apply { refundId = "REF-001" }
    every { refundService.create(any()) } returns refund
    val gw = gateway()
    injectLazy(gw, "refundService\$delegate", refundService)
    injectLazy(gw, "config\$delegate", fakeConfig())

    val refundId = gw.createRefund("OUT-REF-1", "WX-TXN-001", 1000, 1200, "订单取消退款")

    assertEquals("REF-001", refundId)
  }
}
