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

package dev.yidafu.aqua.reconciliation.external.wechat

import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.KotlinModule
import dev.yidafu.aqua.reconciliation.external.wechat.dto.*
import dev.yidafu.aqua.reconciliation.external.config.ReconciliationConfig
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 微信支付对账API服务
 */
@Service
class WeChatReconciliationApi(
    private val restTemplate: RestTemplate,
    private val config: ReconciliationConfig
) {
    private val logger = LoggerFactory.getLogger(WeChatReconciliationApi::class.java)
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")

    companion object {
        private const val WECHAT_API_BASE_URL = "https://api.mch.weixin.qq.com"
        private const val TRADE_TYPE_JSAPI = "JSAPI"
        private const val TRADE_TYPE_NATIVE = "NATIVE"
        private const val TRADE_TYPE_APP = "APP"
        private const val TRADE_TYPE_H5 = "H5"
    }

    /**
     * 获取微信支付交易记录
     */
    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2)
    )
    fun fetchTransactions(date: LocalDate): List<WeChatTransactionRecord> {
        try {
            logger.info("开始获取微信支付交易记录，日期: $date")

            val startTime = date.atStartOfDay()
            val endTime = date.atTime(23, 59, 59)

            // 构建查询参数
            val params = mutableMapOf<String, Any>()
            params["appid"] = config.weChatAppId
            params["mch_id"] = config.weChatMchId
            params["nonce_str"] = generateNonce()
            params["sign_type"] = "HMAC-SHA256"

            // 添加时间范围（微信API通常限制30天查询范围）
            params["time_start"] = startTime.format(dateFormatter)
            params["time_end"] = endTime.format(dateFormatter)

            // 生成签名
            val sign = generateSign(params)
            params["sign"] = sign

            // 发送请求到微信支付查询接口
            val url = "$WECHAT_API_BASE_URL/pay/downloadbill"
            val response = restTemplate.postForObject(url, params, String::class.java)

            if (response != null) {
                return parseTransactionResponse(response, date)
            }

            return emptyList()
        } catch (e: Exception) {
            logger.error("获取微信支付交易记录失败", e)
            throw RuntimeException("获取微信支付交易记录失败: ${e.message}", e)
        }
    }

    /**
     * 获取微信支付退款记录
     */
    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2)
    )
    fun fetchRefunds(date: LocalDate): List<WeChatRefundRecord> {
        try {
            logger.info("开始获取微信支付退款记录，日期: $date")

            // 构建退款查询参数
            val params = mutableMapOf<String, Any>()
            params["appid"] = config.weChatAppId
            params["mch_id"] = config.weChatMchId
            params["nonce_str"] = generateNonce()
            params["sign_type"] = "HMAC-SHA256"
            params["bill_type"] = "ALL"

            val startTime = date.atStartOfDay()
            val endTime = date.atTime(23, 59, 59)
            params["time_start"] = startTime.format(dateFormatter)
            params["time_end"] = endTime.format(dateFormatter)

            // 生成签名
            val sign = generateSign(params)
            params["sign"] = sign

            // 发送请求到微信支付退款查询接口
            val url = "$WECHAT_API_BASE_URL/refundquery"
            val response = restTemplate.postForObject(url, params, String::class.java)

            if (response != null) {
                return parseRefundResponse(response, date)
            }

            return emptyList()
        } catch (e: Exception) {
            logger.error("获取微信支付退款记录失败", e)
            throw RuntimeException("获取微信支付退款记录失败: ${e.message}", e)
        }
    }

    /**
     * 获取微信支付结算记录
     */
    @Retryable(
        maxAttempts = 3,
        backoff = Backoff(delay = 1000, multiplier = 2)
    )
    fun fetchSettlements(date: LocalDate): List<WeChatSettlementRecord> {
        try {
            logger.info("开始获取微信支付结算记录，日期: $date")

            // 构建结算查询参数
            val params = mutableMapOf<String, Any>()
            params["appid"] = config.weChatAppId
            params["mch_id"] = config.weChatMchId
            params["nonce_str"] = generateNonce()
            params["sign_type"] = "HMAC-SHA256"
            params["bill_type"] = "ALL"

            val startTime = date.atStartOfDay()
            val endTime = date.atTime(23, 59, 59)
            params["time_start"] = startTime.format(dateFormatter)
            params["time_end"] = endTime.format(dateFormatter)

            // 生成签名
            val sign = generateSign(params)
            params["sign"] = sign

            // 发送请求到微信支付结算查询接口
            val url = "$WECHAT_API_BASE_URL/pay/downloadfundflow"
            val response = restTemplate.postForObject(url, params, String::class.java)

            if (response != null) {
                return parseSettlementResponse(response, date)
            }

            return emptyList()
        } catch (e: Exception) {
            logger.error("获取微信支付结算记录失败", e)
            throw RuntimeException("获取微信支付结算记录失败: ${e.message}", e)
        }
    }

    /**
     * 生成随机字符串
     */
    private fun generateNonce(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32).map { chars.random() }.joinToString("")
    }

    /**
     * 生成微信支付签名
     */
    private fun generateSign(params: Map<String, Any>): String {
        try {
            // 按键名排序
            val sortedParams = params.toSortedMap()

            // 构建签名字符串
            val signBuilder = StringBuilder()
            sortedParams.forEach { (key, value) ->
                if (key != "sign" && value != null) {
                    if (signBuilder.isNotEmpty()) {
                        signBuilder.append("&")
                    }
                    signBuilder.append("$key=$value")
                }
            }

            val signString = signBuilder.toString()

            // 使用HMAC-SHA256算法生成签名
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(config.weChatApiKey.toByteArray(), "HmacSHA256"))
            val signBytes = mac.doFinal(signString.toByteArray())

            return Base64.getEncoder().encodeToString(signBytes)
        } catch (e: Exception) {
            logger.error("生成微信支付签名失败", e)
            throw RuntimeException("生成微信支付签名失败: ${e.message}", e)
        }
    }

    /**
     * 解析交易记录响应
     */
    private fun parseTransactionResponse(response: String, date: LocalDate): List<WeChatTransactionRecord> {
        try {
            // 微信支付对账单格式通常是CSV，需要解析
            val lines = response.split("\n")
            val records = mutableListOf<WeChatTransactionRecord>()

            // 跳过表头
            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isNotBlank()) {
                    val fields = line.split(",")
                    if (fields.size >= 20) {
                        val record = WeChatTransactionRecord(
                            transactionId = fields[0].trim(),
                            outTradeNo = fields[1].trim(),
                            transactionType = fields[2].trim(),
                            tradeState = fields[3].trim(),
                            bankType = fields[4].trim().takeIf { it.isNotBlank() },
                            totalFee = fields[5].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            feeType = fields[6].trim().takeIf { it.isNotBlank() },
                            cashFee = fields[7].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            refundFee = fields[8].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            couponFee = fields[9].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            body = fields[10].trim(),
                            detail = fields[11].trim().takeIf { it.isNotBlank() },
                            attach = fields[12].trim().takeIf { it.isNotBlank() },
                            deviceInfo = fields[13].trim().takeIf { it.isNotBlank() },
                            goodsTag = fields[14].trim().takeIf { it.isNotBlank() },
                            timeEnd = parseWeChatDateTime(fields[15].trim()),
                            tradeCreateTime = parseWeChatDateTime(fields[16].trim()),
                            tradePayTime = parseWeChatDateTime(fields[17].trim()).takeIf { it.isNotBlank() },
                            settlementTotalFee = fields[18].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            promotionInfo = fields[19].trim().takeIf { it.isNotBlank() },
                            amount = fields[20].trim().toBigDecimal(),
                            openId = if (fields.size > 21) fields[21].trim().takeIf { it.isNotBlank() } else null,
                            subOpenId = if (fields.size > 22) fields[22].trim().takeIf { it.isNotBlank() } else null,
                            returnCode = "SUCCESS",
                            returnMsg = "获取成功",
                            errorCode = null,
                            errorCodeDes = null
                        )
                        records.add(record)
                    }
                }
            }

            logger.info("解析微信支付交易记录完成，共获取${records.size}条记录")
            return records
        } catch (e: Exception) {
            logger.error("解析微信支付交易记录响应失败", e)
            return emptyList()
        }
    }

    /**
     * 解析退款记录响应
     */
    private fun parseRefundResponse(response: String, date: LocalDate): List<WeChatRefundRecord> {
        try {
            // 类似交易记录的解析逻辑
            val lines = response.split("\n")
            val records = mutableListOf<WeChatRefundRecord>()

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isNotBlank()) {
                    val fields = line.split(",")
                    if (fields.size >= 15) {
                        val record = WeChatRefundRecord(
                            transactionId = fields[0].trim(),
                            outTradeNo = fields[1].trim(),
                            outRefundNo = fields[2].trim(),
                            refundId = fields[3].trim(),
                            refundChannel = fields[4].trim().takeIf { it.isNotBlank() },
                            refundFee = fields[5].trim().toBigDecimal(),
                            settlementRefundFee = fields[6].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            totalFee = fields[7].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            settlementTotalFee = fields[8].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            couponType = fields[9].trim().takeIf { it.isNotBlank() },
                            couponRefundFee = fields[10].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            couponFee = fields[11].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            amount = fields[12].trim().toBigDecimal(),
                            refundRecvAccount = fields[13].trim().takeIf { it.isNotBlank() },
                            refundAccount = fields[14].trim().takeIf { it.isNotBlank() },
                            refundRequestSource = if (fields.size > 15) fields[15].trim().takeIf { it.isNotBlank() } else null,
                            refundSuccessTime = if (fields.size > 16) parseWeChatDateTime(fields[16].trim()).takeIf { it.isNotBlank() } else null,
                            refundStatus = if (fields.size > 17) fields[17].trim() else "SUCCESS",
                            createTime = if (fields.size > 18) parseWeChatDateTime(fields[18].trim()) else LocalDate.now().atStartOfDay(),
                            returnCode = "SUCCESS",
                            returnMsg = "获取成功",
                            errorCode = null,
                            errorCodeDes = null
                        )
                        records.add(record)
                    }
                }
            }

            logger.info("解析微信支付退款记录完成，共获取${records.size}条记录")
            return records
        } catch (e: Exception) {
            logger.error("解析微信支付退款记录响应失败", e)
            return emptyList()
        }
    }

    /**
     * 解析结算记录响应
     */
    private fun parseSettlementResponse(response: String, date: LocalDate): List<WeChatSettlementRecord> {
        try {
            // 类似交易记录的解析逻辑
            val lines = response.split("\n")
            val records = mutableListOf<WeChatSettlementRecord>()

            for (i in 1 until lines.size) {
                val line = lines[i]
                if (line.isNotBlank()) {
                    val fields = line.split(",")
                    if (fields.size >= 25) {
                        val record = WeChatSettlementRecord(
                            settlementId = fields[0].trim(),
                            transactionId = fields[1].trim(),
                            outTradeNo = fields[2].trim(),
                            amount = fields[3].trim().toBigDecimal(),
                            settlementAmount = fields[4].trim().toBigDecimal(),
                            mchId = fields[5].trim(),
                            subMchId = fields[6].trim().takeIf { it.isNotBlank() },
                            deviceInfo = fields[7].trim().takeIf { it.isNotBlank() },
                            billType = fields[8].trim(),
                            tradeType = fields[9].trim(),
                            tradeState = fields[10].trim(),
                            bankType = fields[11].trim().takeIf { it.isNotBlank() },
                            feeType = fields[12].trim().takeIf { it.isNotBlank() },
                            totalFee = fields[13].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            settlementTotalFee = fields[14].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            couponFee = fields[15].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            couponRefundFee = fields[16].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            rate = fields[17].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            cashFee = fields[18].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull(),
                            refundFee = fields[19].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            settlementRefundFee = fields[20].trim().takeIf { it.isNotBlank() }?.toBigDecimalOrNull() ?: BigDecimal.ZERO,
                            body = fields[21].trim(),
                            detail = fields[22].trim().takeIf { it.isNotBlank() },
                            attach = fields[23].trim().takeIf { it.isNotBlank() },
                            goodsTag = fields[24].trim().takeIf { it.isNotBlank() },
                            timeEnd = parseWeChatDateTime(fields[25].trim()),
                            settlementTime = if (fields.size > 26) parseWeChatDateTime(fields[26].trim()).takeIf { it.isNotBlank() } else null,
                            tradeCreateTime = if (fields.size > 27) parseWeChatDateTime(fields[27].trim()) else LocalDate.now().atStartOfDay(),
                            tradePayTime = if (fields.size > 28) parseWeChatDateTime(fields[28].trim()).takeIf { it.isNotBlank() } else null,
                            promotionInfo = if (fields.size > 29) fields[29].trim().takeIf { it.isNotBlank() } else null,
                            createTime = LocalDate.now().atStartOfDay(),
                            returnCode = "SUCCESS",
                            returnMsg = "获取成功",
                            errorCode = null,
                            errorCodeDes = null
                        )
                        records.add(record)
                    }
                }
            }

            logger.info("解析微信支付结算记录完成，共获取${records.size}条记录")
            return records
        } catch (e: Exception) {
            logger.error("解析微信支付结算记录响应失败", e)
            return emptyList()
        }
    }

    /**
     * 解析微信支付时间格式
     */
    private fun parseWeChatDateTime(dateTimeStr: String): java.time.LocalDateTime {
        return try {
            // 微信支付时间格式通常是yyyyMMddHHmmss或yyyy-MM-dd HH:mm:ss
            val formatter = when {
                dateTimeStr.length == 14 -> DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                dateTimeStr.contains("-") -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                else -> DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            }
            java.time.LocalDateTime.parse(dateTimeStr, formatter)
        } catch (e: Exception) {
            logger.warn("解析微信支付时间失败: $dateTimeStr", e)
            java.time.LocalDateTime.now()
        }
    }

    /**
     * String转BigDecimal的安全方法
     */
    private fun String.toBigDecimalOrNull(): BigDecimal? {
        return try {
            this.toBigDecimal()
        } catch (e: Exception) {
            null
        }
    }
}
