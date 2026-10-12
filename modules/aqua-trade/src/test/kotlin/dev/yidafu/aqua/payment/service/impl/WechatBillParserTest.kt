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

import dev.yidafu.aqua.payment.service.WechatBillType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

/**
 * 微信官方账单 CSV 解析单测:基于官网样例(ALL.xlsx/REFUND.xlsx/资金账单.xlsx)格式,
 * 覆盖列头定位、反引号剥离、转义逗号、元→分转换、汇总交叉校验、非法格式拒绝。
 */
class WechatBillParserTest :
  FunSpec({
    val date = LocalDate.of(2025, 6, 1)

    // 官方 ALL 账单样例结构:列头行 + 反引号前缀明细行(商品名含转义逗号)+ 汇总表头行 + 汇总值行
    val tradeCsv =
      """
      交易时间,公众账号ID,商户号,特约商户号,设备号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率,订单金额,申请退款金额,费率备注
      `2025-06-01 10:00:00,`wx-appid,`1900009601,`0,`device1,`42000001,`OUT_1,`openid1,`JSAPI,`SUCCESS,`CMB_CREDIT,`CNY,`8.88,`0.00,`0,`0,`0.00,`0.00,`,`,`矿泉水\,550ml,`,`0.06,`0.60%,`8.88,`0.00,`
      `2025-06-01 11:00:00,`wx-appid,`1900009601,`0,`,`42000002,`OUT_2,`openid2,`JSAPI,`REFUND,`OTHERS,`CNY,`0.00,`0.00,`500001,`REF_OUT_2,`8.88,`0.00,`ORIGINAL,`SUCCESS,`矿泉水,`,`0.00,`0.60%,`0.00,`8.88,`
      总交易单数,应结订单总金额,退款总金额,充值券退款总金额,手续费总金额,订单总金额,申请退款总金额
      `2,`8.88,`8.88,`0.00,`0.06,`8.88,`8.88
      """.trimIndent()

    test("parseTradeBill maps official columns by header name") {
      val bill = WechatBillParser.parseTradeBill(tradeCsv, date, WechatBillType.ALL)

      bill.billDate shouldBe date
      bill.rows.size shouldBe 2

      val payment = bill.rows[0]
      payment.transactionId shouldBe "42000001"
      payment.outTradeNo shouldBe "OUT_1"
      payment.tradeState shouldBe "SUCCESS"
      payment.bankType shouldBe "CMB_CREDIT"
      payment.settleAmountCents shouldBe 888L
      payment.orderAmountCents shouldBe 888L
      payment.refundId shouldBe null // 官方订单行微信退款单号=0 → null
      payment.outRefundNo shouldBe null
      payment.body shouldBe "矿泉水,550ml" // 反斜杠转义逗号还原
      payment.tradeTime shouldBe java.time.LocalDateTime.of(2025, 6, 1, 10, 0)

      val refund = bill.rows[1]
      refund.tradeState shouldBe "REFUND"
      refund.refundId shouldBe "500001"
      refund.outRefundNo shouldBe "REF_OUT_2"
      refund.refundAmountCents shouldBe 888L
      refund.refundStatus shouldBe "SUCCESS"
      refund.settleAmountCents shouldBe 0L
    }

    test("parseTradeBill verifies summary against detail sums") {
      val bill = WechatBillParser.parseTradeBill(tradeCsv, date, WechatBillType.ALL)

      bill.summary.totalCount shouldBe 2
      bill.summary.amounts["应结订单总金额"] shouldBe 888L
      bill.summary.amounts["订单总金额"] shouldBe 888L
      bill.summary.amounts["退款总金额"] shouldBe 888L
      bill.summary.amounts["手续费总金额"] shouldBe 6L
    }

    test("parseTradeBill rejects summary mismatch") {
      val corrupted = tradeCsv.replace("`2,`8.88,`8.88", "`3,`8.88,`8.88")
      shouldThrow<IllegalStateException> { WechatBillParser.parseTradeBill(corrupted, date, WechatBillType.ALL) }
    }

    test("parseTradeBill rejects missing header") {
      shouldThrow<IllegalStateException> { WechatBillParser.parseTradeBill("garbage\n1,2,3", date, WechatBillType.ALL) }
    }

    test("parseTradeBill rejects missing summary row") {
      val noSummary = tradeCsv.substringBefore("总交易单数")
      shouldThrow<IllegalStateException> { WechatBillParser.parseTradeBill(noSummary, date, WechatBillType.ALL) }
    }

    test("parseTradeBill tolerates preamble and CRLF and BOM") {
      val withPreamble =
        "﻿微信支付账单明细,商户号1900009601\r\n" +
          tradeCsv.replace("\n", "\r\n")
      val bill = WechatBillParser.parseTradeBill(withPreamble, date, WechatBillType.ALL)
      bill.rows.size shouldBe 2
    }

    test("parseTradeBill rejects illegal amount") {
      val bad = tradeCsv.replace("`8.88,`0.00,`0,", "`abc,`0.00,`0,")
      shouldThrow<IllegalStateException> { WechatBillParser.parseTradeBill(bad, date, WechatBillType.ALL) }
    }

    test("parseTradeBill rejects illegal time") {
      val bad = tradeCsv.replace("`2025-06-01 10:00:00,", "`2025/06/01 10:00:00,")
      shouldThrow<IllegalStateException> { WechatBillParser.parseTradeBill(bad, date, WechatBillType.ALL) }
    }

    // 官方资金账单样例:11 列明细 + 汇总表头 + 汇总值
    val fundCsv =
      """
      记账时间,微信支付业务单号,资金流水单号,业务名称,业务类型,收支类型,收支金额(元),账户结余(元),资金变更提交申请人,备注,业务凭证号
      `2025-06-01 10:00:01,`42000001,`flow1,`交易,`交易,`收入,`0.03,`51.39,`system,`,`OUT_1
      `2025-06-01 10:00:01,`42000001,`flow2,`交易,`扣除交易手续费,`支出,`0.01,`51.38,`system,`,`OUT_1
      资金流水总笔数,收入笔数,收入金额,支出笔数,支出金额
      `2,`1,`0.03,`1,`0.01
      """.trimIndent()

    test("parseFundFlowBill maps columns and splits income/expense") {
      val bill = WechatBillParser.parseFundFlowBill(fundCsv, date)

      bill.rows.size shouldBe 2
      bill.rows[0].wechatBizNo shouldBe "42000001"
      bill.rows[0].flowNo shouldBe "flow1"
      bill.rows[0].bizType shouldBe "交易"
      bill.rows[0].direction shouldBe "收入"
      bill.rows[0].amountCents shouldBe 3L
      bill.rows[0].voucherNo shouldBe "OUT_1"
      bill.rows[1].direction shouldBe "支出"
      bill.rows[1].amountCents shouldBe 1L

      bill.summary.totalCount shouldBe 2
      bill.summary.incomeCount shouldBe 1
      bill.summary.incomeCents shouldBe 3L
      bill.summary.expenseCount shouldBe 1
      bill.summary.expenseCents shouldBe 1L
    }

    test("parseFundFlowBill rejects summary mismatch") {
      val corrupted = fundCsv.replace("`2,`1,`0.03,`1,`0.01", "`3,`1,`0.03,`1,`0.01")
      shouldThrow<IllegalStateException> { WechatBillParser.parseFundFlowBill(corrupted, date) }
    }

    test("yuan to cents conversion edge cases") {
      WechatBillParser.parseCents("0.03", "x") shouldBe 3L
      WechatBillParser.parseCents("8.88", "x") shouldBe 888L
      WechatBillParser.parseCents("0", "x") shouldBe 0L
      WechatBillParser.parseCents("-0.01", "x") shouldBe -1L
      WechatBillParser.parseCents("12345", "x") shouldBe 1_234_500L
      shouldThrow<IllegalStateException> { WechatBillParser.parseCents("abc", "金额") }
      shouldThrow<IllegalStateException> { WechatBillParser.parseCents("1.234", "金额") }
    }
  })
