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

package dev.yidafu.aqua.client.payment.controller

import dev.yidafu.aqua.api.service.payment.PaymentService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 微信支付 V3 回调端点(PRD 04-order.md §F-ORD-005/R-PAY-01):
 * - 微信以 POST 推送加密 JSON 报文,验签所需头信息(wechatpay-serial/timestamp/nonce/signature)
 *   由微信支付 SDK 校验,签名或金额不符时返回非 SUCCESS 让微信重试;
 * - 响应体固定为 {"code":"SUCCESS","message":"成功"} 或 FAIL。
 */
@RestController
@RequestMapping("/api/pay")
class WechatPayNotifyController(
  private val paymentService: PaymentService,
) {
  private val logger = LoggerFactory.getLogger(WechatPayNotifyController::class.java)

  @PostMapping("/notify")
  fun notify(
    @RequestBody rawBody: String,
    @RequestHeader headers: HttpHeaders,
  ): ResponseEntity<Map<String, String>> {
    val headerMap =
      headers.toSingleValueMap().mapKeys { (key, _) -> key.lowercase() }
    return try {
      val success = paymentService.handleWechatPayCallback(rawBody, headerMap)
      if (success) {
        ResponseEntity.ok(mapOf("code" to "SUCCESS", "message" to "成功"))
      } else {
        logger.warn("微信支付回调处理失败: 返回 FAIL 等待微信重试")
        ResponseEntity.status(500).body(mapOf("code" to "FAIL", "message" to "处理失败"))
      }
    } catch (e: Exception) {
      logger.error("微信支付回调处理异常", e)
      ResponseEntity.status(500).body(mapOf("code" to "FAIL", "message" to (e.message ?: "处理失败")))
    }
  }
}
