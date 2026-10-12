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

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 微信对账单类型(申请交易账单 bill_type,APIv3 /v3/bill/tradebill,官方枚举子集):
 * - ALL:当日所有订单信息(含退款单行,交易状态=REFUND);
 * - SUCCESS:当日成功支付的订单;
 * - REFUND:当日退款订单(含退款申请/成功时间列)。
 */
enum class WechatBillType {
  ALL,
  SUCCESS,
  REFUND,
}

/**
 * 微信对账单网关抽象(对账域,设计 07-payment-reconciliation.md §4):
 * 封装 APIv3「申请账单 → 下载账单文件 → SHA1 完整性校验 → CSV 解析」全链路,
 * 凭据与 [WechatPayGateway] 同源(wechat.pay.*),解析产物与 SDK 模型解耦。
 *
 * 官方接口约束(下载账单):
 * - 账单 T+1 生成,次日 9 点开始、建议 10 点后获取,当日账单不可下载;
 * - 仅支持三个月内账单;下载链接 5min 有效(内部处理,不外泄);
 * - 下载响应无签名(无需验签),以申请账单返回的 SHA1 哈希校验文件完整性;
 * - 当日无交易/资金变动返回 NO_STATEMENT_EXIST → 映射为空账单(合法状态);
 * - STATEMENT_CREATING(账单生成中)/FREQUENCY_LIMITED 等 → 抛异常,由上层置任务 FAILED。
 *
 * 真实实现见 [dev.yidafu.aqua.payment.service.impl.SdkWechatBillGateway]。
 */
interface WechatBillGateway {
  /**
   * 是否已配置真实微信支付商户参数(商户号/私钥路径/证书序列号/APIv3 密钥)。
   * 未配置时(假支付模式)上层对账任务应置 SKIPPED,不得调用真实接口。
   * 口径与 SdkWechatPayGateway.isConfigured 同源于 wechat.pay.*(账单下载不依赖 appid/notify-url)。
   */
  val isConfigured: Boolean

  /**
   * 申请并下载交易账单(逐笔明细 + 汇总行,金额一律已转为「分」)。
   * 退款单 REFUND 类型包含退款申请/成功时间列;ALL 类型含退款行(交易状态=REFUND)。
   * 账单哈希校验失败、汇总与逐笔合计不一致均抛异常(宁可失败重跑,不带病入账)。
   */
  fun downloadTradeBill(
    billDate: LocalDate,
    billType: WechatBillType = WechatBillType.ALL,
  ): WechatTradeBill

  /**
   * 申请并下载资金账单(基本账户,逐笔资金流水 + 汇总行,含收入与支出全部流水)。
   * 与交易账单不同,资金账单不限交易类型,含手续费扣除/退款支出/提现等行,
   * 消费方按业务类型(bizType)与收支类型(direction)自行过滤。
   */
  fun downloadFundFlowBill(billDate: LocalDate): WechatFundFlowBill
}

/**
 * 交易账单逐笔明细行(官方列头按名定位,金额已转「分」)。
 * 隐私最小化:官方「用户标识」(OpenID)列不解析、不落库(设计 §9)。
 * 官方格式:每字段前缀1个反引号(`),字段间英文逗号,特殊字符反斜杠转义;REFUND 账单才有退款申请/成功时间列。
 */
