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

package dev.yidafu.aqua.admin.payment.resolvers

import dev.yidafu.aqua.admin.payment.mapper.toRefundRequest
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.payment.PaymentQueryService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.generated.ProcessRefundInput
import dev.yidafu.aqua.common.graphql.generated.RefundAction
import dev.yidafu.aqua.common.graphql.generated.RefundRequest
import dev.yidafu.aqua.common.graphql.generated.RefundRequestPage
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import dev.yidafu.aqua.payment.service.RefundReviewService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 管理端退款审核解析器(R-PAY-02:退款申请查询 + 审核通过/驳回)
 */
@Controller
class AdminRefundResolver(
  private val paymentQueryService: PaymentQueryService,
  private val paymentRepository: PaymentRepository,
  private val refundReviewService: RefundReviewService,
  private val adminService: AdminService,
) {
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_READ')")
  fun refundRequests(
    @Argument status: RefundStatus?,
    @Argument dateFrom: LocalDateTime?,
    @Argument dateTo: LocalDateTime?,
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): RefundRequestPage {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val result =
      paymentQueryService.getRefundRequests(
        status = status,
        dateFrom = dateFrom,
        dateTo = dateTo,
        page = page,
        size = size,
      )
    return RefundRequestPage(
      content = result.content.map { it.toRefundRequest(resolveTransactionId(it.paymentId)) },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_READ')")
  fun refundRequest(
    @Argument id: String,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): RefundRequest? {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val refundId = id.toLongOrNull() ?: throw BadRequestException("退款申请ID无效")
    return paymentQueryService.getRefundRequest(refundId)?.let { it.toRefundRequest(resolveTransactionId(it.paymentId)) }
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_REFUND')")
  fun processRefund(
    @Argument input: ProcessRefundInput,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): RefundRequest {
    val refundRequestId = input.refundRequestId.toLongOrNull() ?: throw BadRequestException("退款申请ID无效")
    val admin =
      adminService.findByUsername(userDetails.username)
        ?: throw UserNotFoundException("管理员不存在")
    val adminId = admin.id ?: throw BadRequestException("管理员ID无效")

    val result =
      when (input.action) {
        RefundAction.APPROVE ->
          refundReviewService.approveRefund(
            refundRequestId = refundRequestId,
            adminId = adminId,
            adminNote = input.adminNote,
            username = userDetails.username,
          )

        RefundAction.REJECT ->
          refundReviewService.rejectRefund(
            refundRequestId = refundRequestId,
            adminId = adminId,
            adminNote = input.adminNote,
            username = userDetails.username,
          )
      }
    return result.toRefundRequest(resolveTransactionId(result.paymentId))
  }

  private fun resolveTransactionId(paymentId: Long): String? = paymentRepository.findById(paymentId).map { it.transactionId }.orElse(null)
}
