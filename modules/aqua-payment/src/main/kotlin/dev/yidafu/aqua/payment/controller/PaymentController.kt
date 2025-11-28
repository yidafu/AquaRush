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

package dev.yidafu.aqua.payment.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.payment.service.PaymentService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/payment")
class PaymentController(
  private val paymentService: PaymentService,
) {
  @PostMapping("/wechat/create")
  fun createWechatPayment(
    @RequestParam orderId: Long,
    @RequestParam amount: Int,
    @RequestParam description: String,
  ): ApiResponse<Map<String, Any>> {
    val paymentData = paymentService.createWechatPayOrder(orderId, amount, description)
    return ApiResponse.success(paymentData)
  }

  @PostMapping("/wechat/callback")
  fun handleWechatCallback(
    @RequestBody callbackData: Map<String, Any>,
  ): String {
    val success = paymentService.handleWechatPayCallback(callbackData)
    return if (success) {
      """{"code": "SUCCESS", "message": "成功"}"""
    } else {
      """{"code": "FAIL", "message": "失败"}"""
    }
  }

  @GetMapping("/status/{transactionId}")
  fun queryPaymentStatus(
    @PathVariable transactionId: String,
  ): ApiResponse<String> {
    // TODO: Implement proper transactionId to orderId mapping or update service to handle transactionId
    // For now, we'll convert string to Long assuming it's an orderId
    try {
      val orderId = transactionId.toLong()
      val status = paymentService.queryPaymentStatus(orderId)
      return ApiResponse.success(status)
    } catch (e: IllegalArgumentException) {
      return ApiResponse.error("Invalid transaction ID format")
    }
  }

  @PostMapping("/refund")
  fun refund(
    @RequestParam transactionId: String,
    @RequestParam refundAmount: Int,
    @RequestParam totalAmount: Int,
  ): ApiResponse<Boolean> {
    val result = paymentService.refund(transactionId, refundAmount, totalAmount)
    return ApiResponse.success(result)
  }
}
