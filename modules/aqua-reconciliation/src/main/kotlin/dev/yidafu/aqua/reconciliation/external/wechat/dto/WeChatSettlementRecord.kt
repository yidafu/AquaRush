/*
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

package dev.yidafu.aqua.reconciliation.external.wechat.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 微信支付结算记录DTO
 */
data class WeChatSettlementRecord(
    @JsonProperty("settlement_id")
    val settlementId: String,
    @JsonProperty("transaction_id")
    val transactionId: String,
    @JsonProperty("out_trade_no")
    val outTradeNo: String,
    @JsonProperty("amount")
    val amount: BigDecimal,
    @JsonProperty("settlement_amount")
    val settlementAmount: BigDecimal,
    @JsonProperty("mchid")
    val mchId: String,
    @JsonProperty("sub_mchid")
    val subMchId: String?,
    @JsonProperty("device_info")
    val deviceInfo: String?,
    @JsonProperty("bill_type")
    val billType: String,
    @JsonProperty("trade_type")
    val tradeType: String,
    @JsonProperty("trade_state")
    val tradeState: String,
    @JsonProperty("bank_type")
    val bankType: String?,
    @JsonProperty("fee_type")
    val feeType: String?,
    @JsonProperty("total_fee")
    val totalFee: BigDecimal?,
    @JsonProperty("settlement_total_fee")
    val settlementTotalFee: BigDecimal?,
    @JsonProperty("coupon_fee")
    val couponFee: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("coupon_refund_fee")
    val couponRefundFee: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("rate")
    val rate: BigDecimal?,
    @JsonProperty("cash_fee")
    val cashFee: BigDecimal?,
    @JsonProperty("refund_fee")
    val refundFee: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("settlement_refund_fee")
    val settlementRefundFee: BigDecimal? = BigDecimal.ZERO,
    @JsonProperty("body")
    val body: String,
    @JsonProperty("detail")
    val detail: String?,
    @JsonProperty("attach")
    val attach: String?,
    @JsonProperty("goods_tag")
    val goodsTag: String?,
    @JsonProperty("time_end")
    val timeEnd: LocalDateTime,
    @JsonProperty("settlement_time")
    val settlementTime: LocalDateTime?,
    @JsonProperty("trade_create_time")
    val tradeCreateTime: LocalDateTime,
    @JsonProperty("trade_pay_time")
    val tradePayTime: LocalDateTime?,
    @JsonProperty("promotion_info")
    val promotionInfo: String?,
    @JsonProperty("create_time")
    val createTime: LocalDateTime,
    @JsonProperty("return_code")
    val returnCode: String,
    @JsonProperty("return_msg")
    val returnMsg: String?,
    @JsonProperty("err_code")
    val errorCode: String?,
    @JsonProperty("err_code_des")
    val errorCodeDes: String?,
) {
  companion object {
    const val SUCCESS = "SUCCESS"
  }

  /**
   * 是否结算成功
   */
  fun isSuccess(): Boolean = returnCode == SUCCESS

  /**
   * 获取手续费
   */
  fun getEffectiveTotalFee(): BigDecimal = totalFee ?: BigDecimal.ZERO

  /**
   * 获取净结算金额
   */
  fun getNetSettlementAmount(): BigDecimal = settlementAmount - getEffectiveTotalFee()
}
