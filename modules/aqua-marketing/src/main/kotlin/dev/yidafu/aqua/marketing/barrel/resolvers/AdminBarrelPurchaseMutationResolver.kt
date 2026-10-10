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

package dev.yidafu.aqua.marketing.barrel.resolvers

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseMutationService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.BarrelPurchaseAdminVo
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.mapper.BarrelPurchaseModelMapper
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller

/**
 * 管理端购桶单变更解析器(G06:管理员手动退还水桶费)
 * 退还流程:校验购桶单已支付 → 原交易执行水桶费退款(微信/假支付)→ 购桶单置「已退还」留痕。
 * 无交易号(异常/线下场景):跳过退款执行,仅落记录,人工线下处理。
 */
@Controller
class AdminBarrelPurchaseMutationResolver(
  private val barrelPurchaseQueryService: BarrelPurchaseQueryService,
  private val barrelPurchaseMutationService: BarrelPurchaseMutationService,
  private val paymentService: PaymentService,
  private val adminService: AdminService,
  private val bizLogger: BizLogger,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminBarrelPurchaseMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_REFUND')")
  fun refundBarrelPurchase(
    @Argument purchaseId: Long,
    @Argument remark: String?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): BarrelPurchaseAdminVo {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val admin =
      adminService.findByUsername(userDetails.username)
        ?: throw BadRequestException("管理员不存在")
    val adminId = admin.id ?: throw BadRequestException("管理员ID无效")

    val purchase =
      barrelPurchaseQueryService.getById(purchaseId)
        ?: throw BadRequestException("购桶单不存在: $purchaseId")
    if (purchase.paymentStatus != BarrelPaymentStatus.PAID) {
      throw BadRequestException("购桶单状态为 ${purchase.paymentStatus},不可退还")
    }

    // 原交易执行水桶费退款(仅线上支付购桶单有交易号;无交易号仅落记录人工处理)
    val transactionId = purchase.transactionId
    if (!transactionId.isNullOrEmpty()) {
      try {
        paymentService.refund(
          transactionId = transactionId,
          refundAmountCents = purchase.totalAmountCents,
          totalAmountCents = purchase.totalAmountCents,
//          reason = "退还水桶费 - 购桶单号: ${purchase.orderNo}",
//          refundType = RefundType.DEPOSIT_REFUND,
        )
        logger.info("水桶费退款执行成功: purchaseId={}, transactionId={}", purchaseId, transactionId)
      } catch (e: Exception) {
        logger.error("水桶费退款执行失败: purchaseId={}", purchaseId, e)
        throw BadRequestException("水桶费退款执行失败: ${e.message}")
      }
    } else {
      logger.warn("购桶单 {} 无交易号,仅落退还记录人工线下处理", purchaseId)
    }

    val refunded = barrelPurchaseMutationService.markRefunded(purchaseId, adminId, remark)

    // 业务日志(F-LOG-003 PAYMENT 域):退还水桶费
    bizLogger.logOperation(
      operation = "BARREL_REFUNDED",
      module = "PAYMENT",
      result = "SUCCESS",
      target = purchaseId.toString(),
      userId = adminId,
      username = userDetails.username,
      additionalData =
        mapOf(
          "orderNo" to purchase.orderNo,
          "totalAmountCents" to purchase.totalAmountCents,
          "remark" to (remark ?: ""),
        ),
    )
    return BarrelPurchaseModelMapper.map(
      refunded,
      barrelPurchaseQueryService.findItemsByPurchaseId(purchaseId),
    )
  }
}
