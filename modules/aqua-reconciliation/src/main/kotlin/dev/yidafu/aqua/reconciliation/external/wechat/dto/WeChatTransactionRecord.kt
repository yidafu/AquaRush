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
 * 微信支付交易记录DTO
 */
data class WeChatTransactionRecord(
    @JsonProperty("transaction_id")
    val transactionId: String,

    @JsonProperty("out_trade_no")
    val outTradeNo: String,

    @JsonProperty("transaction_type")
    val transactionType: String,

    @JsonProperty("trade_state")
    val tradeState: String,

    @JsonProperty("bank_type")
    val bankType: String?,

    @JsonProperty("total_fee")
    val totalFee: BigDecimal?,

    @JsonProperty("fee_type")
    val feeType: String?,

    @JsonProperty("cash_fee")
    val cashFee: BigDecimal?,

    @JsonProperty("refund_fee")
    val refundFee: BigDecimal? = BigDecimal.ZERO,

    @JsonProperty("coupon_fee")
    val couponFee: BigDecimal? = BigDecimal.ZERO,

    @JsonProperty("body")
    val body: String,

    @JsonProperty("detail")
    val detail: String?,

    @JsonProperty("attach")
    val attach: String?,

    @JsonProperty("device_info")
    val deviceInfo: String?,

    @JsonProperty("goods_tag")
    val goodsTag: String?,

    @JsonProperty("time_end")
    val timeEnd: LocalDateTime,

    @JsonProperty("trade_create_time")
    val tradeCreateTime: LocalDateTime,

    @JsonProperty("trade_pay_time")
    val tradePayTime: LocalDateTime?,

    @JsonProperty("settlement_total_fee")
    val settlementTotalFee: BigDecimal?,

    @JsonProperty("promotion_info")
    val promotionInfo: String?,

    @JsonProperty("amount")
    val amount: BigDecimal,

    @JsonProperty("openid")
    val openId: String?,

    @JsonProperty("sub_openid")
    val subOpenId: String?,

    @JsonProperty("trade_bill")
    val tradeBill: String?,

    @JsonProperty("goods_detail")
    val goodsDetail: String?,

    @JsonProperty("cost_type")
    val costType: String?,

    @JsonProperty("profit_sharing")
    val profitSharing: String?,

    @JsonProperty("return_code")
    val returnCode: String,

    @JsonProperty("return_msg")
    val returnMsg: String?,

    @JsonProperty("err_code")
    val errorCode: String?,

    @JsonProperty("err_code_des")
    val errorCodeDes: String?
) {
    companion object {
        const val SUCCESS = "SUCCESS"
        const val REFUND = "REFUND"
        const val NOTPAY = "NOTPAY"
        const val CLOSED = "CLOSED"
        const val REVOKED = "REVOKED"
        const val USERPAYING = "USERPAYING"
        const val PAYERROR = "PAYERROR"
    }

    /**
     * 是否成功支付
     */
    fun isSuccess(): Boolean = tradeState == SUCCESS

    /**
     * 是否已退款
     */
    fun isRefunded(): Boolean = tradeState == REFUND

    /**
     * 是否未支付
     */
    fun isNotPaid(): Boolean = tradeState in listOf(NOTPAY, USERPAYING, PAYERROR)

    /**
     * 是否已关闭
     */
    fun isClosed(): Boolean = tradeState in listOf(CLOSED, REVOKED)
}
