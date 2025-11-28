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
 * 微信支付退款记录DTO
 */
data class WeChatRefundRecord(
    @JsonProperty("transaction_id")
    val transactionId: String,

    @JsonProperty("out_trade_no")
    val outTradeNo: String,

    @JsonProperty("out_refund_no")
    val outRefundNo: String,

    @JsonProperty("refund_id")
    val refundId: String,

    @JsonProperty("refund_channel")
    val refundChannel: String?,

    @JsonProperty("refund_fee")
    val refundFee: BigDecimal,

    @JsonProperty("settlement_refund_fee")
    val settlementRefundFee: BigDecimal?,

    @JsonProperty("total_fee")
    val totalFee: BigDecimal?,

    @JsonProperty("settlement_total_fee")
    val settlementTotalFee: BigDecimal?,

    @JsonProperty("coupon_type")
    val couponType: String?,

    @JsonProperty("coupon_refund_fee")
    val couponRefundFee: BigDecimal? = BigDecimal.ZERO,

    @JsonProperty("coupon_fee")
    val couponFee: BigDecimal? = BigDecimal.ZERO,

    @JsonProperty("amount")
    val amount: BigDecimal,

    @JsonProperty("refund_recv_account")
    val refundRecvAccount: String?,

    @JsonProperty("refund_account")
    val refundAccount: String?,

    @JsonProperty("refund_request_source")
    val refundRequestSource: String?,

    @JsonProperty("refund_success_time")
    val refundSuccessTime: LocalDateTime?,

    @JsonProperty("refund_status")
    val refundStatus: String,

    @JsonProperty("create_time")
    val createTime: LocalDateTime,

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
        const val REFUND_CLOSE = "REFUND_CLOSE"
        const val PROCESSING = "PROCESSING"
        const val CHANGE = "CHANGE"
        const val FAIL = "FAIL"
    }

    /**
     * 是否退款成功
     */
    fun isSuccess(): Boolean = refundStatus == SUCCESS

    /**
     * 是否退款处理中
     */
    fun isProcessing(): Boolean = refundStatus == PROCESSING

    /**
     * 是否退款失败
     */
    fun isFailed(): Boolean = refundStatus == FAIL

    /**
     * 是否退款已关闭
     */
    fun isClosed(): Boolean = refundStatus == REFUND_CLOSE

    /**
     * 是否退款有变化
     */
    fun hasChange(): Boolean = refundStatus == CHANGE
}