data class WechatTradeBillRow(
  /** 交易时间(支付成功时间或发起退款成功时间),yyyy-MM-dd HH:mm:ss */
  val tradeTime: LocalDateTime,
  /** 微信订单号 */
  val transactionId: String,
  /** 商户订单号 */
  val outTradeNo: String,
  /** 交易类型 JSAPI/NATIVE/APP/... */
  val transactionType: String,
  /** 交易状态 SUCCESS/REFUND/REVOKED/USERPAYING/PAYERROR */
  val tradeState: String,
  /** 付款银行(如 CMB_CREDIT/OTHERS) */
  val bankType: String?,
  /** 货币种类 ISO4217,通常 CNY */
  val feeType: String?,
  /** 应结订单金额(分)= 订单金额 − 代金券金额;对账比对基准(设计 §6) */
  val settleAmountCents: Long,
  /** 订单金额(分) */
  val orderAmountCents: Long,
  /** 代金券金额(分) */
  val couponCents: Long,
  /** 微信退款单号;订单行(官方值为0)映射为 null */
  val refundId: String?,
  /** 商户退款单号;订单行(官方值为0)映射为 null */
  val outRefundNo: String?,
  /** 退款金额(分,应结口径);订单行为0 */
  val refundAmountCents: Long,
  /** 退款申请时间(仅 REFUND 账单) */
  val refundApplyTime: LocalDateTime?,
  /** 退款成功时间(仅 REFUND 账单,处理中可为空) */
  val refundSuccessTime: LocalDateTime?,
  /** 退款类型 ORIGINAL/BALANCE/...;订单行留空 */
  val refundType: String?,
  /** 退款状态 SUCCESS/PROCESSING/FAIL/CHANGE;订单行留空 */
  val refundStatus: String?,
  /** 商品名称(body) */
  val body: String?,
  /** 商户数据包(attach) */
  val attach: String?,
  /** 设备号(device_id) */
  val deviceInfo: String?,
  /** 手续费(分,订单正数/退款负数) */
  val feeCents: Long,
)

/** 交易账单汇总行(官方汇总表头按名定位;金额已转分;用于与逐笔合计交叉校验) */
data class WechatTradeBillSummary(
  /** 总交易单数 */
  val totalCount: Int,
  /** 汇总金额(官方汇总列名 → 分),如「应结订单总金额」「订单总金额」「退款总金额」「手续费总金额」 */
  val amounts: Map<String, Long>,
)

/** 交易账单(明细 + 汇总);NO_STATEMENT_EXIST 时空明细 + 空汇总 */
data class WechatTradeBill(
  val billDate: LocalDate,
  val billType: WechatBillType,
  val rows: List<WechatTradeBillRow>,
  val summary: WechatTradeBillSummary,
)

/**
 * 资金账单逐笔明细行(官方列头按名定位,金额已转「分」)。
 * 含收入与支出全部流水(交易入账/手续费扣除/退款支出/提现等),
 * 消费方按 bizType + direction 自行过滤出与支付比对的收入行。
 */
data class WechatFundFlowRow(
  /** 记账时间(资金实际增减时间),yyyy-MM-dd HH:mm:ss */
  val recordTime: LocalDateTime,
  /** 微信支付业务单号(交易入账即微信订单号) */
  val wechatBizNo: String,
  /** 资金流水单号 */
  val flowNo: String,
  /** 业务名称 交易/退款/充值、提现/企业付款/分账 等 */
  val bizName: String,
  /** 业务类型 交易/扣除交易手续费/退款 等 */
  val bizType: String,
  /** 收支类型 收入/支出 */
  val direction: String,
  /** 收支金额(分,恒为正,方向由 [direction] 表达) */
  val amountCents: Long,
  /** 账户结余(分) */
  val balanceCents: Long?,
  /** 资金变更提交申请人(如 system/员工账号@商户号),可能为空 */
  val applicant: String?,
  /** 备注(商户/业务自定义),可能为空 */
  val remark: String?,
  /** 业务凭证号(一般为商户订单号),可能为空 */
  val voucherNo: String?,
)

/** 资金账单汇总行 */
data class WechatFundFlowSummary(
  /** 资金流水总笔数 */
  val totalCount: Int,
  /** 收入笔数 */
  val incomeCount: Int,
  /** 收入金额(分) */
  val incomeCents: Long,
  /** 支出笔数 */
  val expenseCount: Int,
  /** 支出金额(分) */
  val expenseCents: Long,
)

/** 资金账单(明细 + 汇总);NO_STATEMENT_EXIST 时空明细 + 空汇总 */
data class WechatFundFlowBill(
  val billDate: LocalDate,
  val rows: List<WechatFundFlowRow>,
  val summary: WechatFundFlowSummary,
)
