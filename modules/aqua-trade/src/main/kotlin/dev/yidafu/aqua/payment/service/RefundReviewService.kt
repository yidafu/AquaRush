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

import dev.yidafu.aqua.api.service.deposit.DepositMutationApiService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 管理员退款审核服务(R-PAY-02):
 * 状态机 PENDING(待审核)→ APPROVED(已通过)→ COMPLETED(已完成)/FAILED(失败);PENDING → REJECTED(已拒绝)。
 * 通过:执行订单退款(商品+配送费,ORDER_REFUND)+ 历史订单押金退还(DEPOSIT_REFUND,如有,2026-08 水桶费改造后仅旧订单触发),订单流转「已退款」;
 * 驳回:退款终止,订单保持「已取消」;失败:人工介入可重试(再 APPROVE)。
 */
@Service
class RefundReviewService(
  private val paymentRefundRepository: PaymentRefundRepository,
  private val paymentRepository: PaymentRepository,
  private val paymentService: PaymentService,
  private val orderQueryService: OrderQueryService,
  private val orderMutationService: OrderMutationService,
  private val orderOperationService: OrderOperationService,
  private val depositMutationApiService: DepositMutationApiService,
  private val bizLogger: BizLogger,
) {
  private val logger = LoggerFactory.getLogger(RefundReviewService::class.java)

  /**
   * 审核通过:执行退款并流转订单「已退款」
   */
  @Transactional
  fun approveRefund(
    refundRequestId: Long,
    adminId: Long,
    adminNote: String?,
    username: String,
  ): PaymentRefundModel {
    val request = loadRequest(refundRequestId)
    if (request.status != RefundStatus.PENDING && request.status != RefundStatus.FAILED) {
      throw BadRequestException("退款申请状态为 ${request.status},不可审核通过")
    }

    val order = orderQueryService.getOrderById(request.orderId)
    val transactionId = paymentRepository.findById(request.paymentId).map { it.transactionId }.orElse(null)

    return try {
      if (transactionId.isNullOrEmpty()) {
        throw IllegalStateException("支付记录缺少交易号,无法执行退款")
      }
      val totalAmountCents = order.payAmountCents
      // 订单退款(商品+配送费,ORDER_REFUND 计入退款统计);
      // refundRequestId 回写本条申请为 COMPLETED(取消时已生成 PENDING,避免双记录双计)
      val orderRefundAmount = order.amountCents + order.deliveryFeeCents
      paymentService.refund(
        transactionId = transactionId,
        refundAmountCents = orderRefundAmount,
        totalAmountCents = totalAmountCents,
        reason = "订单取消退款 - 订单号: ${order.orderNo}",
        refundType = RefundType.ORDER_REFUND,
        refundRequestId = refundRequestId,
      )
      // 历史订单押金退还(2026-08 水桶费改造后新订单恒无押金;此处仅兼容旧订单 orders.deposit_amount_cents>0,
      // DEPOSIT_REFUND 不计入退款统计,见 14-misc.md §G06)
      if (order.depositCents > 0) {
        paymentService.refund(
          transactionId = transactionId,
          refundAmountCents = order.depositCents,
          totalAmountCents = totalAmountCents,
          reason = "订单取消退还桶押金 - 订单号: ${order.orderNo}",
          refundType = RefundType.DEPOSIT_REFUND,
        )
        depositMutationApiService.markRefundedByOrder(order.id!!, adminId, "订单取消自动退还")
      }

      // 退款申请完成留痕:重载记录以保留 refund() 回写的 refundId/outRefundNo,避免旧快照覆盖
      val now = LocalDateTime.now()
      val completed = paymentRefundRepository.findById(refundRequestId).orElse(request)
      completed.status = RefundStatus.COMPLETED
      completed.refundedAt = completed.refundedAt ?: now
      completed.processedBy = adminId
      completed.adminNote = adminNote
      completed.refundReason = completed.refundReason ?: "订单取消退款 - 订单号: ${order.orderNo}"
      val saved = paymentRefundRepository.save(completed)

      // 订单流转「已退款」(R-PAY-02:审核通过且退款成功;已退款状态幂等)
      if (order.status == OrderModelStatus.CANCELLED) {
        orderMutationService.updateOrderStatus(order.id!!, OrderModelStatus.REFUNDED)
      }

      // 订单操作留痕:退款完成
      orderOperationService.recordOperation(
        orderId = order.id!!,
        operationType = OrderOperationType.REFUND_COMPLETED,
        operatorType = OperatorType.ADMIN,
        operatorId = adminId,
        description = "退款审核通过并执行成功",
        extraData = """{"refundRequestId": "$refundRequestId", "adminNote": "${adminNote ?: ""}"}""",
      )

      // 业务日志(F-LOG-003 PAYMENT 域):退款完成
      bizLogger.logOperation(
        operation = "PAYMENT_REFUNDED",
        module = "PAYMENT",
        result = "SUCCESS",
        target = refundRequestId.toString(),
        userId = adminId,
        username = username,
        additionalData =
          mapOf<String, Any>(
            "orderId" to order.id!!,
            "refundAmountCents" to request.refundAmount,
            "status" to RefundStatus.COMPLETED.name,
            "adminNote" to (adminNote ?: ""),
          ),
      )
      logger.info("退款审核通过并完成: requestId={}, orderId={}, amount={}", refundRequestId, order.id, request.refundAmount)
      saved
    } catch (e: Exception) {
      logger.error("退款执行失败: requestId={}, orderId={}", refundRequestId, request.orderId, e)
      // 重载以保留 refund() 可能已回写的 refundId;资金已出(COMPLETED)不降级 FAILED,
      // 避免再次审核重复退款——后续步骤(押金退还/订单流转)失败由人工核对
      val latest = paymentRefundRepository.findById(refundRequestId).orElse(request)
      if (latest.status != RefundStatus.COMPLETED) {
        latest.status = RefundStatus.FAILED
        latest.failureReason = e.message
      }
      latest.processedBy = adminId
      latest.adminNote = adminNote
      paymentRefundRepository.save(latest)

      bizLogger.logOperation(
        operation = "PAYMENT_REFUNDED",
        module = "PAYMENT",
        result = "FAILURE",
        target = refundRequestId.toString(),
        userId = adminId,
        username = username,
        additionalData = mapOf("orderId" to request.orderId, "error" to (e.message ?: "unknown error")),
      )
      // 返回 FAILED 记录(不抛异常:审核动作已完成,前端展示失败状态,可重试)
      latest
    }
  }

  /**
   * 审核驳回:退款终止,订单保持「已取消」
   */
  @Transactional
  fun rejectRefund(
    refundRequestId: Long,
    adminId: Long,
    adminNote: String?,
    username: String,
  ): PaymentRefundModel {
    val request = loadRequest(refundRequestId)
    if (request.status != RefundStatus.PENDING) {
      throw BadRequestException("退款申请状态为 ${request.status},不可驳回")
    }
    val now = LocalDateTime.now()
    request.status = RefundStatus.REJECTED
    request.refundedAt = now
    request.processedBy = adminId
    request.adminNote = adminNote
    val saved = paymentRefundRepository.save(request)

    // 业务日志(F-LOG-003 PAYMENT 域):退款驳回
    bizLogger.logOperation(
      operation = "REFUND_REJECTED",
      module = "PAYMENT",
      result = "SUCCESS",
      target = refundRequestId.toString(),
      userId = adminId,
      username = username,
      additionalData = mapOf("orderId" to request.orderId, "adminNote" to (adminNote ?: "")),
    )
    logger.info("退款审核驳回: requestId={}, orderId={}", refundRequestId, request.orderId)
    return saved
  }

  private fun loadRequest(refundRequestId: Long): PaymentRefundModel =
    paymentRefundRepository
      .findById(refundRequestId)
      .orElseThrow { BadRequestException("退款申请不存在: $refundRequestId") }
}
