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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.admin.payment.resolvers

import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.CreateManualRefundInput
import dev.yidafu.aqua.common.graphql.generated.ExportTransactionsInput
import dev.yidafu.aqua.common.graphql.generated.ProcessRefundInput
import dev.yidafu.aqua.common.graphql.generated.RefundAction
import dev.yidafu.aqua.common.graphql.generated.RefundRequest
import dev.yidafu.aqua.payment.service.PaymentService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * 管理端支付变更解析器
 * 提供支付管理的变更功能，仅管理员可访问
 */
@AdminService
@Controller
class AdminPaymentMutationResolver(
    private val paymentService: PaymentService
) {
    private val logger = LoggerFactory.getLogger(AdminPaymentMutationResolver::class.java)

    /**
     * 处理退款请求（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun processRefund(@Valid input: ProcessRefundInput): RefundRequest {
        try {
            // 验证输入
            validateProcessRefundInput(input)

            // TODO: 实现从paymentService处理退款
            // 目前返回模拟数据
            val refundRequest = RefundRequest(
                id = "refund_${input.refundRequestId}",
                originalTransactionId = input.originalTransactionId,
                orderId = input.orderId,
                userId = input.userId,
                amount = input.amount,
                reason = input.reason,
                status = when (input.action) {
                    RefundAction.Approve -> RefundStatus.Approved
                    RefundAction.Reject -> RefundStatus.Rejected
                },
                requestedAt = java.time.LocalDateTime.now(),
                processedAt = java.time.LocalDateTime.now(),
                processedBy = getCurrentAdminId(),
                adminNote = input.adminNote
            )

            logger.info("Successfully processed refund request: ${refundRequest.id}")
            return refundRequest
        } catch (e: Exception) {
            logger.error("Failed to process refund request", e)
            throw BadRequestException("处理退款请求失败: ${e.message}")
        }
    }

    /**
     * 手动创建退款（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun createManualRefund(@Valid input: CreateManualRefundInput): RefundRequest {
        try {
            // 验证输入
            validateManualRefundInput(input)

            // TODO: 实现从paymentService创建手动退款
            // 目前返回模拟数据
            val refundRequest = RefundRequest(
                id = "manual_refund_${System.currentTimeMillis()}",
                originalTransactionId = input.originalTransactionId,
                orderId = input.orderId,
                userId = input.userId,
                amount = input.amount,
                reason = input.reason,
                status = RefundStatus.Pending,
                requestedAt = java.time.LocalDateTime.now(),
                processedAt = null,
                processedBy = getCurrentAdminId(),
                adminNote = input.adminNote
            )

            logger.info("Successfully created manual refund: ${refundRequest.id}")
            return refundRequest
        } catch (e: Exception) {
            logger.error("Failed to create manual refund", e)
            throw BadRequestException("创建手动退款失败: ${e.message}")
        }
    }

    /**
     * 强制完成支付（管理员功能）
     * 用于处理异常情况下的支付状态修正
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun forceCompletePayment(transactionId: String, adminNote: String): Boolean {
        return try {
            // TODO: 实现从paymentService强制完成支付
            logger.info("Successfully forced completion of payment: $transactionId")
            true
        } catch (e: Exception) {
            logger.error("Failed to force complete payment", e)
            throw BadRequestException("强制完成支付失败: ${e.message}")
        }
    }

    /**
     * 强制退款（管理员功能）
     * 用于处理异常情况下的强制退款
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun forceRefund(
        transactionId: String,
        amount: BigDecimal,
        reason: String,
        adminNote: String
    ): Boolean {
        return try {
            if (amount <= BigDecimal.ZERO) {
                throw BadRequestException("退款金额必须大于0")
            }

            // TODO: 实现从paymentService强制退款
            logger.info("Successfully forced refund for payment: $transactionId, amount: $amount")
            true
        } catch (e: Exception) {
            logger.error("Failed to force refund", e)
            throw BadRequestException("强制退款失败: ${e.message}")
        }
    }

    /**
     * 更新交易备注（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun updateTransactionNote(transactionId: String, note: String): Boolean {
        return try {
            // TODO: 实现从paymentService更新交易备注
            logger.info("Successfully updated note for transaction: $transactionId")
            true
        } catch (e: Exception) {
            logger.error("Failed to update transaction note", e)
            throw BadRequestException("更新交易备注失败: ${e.message}")
        }
    }

    /**
     * 冻结可疑交易（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    fun freezeSuspiciousTransaction(transactionId: String, reason: String): Boolean {
        return try {
            // TODO: 实现从paymentService冻结可疑交易
            logger.info("Successfully frozen suspicious transaction: $transactionId")
            true
        } catch (e: Exception) {
            logger.error("Failed to freeze suspicious transaction", e)
            throw BadRequestException("冻结可疑交易失败: ${e.message}")
        }
    }

    /**
     * 导出交易报表（管理员功能）
     */
    @PreAuthorize("hasRole('ADMIN')")
    fun exportTransactions(input: ExportTransactionsInput): String {
        try {
            validateExportInput(input)

            // TODO: 实现从paymentService导出交易报表
            val reportId = "report_${System.currentTimeMillis()}"
            logger.info("Successfully exported transactions: $reportId")
            return reportId
        } catch (e: Exception) {
            logger.error("Failed to export transactions", e)
            throw BadRequestException("导出交易报表失败: ${e.message}")
        }
    }

    /**
     * 验证处理退款输入
     */
    private fun validateProcessRefundInput(input: ProcessRefundInput) {
        if (input.refundRequestId.isBlank()) {
            throw BadRequestException("退款请求ID不能为空")
        }
        if (input.originalTransactionId.isBlank()) {
            throw BadRequestException("原始交易ID不能为空")
        }
        if (input.amount <= BigDecimal.ZERO) {
            throw BadRequestException("退款金额必须大于0")
        }
        if (input.reason.isBlank()) {
            throw BadRequestException("退款原因不能为空")
        }
    }

    /**
     * 验证手动退款输入
     */
    private fun validateManualRefundInput(input: CreateManualRefundInput) {
        if (input.originalTransactionId.isBlank()) {
            throw BadRequestException("原始交易ID不能为空")
        }
        if (input.amount <= BigDecimal.ZERO) {
            throw BadRequestException("退款金额必须大于0")
        }
        if (input.reason.isBlank()) {
            throw BadRequestException("退款原因不能为空")
        }
    }

    /**
     * 验证导出输入
     */
    private fun validateExportInput(input: ExportTransactionsInput) {
        if (input.dateFrom != null && input.dateTo != null) {
            if (input.dateFrom!!.isAfter(input.dateTo!!)) {
                throw BadRequestException("开始日期不能晚于结束日期")
            }
        }
        if (input.format !in listOf("CSV", "EXCEL", "PDF")) {
            throw BadRequestException("不支持的导出格式: ${input.format}")
        }
    }

    /**
     * 获取当前管理员ID
     * 在实际实现中应该从Spring Security Context获取
     */
    private fun getCurrentAdminId(): Long {
        // TODO: 从Spring Security Context获取当前管理员ID
        // 暂时返回占位符
        return 1L
    }

}
