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

package dev.yidafu.aqua.analytics.reconciliation.external.wechat

import dev.yidafu.aqua.analytics.reconciliation.external.config.ReconciliationConfig
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.client.RestTemplate
import java.time.LocalDate

/**
 * 微信对账 API 单元测试: CSV 解析(交易/退款/结算)、异常兜底、空响应
 */
class WeChatReconciliationApiTest :
  FunSpec({
    var restTemplate: RestTemplate = mockk()
    var api: WeChatReconciliationApi = mockk()
    val date = LocalDate.of(2025, 6, 1)
    val transactionCsv =
      """
      交易时间,商户订单号,微信支付订单号,...
      wx123,out123,JSAPI,SUCCESS,CFT,100,CNY,100,0,0,矿泉水,,,,,20250601100000,20250601095900,20250601100000,,,100
      """.trimIndent()
    val refundCsv =
      """
      退款时间,...
      wx123,out123,refund1,refundid1,ORIGINAL,50,50,100,100,MCHT,50,50,50,user,user2
      """.trimIndent()
    val settlementCsv =
      """
      结算单号,...
      s123,wx123,out123,100,100,mch1,sub1,,ALL,JSAPI,SUCCESS,CFT,CNY,100,100,0,0,0.6,100,0,0,矿泉水,,,,20250601100000
      """.trimIndent()

    beforeTest {
      restTemplate = mockk(relaxed = true)
      api =
        WeChatReconciliationApi(
          restTemplate,
          ReconciliationConfig(weChatAppId = "appid", weChatMchId = "mchid", weChatApiKey = "secretkey"),
        )
    }
    test("fetchTransactions parses csv into records") {
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns transactionCsv

      val records = api.fetchTransactions(date)

      records.size shouldBe 1
      records[0].transactionId shouldBe "wx123"
      records[0].outTradeNo shouldBe "out123"
      records[0].tradeState shouldBe "SUCCESS"
      records[0].amount shouldBe 100L
      records[0].body shouldBe "矿泉水"
    }
    test("fetchTransactions returns empty list when response null") {
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns null

      api.fetchTransactions(date) shouldBe emptyList()
    }
    test("fetchTransactions throws on rest template failure") {
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } throws RuntimeException("network")

      shouldThrow<RuntimeException> { api.fetchTransactions(date) }
    }
    test("fetchTransactions ignores malformed lines") {
      val malformed = "line1\nshort,line\n"
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns malformed

      api.fetchTransactions(date) shouldBe emptyList()
    }
    test("fetchTransactions tolerates invalid datetime with fallback") {
      val badDateCsv =
        "h,wx1,out1,JSAPI,SUCCESS,,,,\nwx1,out1,JSAPI,SUCCESS,CFT,100,CNY,100,0,0,body" +
          ",,,,,bad-date,bad-date,bad-date,,,100\n"
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns badDateCsv

      val records = api.fetchTransactions(date)

      // 第一行字段数不足被跳过, 第二行日期解析失败回退到当前时间
      records.size shouldBe 1
      records[0].amount shouldBe 100L
    }
    test("fetchRefunds parses csv into refund records") {
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns refundCsv

      val records = api.fetchRefunds(date)

      records.size shouldBe 1
      records[0].outRefundNo shouldBe "refund1"
      records[0].refundId shouldBe "refundid1"
    }
    test("fetchRefunds throws on failure") {
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } throws RuntimeException("network")

      shouldThrow<RuntimeException> { api.fetchRefunds(date) }
    }
    test("fetchSettlements parses csv into settlement records") {
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns settlementCsv

      val records = api.fetchSettlements(date)

      records.size shouldBe 1
      records[0].settlementId shouldBe "s123"
      records[0].transactionId shouldBe "wx123"
    }
    test("fetchSettlements throws on failure") {
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } throws RuntimeException("network")

      shouldThrow<RuntimeException> { api.fetchSettlements(date) }
    }
    test("fetchTransactions parses optional empty fields") {
      // bankType/totalFee/feeType/cashFee/refundFee/couponFee/detail 等可空字段置空
      val csv =
        "h\n" +
          "wx2,out2,JSAPI,SUCCESS,,,CNY,,0,0,body,,,,,20250601100000,20250601095900,20250601100000,,,100,openid2,,trade2,,\n"
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns csv

      val records = api.fetchTransactions(date)

      records.size shouldBe 1
      records[0].transactionId shouldBe "wx2"
      records[0].bankType shouldBe null
      records[0].totalFee shouldBe null
      records[0].openId shouldBe "openid2"
      records[0].tradeBill shouldBe "trade2"
      records[0].amount shouldBe 100L
    }
    test("fetchTransactions parses non-numeric optional fields as null") {
      // totalFee/cashFee 为非数字字符串 -> toBigDecimalOrNull 返回 null
      val csv =
        "h\n" +
          "wx3,out3,JSAPI,SUCCESS,CFT,abc,CNY,xyz,0,0,body,detail,attach,dev,tag,20250601100000,20250601095900,20250601100000,,info,100\n"
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns csv

      val records = api.fetchTransactions(date)

      records.size shouldBe 1
      records[0].totalFee shouldBe null
      records[0].cashFee shouldBe null
      records[0].settlementTotalFee shouldBe null
      records[0].detail shouldBe "detail"
      records[0].deviceInfo shouldBe "dev"
    }
    test("fetchRefunds parses short record with defaults") {
      // 15 个字段的短记录: refundSuccessTime/refundStatus/createTime 使用默认值
      val csv = "h\nwx4,out4,r1,rid1,ORIGINAL,50,50,100,100,MCHT,0,0,50,acct,acct2\n"
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns csv

      val records = api.fetchRefunds(date)

      records.size shouldBe 1
      records[0].outRefundNo shouldBe "r1"
      records[0].refundStatus shouldBe "SUCCESS"
      records[0].refundSuccessTime shouldBe null
    }
    test("fetchSettlements parses record with empty optional fields") {
      val csv =
        "h\n" +
          "s2,wx5,out5,100,100,mch1,,,ALL,JSAPI,SUCCESS,CFT,CNY,100,100,0,0,0.6,100,0,0,body,detail,attach,tag,20250601100000\n"
      every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns csv

      val records = api.fetchSettlements(date)

      records.size shouldBe 1
      records[0].settlementId shouldBe "s2"
      records[0].subMchId shouldBe null
      records[0].settlementTime shouldBe null
      records[0].detail shouldBe "detail"
    }
  })
