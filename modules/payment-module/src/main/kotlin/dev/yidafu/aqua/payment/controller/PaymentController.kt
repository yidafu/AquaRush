package dev.yidafu.aqua.payment.controller

import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.payment.service.PaymentService
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/payment")
class PaymentController(
    private val paymentService: PaymentService
) {
    
    @PostMapping("/wechat/create")
    fun createWechatPayment(
        @RequestParam orderId: UUID,
        @RequestParam amount: Int,
        @RequestParam description: String
    ): ApiResponse<Map<String, Any>> {
        val paymentData = paymentService.createWechatPayOrder(orderId, amount, description)
        return ApiResponse.success(paymentData)
    }
    
    @PostMapping("/wechat/callback")
    fun handleWechatCallback(@RequestBody callbackData: Map<String, Any>): String {
        val success = paymentService.handleWechatPayCallback(callbackData)
        return if (success) {
            """{"code": "SUCCESS", "message": "成功"}"""
        } else {
            """{"code": "FAIL", "message": "失败"}"""
        }
    }
    
    @GetMapping("/status/{transactionId}")
    fun queryPaymentStatus(@PathVariable transactionId: String): ApiResponse<String> {
        val status = paymentService.queryPaymentStatus(transactionId)
        return ApiResponse.success(status)
    }
    
    @PostMapping("/refund")
    fun refund(
        @RequestParam transactionId: String,
        @RequestParam refundAmount: Int,
        @RequestParam totalAmount: Int
    ): ApiResponse<Boolean> {
        val result = paymentService.refund(transactionId, refundAmount, totalAmount)
        return ApiResponse.success(result)
    }
}
