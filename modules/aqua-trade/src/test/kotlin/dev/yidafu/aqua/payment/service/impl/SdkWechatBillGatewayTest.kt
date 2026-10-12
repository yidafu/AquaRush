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

import com.wechat.pay.java.core.exception.ServiceException
import com.wechat.pay.java.core.http.HttpMethod
import com.wechat.pay.java.core.http.HttpRequest
import com.wechat.pay.java.service.billdownload.BillDownloadServiceExtension
import com.wechat.pay.java.service.billdownload.DigestBillEntity
import com.wechat.pay.java.service.billdownload.model.HashType
import dev.yidafu.aqua.payment.service.WechatBillType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.time.LocalDate

/**
 * 微信对账单网关单元测试:
 * - isConfigured 配置完备性判定;
 * - NO_STATEMENT_EXIST → 空账单(合法状态)、STATEMENT_CREATING → 抛异常;
 * - SHA1 哈希校验失败拒绝解析;
 * - 通过反射注入 mock SDK 服务,不触发真实网络与证书下载(同 SdkWechatPayGatewayTest 模式)。
 */
class SdkWechatBillGatewayTest :
  FunSpec({
    val date = LocalDate.of(2025, 6, 1)
    val billCsv =
      """
      交易时间,公众账号ID,商户号,特约商户号,设备号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率,订单金额,申请退款金额,费率备注
      `2025-06-01 10:00:00,`wx-appid,`1900009601,`0,`,`42000001,`OUT_1,`openid1,`JSAPI,`SUCCESS,`CMB_CREDIT,`CNY,`8.88,`0.00,`0,`0,`0.00,`0.00,`,`,`矿泉水,`,`0.06,`0.60%,`8.88,`0.00,`
      总交易单数,应结订单总金额,退款总金额,充值券退款总金额,手续费总金额,订单总金额,申请退款总金额
      `1,`8.88,`0.00,`0.00,`0.06,`8.88,`0.00
      """.trimIndent()

    fun gateway(
      mchId: String = "1900009601",
      privateKeyPath: String = "/tmp/apiclient_key.pem",
      serialNumber: String = "SERIAL-1",
      apiV3Key: String = "v3-key",
    ) = SdkWechatBillGateway(mchId, privateKeyPath, serialNumber, apiV3Key)

    fun <T : Any> injectLazy(
      target: Any,
      fieldName: String,
      value: T,
    ) {
      val field = target::class.java.getDeclaredField(fieldName)
      field.isAccessible = true
      field.set(target, lazyOf(value))
    }

    fun sha1Hex(content: String): String =
      MessageDigest
        .getInstance("SHA-1")
        .digest(content.toByteArray())
        .joinToString("") { "%02x".format(it) }

    fun errorRequest(): HttpRequest =
      HttpRequest
        .Builder()
        .httpMethod(HttpMethod.GET)
        .url("https://api.mch.weixin.qq.com/v3/bill/tradebill")
        .build()

    fun serviceException(code: String): ServiceException = ServiceException(errorRequest(), 400, """{"code":"$code","message":"$code"}""")

    test("isConfigured true when all params present") {
      gateway().isConfigured shouldBe true
    }

    test("isConfigured false when any param missing") {
      gateway(mchId = "").isConfigured shouldBe false
      gateway(privateKeyPath = "").isConfigured shouldBe false
      gateway(serialNumber = "").isConfigured shouldBe false
      gateway(apiV3Key = "").isConfigured shouldBe false
    }

    test("downloadTradeBill throws IllegalStateException when not configured") {
      shouldThrow<IllegalStateException> { gateway(mchId = "").downloadTradeBill(date) }
    }

    test("NO_STATEMENT_EXIST maps to empty bill") {
      val gw = gateway()
      val ext = mockk<BillDownloadServiceExtension>()
      every { ext.getTradeBill(any()) } throws serviceException("NO_STATEMENT_EXIST")
      injectLazy(gw, "billService\$delegate", ext)

      val bill = gw.downloadTradeBill(date, WechatBillType.ALL)
      bill.rows shouldBe emptyList()
      bill.summary.totalCount shouldBe 0
    }

    test("STATEMENT_CREATING throws with retry hint") {
      val gw = gateway()
      val ext = mockk<BillDownloadServiceExtension>()
      every { ext.getTradeBill(any()) } throws serviceException("STATEMENT_CREATING")
      injectLazy(gw, "billService\$delegate", ext)

      val e = shouldThrow<IllegalStateException> { gw.downloadTradeBill(date, WechatBillType.ALL) }
      e.message!!.contains("STATEMENT_CREATING") shouldBe true
    }

    test("hash mismatch rejects bill instead of parsing") {
      val gw = gateway()
      val ext = mockk<BillDownloadServiceExtension>()
      every { ext.getTradeBill(any()) } returns
        DigestBillEntity(ByteArrayInputStream(billCsv.toByteArray()), "deadbeef", HashType.SHA1)
      injectLazy(gw, "billService\$delegate", ext)

      val e = shouldThrow<IllegalStateException> { gw.downloadTradeBill(date, WechatBillType.ALL) }
      e.message!!.contains("哈希") shouldBe true
    }

    test("download verifies SHA1 and parses bill") {
      val gw = gateway()
      val ext = mockk<BillDownloadServiceExtension>()
      every { ext.getTradeBill(any()) } returns
        DigestBillEntity(ByteArrayInputStream(billCsv.toByteArray()), sha1Hex(billCsv), HashType.SHA1)
      injectLazy(gw, "billService\$delegate", ext)

      val bill = gw.downloadTradeBill(date, WechatBillType.ALL)
      bill.rows.size shouldBe 1
      bill.rows[0].transactionId shouldBe "42000001"
      bill.rows[0].settleAmountCents shouldBe 888L
    }
  })
