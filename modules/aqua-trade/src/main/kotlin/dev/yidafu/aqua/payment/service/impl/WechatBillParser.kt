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
import dev.yidafu.aqua.payment.service.WechatFundFlowBill
import dev.yidafu.aqua.payment.service.WechatFundFlowRow
import dev.yidafu.aqua.payment.service.WechatFundFlowSummary
import dev.yidafu.aqua.payment.service.WechatTradeBill
import dev.yidafu.aqua.payment.service.WechatTradeBillRow
import dev.yidafu.aqua.payment.service.WechatTradeBillSummary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 微信官方账单 CSV 解析器(纯函数,无 IO,设计 07 §4.2):
 *
 * 官方格式(与官网样例 ALL.xlsx/REFUND.xlsx/资金账单.xlsx 对齐):
 * - 列按**列头行名称**定位,不按固定下标(微信侧列增减/顺序变化容错);
 * - 每字段前缀 1 个反引号(`),防 Excel 科学计数法,解析时剥离;
 * - 字段间英文逗号分隔,内容逗号/反斜杠等按官方规则反斜杠转义(`\,`);
 * - 结构:可选说明行 + 列头行 + 明细行 + 汇总表头行(总交易单数/资金流水总笔数)+ 汇总值行;
 * - 金额为「元」字符串(1-2 位小数,手续费可为负)→ 严格转「分」,禁止 Double 中转(R-MON-01);
 * - 逐笔合计必须等于汇总行,不一致抛异常(宁可失败重跑,不带病入账,设计 §4.2)。
 *
 * 时间格式:yyyy-MM-dd HH:mm:ss(官方附录),解析失败即抛,不回退当前时间。
 */
internal object WechatBillParser {
  private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  private val AMOUNT_PATTERN = Regex("""-?\d+(\.\d{1,2})?""")

  /** 汇总列名 → 明细列名(用于逐笔合计与汇总行交叉校验) */
  private val TRADE_SUMMARY_TO_DETAIL =
    mapOf(
      "应结订单总金额" to "应结订单金额",
      "订单总金额" to "订单金额",
      "退款总金额" to "退款金额",
      "充值券退款总金额" to "充值券退款金额",
      "手续费总金额" to "手续费",
      "申请退款总金额" to "申请退款金额",
    )

  /**
   * 解析交易账单 CSV。[billType] 仅用于回填结果(列集按实际列头定位,ALL/SUCCESS/REFUND 通用)。
   * 结构缺失(无列头/无汇总行)、必填列缺失、金额/时间格式非法、汇总与逐笔不一致 → 抛 [IllegalStateException]。
   */
  fun parseTradeBill(
    content: String,
    billDate: LocalDate,
    billType: WechatBillType,
  ): WechatTradeBill {
    val lines = content.removePrefix("").lines().filter { it.isNotBlank() }
    val headerIdx = lines.indexOfFirst { firstCell(it) == "交易时间" }
    if (headerIdx < 0) throw IllegalStateException("微信交易账单格式异常:未找到列头行(交易时间)")
    val header = splitLine(lines[headerIdx]).map { it.stripTick() }

    val rows = mutableListOf<WechatTradeBillRow>()
    val detailSums = mutableMapOf<String, Long>()
    var summaryIdx = -1
    for (i in (headerIdx + 1) until lines.size) {
      val cells = splitLine(lines[i]).map { it.stripTick() }
      if (cells.firstOrNull().orEmpty() == "总交易单数") {
        summaryIdx = i
        break
      }
      rows += parseTradeRow(header, cells, detailSums)
    }
    if (summaryIdx < 0) throw IllegalStateException("微信交易账单格式异常:未找到汇总行(总交易单数)")
    if (summaryIdx + 1 >= lines.size) throw IllegalStateException("微信交易账单格式异常:汇总值行缺失")

    val summary =
      parseSummary(
        headerNames = splitLine(lines[summaryIdx]).map { it.stripTick() },
        values = splitLine(lines[summaryIdx + 1]).map { it.stripTick() },
      )
    verifySummary("微信交易账单", summary.totalCount, rows.size, summary.amounts, detailSums)

    return WechatTradeBill(billDate, billType, rows, summary)
  }

  /** 解析资金账单 CSV(记账时间列头;含收入与支出全部流水) */
  fun parseFundFlowBill(
    content: String,
    billDate: LocalDate,
  ): WechatFundFlowBill {
    val lines = content.removePrefix("﻿").lines().filter { it.isNotBlank() }
    val headerIdx = lines.indexOfFirst { firstCell(it) == "记账时间" }
    if (headerIdx < 0) throw IllegalStateException("微信资金账单格式异常:未找到列头行(记账时间)")
    val header = splitLine(lines[headerIdx]).map { it.stripTick() }

    val rows = mutableListOf<WechatFundFlowRow>()
    var summaryIdx = -1
    for (i in (headerIdx + 1) until lines.size) {
      val cells = splitLine(lines[i]).map { it.stripTick() }
      if (cells.firstOrNull().orEmpty() == "资金流水总笔数") {
        summaryIdx = i
        break
      }
      rows += parseFundFlowRow(header, cells)
    }
    if (summaryIdx < 0) throw IllegalStateException("微信资金账单格式异常:未找到汇总行(资金流水总笔数)")
    if (summaryIdx + 1 >= lines.size) throw IllegalStateException("微信资金账单格式异常:汇总值行缺失")

    val summaryHeader = splitLine(lines[summaryIdx]).map { it.stripTick() }
    val summaryValues = splitLine(lines[summaryIdx + 1]).map { it.stripTick() }
    val summaryMap = summaryHeader.zip(summaryValues).toMap()
    val summary =
      WechatFundFlowSummary(
        totalCount = requireCount(summaryMap["资金流水总笔数"], "资金流水总笔数"),
        incomeCount = requireCount(summaryMap["收入笔数"], "收入笔数"),
        incomeCents = parseCentsOrZero(summaryMap["收入金额"], "收入金额"),
        expenseCount = requireCount(summaryMap["支出笔数"], "支出笔数"),
        expenseCents = parseCentsOrZero(summaryMap["支出金额"], "支出金额"),
      )

    val incomeRows = rows.filter { it.direction == "收入" }
    val expenseRows = rows.filter { it.direction == "支出" }
    verify("微信资金账单", "资金流水总笔数", summary.totalCount.toLong(), rows.size.toLong())
    verify("微信资金账单", "收入笔数", summary.incomeCount.toLong(), incomeRows.size.toLong())
    verify("微信资金账单", "收入金额", summary.incomeCents, incomeRows.sumOf { it.amountCents })
    verify("微信资金账单", "支出笔数", summary.expenseCount.toLong(), expenseRows.size.toLong())
    verify("微信资金账单", "支出金额", summary.expenseCents, expenseRows.sumOf { it.amountCents })

    return WechatFundFlowBill(billDate, rows, summary)
  }

  private fun parseTradeRow(
    header: List<String>,
    cells: List<String>,
    detailSums: MutableMap<String, Long>,
  ): WechatTradeBillRow {
    fun cell(name: String): String? {
      val idx = header.indexOf(name)
      return if (idx < 0) null else cells.getOrNull(idx)?.trim()?.takeIf { it.isNotEmpty() }
    }

    // 逐笔累加可校验的金额列(汇总交叉校验用)
    for ((detailCol, sumKey) in TRADE_SUMMARY_TO_DETAIL.values.associateWith { it }) {
      val raw = cell(detailCol) ?: continue
      detailSums[sumKey] = (detailSums[sumKey] ?: 0L) + parseCents(raw, detailCol)
    }

    val refundIdRaw = cell("微信退款单号")
    return WechatTradeBillRow(
      tradeTime = parseTime(cell("交易时间") ?: error("微信交易账单缺少列:交易时间"), "交易时间"),
      transactionId = cell("微信订单号") ?: error("微信交易账单缺少列:微信订单号"),
      outTradeNo = cell("商户订单号") ?: error("微信交易账单缺少列:商户订单号"),
      transactionType = cell("交易类型") ?: "",
      tradeState = cell("交易状态") ?: error("微信交易账单缺少列:交易状态"),
      bankType = cell("付款银行"),
      feeType = cell("货币种类"),
      settleAmountCents = parseCents(cell("应结订单金额") ?: error("微信交易账单缺少列:应结订单金额"), "应结订单金额"),
      orderAmountCents = parseCents(cell("订单金额") ?: "0", "订单金额"),
      couponCents = parseCents(cell("代金券金额") ?: "0", "代金券金额"),
      refundId = refundIdRaw?.takeIf { it != "0" },
      outRefundNo = cell("商户退款单号")?.takeIf { it != "0" && it.isNotEmpty() },
      refundAmountCents = parseCents(cell("退款金额") ?: "0", "退款金额"),
      refundApplyTime = cell("退款申请时间")?.let { parseTime(it, "退款申请时间") },
      refundSuccessTime = cell("退款成功时间")?.let { parseTime(it, "退款成功时间") },
      refundType = cell("退款类型"),
      refundStatus = cell("退款状态"),
      body = cell("商品名称"),
      attach = cell("商户数据包"),
      deviceInfo = cell("设备号"),
      feeCents = parseCents(cell("手续费") ?: "0", "手续费"),
    )
  }

  private fun parseFundFlowRow(
    header: List<String>,
    cells: List<String>,
  ): WechatFundFlowRow {
    fun cell(name: String): String? {
      val idx = header.indexOf(name)
      return if (idx < 0) null else cells.getOrNull(idx)?.trim()?.takeIf { it.isNotEmpty() }
    }

    return WechatFundFlowRow(
      recordTime = parseTime(cell("记账时间") ?: error("微信资金账单缺少列:记账时间"), "记账时间"),
      wechatBizNo = cell("微信支付业务单号") ?: "",
      flowNo = cell("资金流水单号") ?: error("微信资金账单缺少列:资金流水单号"),
      bizName = cell("业务名称") ?: "",
      bizType = cell("业务类型") ?: "",
      direction = cell("收支类型") ?: error("微信资金账单缺少列:收支类型"),
      amountCents = parseCents(cell("收支金额(元)") ?: error("微信资金账单缺少列:收支金额(元)"), "收支金额(元)"),
      balanceCents = cell("账户结余(元)")?.let { parseCents(it, "账户结余(元)") },
      applicant = cell("资金变更提交申请人"),
      remark = cell("备注"),
      voucherNo = cell("业务凭证号"),
    )
  }

  private fun parseSummary(
    headerNames: List<String>,
    values: List<String>,
  ): WechatTradeBillSummary {
    val map = headerNames.zip(values).toMap()
    return WechatTradeBillSummary(
      totalCount = requireCount(map["总交易单数"], "总交易单数"),
      amounts =
        TRADE_SUMMARY_TO_DETAIL.keys.associateWith { key ->
          parseCentsOrZero(map[key], key)
        },
    )
  }

  private fun verifySummary(
    label: String,
    summaryCount: Int,
    detailCount: Int,
    summaryAmounts: Map<String, Long>,
    detailSums: Map<String, Long>,
  ) {
    verify(label, "总交易单数", summaryCount.toLong(), detailCount.toLong())
    for ((name, summaryValue) in summaryAmounts) {
      val detailSum = detailSums[name] ?: continue
      verify(label, name, summaryValue, detailSum)
    }
  }

  private fun verify(
    label: String,
    name: String,
    summaryValue: Long,
    detailSum: Long,
  ) {
    check(summaryValue == detailSum) {
      "${label}汇总校验失败:$name 汇总行=$summaryValue 逐笔合计=$detailSum(账单完整性可疑,拒绝入账)"
    }
  }

  private fun requireCount(
    raw: String?,
    field: String,
  ): Int =
    raw?.toBigDecimalOrNull()?.toInt()
      ?: error("微信账单汇总行[$field]解析失败: $raw")

  private fun parseCentsOrZero(
    raw: String?,
    field: String,
  ): Long = if (raw.isNullOrBlank()) 0L else parseCents(raw, field)

  /** 「元」字符串 → 分:严格校验格式,字符串运算,禁止 Double(R-MON-01/设计 §4.2) */
  internal fun parseCents(
    raw: String,
    field: String,
  ): Long {
    val value = raw.stripTick().trim()
    if (!AMOUNT_PATTERN.matches(value)) {
      throw IllegalStateException("微信账单金额格式非法($field): $raw")
    }
    val negative = value.startsWith("-")
    val body = value.removePrefix("-")
    val parts = body.split(".")
    val yuan = parts[0].ifEmpty { "0" }.toLong()
    val frac = (parts.getOrNull(1) ?: "").padEnd(2, '0').take(2)
    val cents = yuan * 100 + frac.toLong()
    return if (negative) -cents else cents
  }

  private fun parseTime(
    raw: String,
    field: String,
  ): LocalDateTime =
    try {
      LocalDateTime.parse(raw.stripTick().trim(), TIME_FORMATTER)
    } catch (e: Exception) {
      throw IllegalStateException("微信账单时间格式非法($field): $raw", e)
    }

  /** 拆分一行 CSV:支持双引号包裹与反斜杠转义(官方转义规则: `\,` `\\` `\"` 等) */
  private fun splitLine(line: String): List<String> {
    val cells = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
      val c = line[i]
      when {
        c == '\\' && i + 1 < line.length -> {
          sb.append(c)
          sb.append(line[i + 1])
          i += 2
        }
        c == '"' -> {
          inQuotes = !inQuotes
          i++
        }
        c == ',' && !inQuotes -> {
          cells.add(sb.toString())
          sb.clear()
          i++
        }
        else -> {
          sb.append(c)
          i++
        }
      }
    }
    cells.add(sb.toString())
    return cells
  }

  /** 剥离官方防科学计数法反引号前缀,并按官方规则反转义(`\,`→`,`、`\\`→`\`、`\n`→LF 等) */
  private fun String.stripTick(): String = removePrefix("`").trim().unescapeOfficial()

  private fun String.unescapeOfficial(): String {
    if (!contains('\\')) return this
    val sb = StringBuilder(length)
    var i = 0
    while (i < length) {
      val c = this[i]
      if (c == '\\' && i + 1 < length) {
        when (val next = this[i + 1]) {
          'n' -> sb.append('\n')
          'r' -> sb.append('\r')
          't' -> sb.append('\t')
          else -> sb.append(next)
        }
        i += 2
      } else {
        sb.append(c)
        i++
      }
    }
    return sb.toString()
  }

  private fun firstCell(line: String): String = splitLine(line).firstOrNull()?.stripTick().orEmpty()
}
