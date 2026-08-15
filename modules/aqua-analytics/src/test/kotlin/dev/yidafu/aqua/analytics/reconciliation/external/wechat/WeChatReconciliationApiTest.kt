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
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestTemplate
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 微信对账 API 单元测试: CSV 解析(交易/退款/结算)、异常兜底、空响应
 */
class WeChatReconciliationApiTest {
  private lateinit var restTemplate: RestTemplate
  private lateinit var api: WeChatReconciliationApi
  private val date = LocalDate.of(2025, 6, 1)

  private val transactionCsv =
    """
    交易时间,商户订单号,微信支付订单号,...
    wx123,out123,JSAPI,SUCCESS,CFT,100,CNY,100,0,0,矿泉水,,,,,20250601100000,20250601095900,20250601100000,,,100
    """.trimIndent()

  private val refundCsv =
    """
    退款时间,...
    wx123,out123,refund1,refundid1,ORIGINAL,50,50,100,100,MCHT,50,50,50,user,user2
    """.trimIndent()

  private val settlementCsv =
    """
    结算单号,...
    s123,wx123,out123,100,100,mch1,sub1,,ALL,JSAPI,SUCCESS,CFT,CNY,100,100,0,0,0.6,100,0,0,矿泉水,,,,20250601100000
    """.trimIndent()

  @BeforeEach
  fun setUp() {
    restTemplate = mockk(relaxed = true)
    api =
      WeChatReconciliationApi(
        restTemplate,
        ReconciliationConfig(weChatAppId = "appid", weChatMchId = "mchid", weChatApiKey = "secretkey"),
      )
  }

  @Test
  fun `fetchTransactions parses csv into records`() {
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns transactionCsv

    val records = api.fetchTransactions(date)

    assertEquals(1, records.size)
    assertEquals("wx123", records[0].transactionId)
    assertEquals("out123", records[0].outTradeNo)
    assertEquals("SUCCESS", records[0].tradeState)
    assertEquals(100L, records[0].amount)
    assertEquals("矿泉水", records[0].body)
  }

  @Test
  fun `fetchTransactions returns empty list when response null`() {
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns null

    assertEquals(emptyList(), api.fetchTransactions(date))
  }

  @Test
  fun `fetchTransactions throws on rest template failure`() {
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } throws RuntimeException("network")

    assertFailsWith<RuntimeException> { api.fetchTransactions(date) }
  }

  @Test
  fun `fetchTransactions ignores malformed lines`() {
    val malformed = "line1\nshort,line\n"
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns malformed

    assertEquals(emptyList(), api.fetchTransactions(date))
  }

  @Test
  fun `fetchTransactions tolerates invalid datetime with fallback`() {
    val badDateCsv = "h,wx1,out1,JSAPI,SUCCESS,,,,\nwx1,out1,JSAPI,SUCCESS,CFT,100,CNY,100,0,0,body,,,,,bad-date,bad-date,bad-date,,,100\n"
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns badDateCsv

    val records = api.fetchTransactions(date)

    // 第一行字段数不足被跳过, 第二行日期解析失败回退到当前时间
    assertEquals(1, records.size)
    assertEquals(100L, records[0].amount)
  }

  @Test
  fun `fetchRefunds parses csv into refund records`() {
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns refundCsv

    val records = api.fetchRefunds(date)

    assertEquals(1, records.size)
    assertEquals("refund1", records[0].outRefundNo)
    assertEquals("refundid1", records[0].refundId)
  }

  @Test
  fun `fetchRefunds throws on failure`() {
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } throws RuntimeException("network")

    assertFailsWith<RuntimeException> { api.fetchRefunds(date) }
  }

  @Test
  fun `fetchSettlements parses csv into settlement records`() {
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns settlementCsv

    val records = api.fetchSettlements(date)

    assertEquals(1, records.size)
    assertEquals("s123", records[0].settlementId)
    assertEquals("wx123", records[0].transactionId)
  }

  @Test
  fun `fetchSettlements throws on failure`() {
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } throws RuntimeException("network")

    assertFailsWith<RuntimeException> { api.fetchSettlements(date) }
  }

  @Test
  fun `fetchTransactions parses optional empty fields`() {
    // bankType/totalFee/feeType/cashFee/refundFee/couponFee/detail 等可空字段置空
    val csv =
      "h\n" +
        "wx2,out2,JSAPI,SUCCESS,,,CNY,,0,0,body,,,,,20250601100000,20250601095900,20250601100000,,,100,openid2,,trade2,,\n"
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns csv

    val records = api.fetchTransactions(date)

    assertEquals(1, records.size)
    assertEquals("wx2", records[0].transactionId)
    assertEquals(null, records[0].bankType)
    assertEquals(null, records[0].totalFee)
    assertEquals("openid2", records[0].openId)
    assertEquals("trade2", records[0].tradeBill)
    assertEquals(100L, records[0].amount)
  }

  @Test
  fun `fetchTransactions parses non-numeric optional fields as null`() {
    // totalFee/cashFee 为非数字字符串 -> toBigDecimalOrNull 返回 null
    val csv =
      "h\n" +
        "wx3,out3,JSAPI,SUCCESS,CFT,abc,CNY,xyz,0,0,body,detail,attach,dev,tag,20250601100000,20250601095900,20250601100000,,info,100\n"
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns csv

    val records = api.fetchTransactions(date)

    assertEquals(1, records.size)
    assertEquals(null, records[0].totalFee)
    assertEquals(null, records[0].cashFee)
    assertEquals(null, records[0].settlementTotalFee)
    assertEquals("detail", records[0].detail)
    assertEquals("dev", records[0].deviceInfo)
  }

  @Test
  fun `fetchRefunds parses short record with defaults`() {
    // 15 个字段的短记录: refundSuccessTime/refundStatus/createTime 使用默认值
    val csv = "h\nwx4,out4,r1,rid1,ORIGINAL,50,50,100,100,MCHT,0,0,50,acct,acct2\n"
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns csv

    val records = api.fetchRefunds(date)

    assertEquals(1, records.size)
    assertEquals("r1", records[0].outRefundNo)
    assertEquals("SUCCESS", records[0].refundStatus)
    assertEquals(null, records[0].refundSuccessTime)
  }

  @Test
  fun `fetchSettlements parses record with empty optional fields`() {
    val csv =
      "h\n" +
        "s2,wx5,out5,100,100,mch1,,,ALL,JSAPI,SUCCESS,CFT,CNY,100,100,0,0,0.6,100,0,0,body,detail,attach,tag,20250601100000\n"
    every { restTemplate.postForObject(any(), any(), String::class.java, *anyVararg()) } returns csv

    val records = api.fetchSettlements(date)

    assertEquals(1, records.size)
    assertEquals("s2", records[0].settlementId)
    assertEquals(null, records[0].subMchId)
    assertEquals(null, records[0].settlementTime)
    assertEquals("detail", records[0].detail)
  }
}
