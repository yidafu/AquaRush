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

import dev.yidafu.aqua.api.service.deposit.DepositRefundMutationService
import dev.yidafu.aqua.common.annotation.AdminService
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.DepositRefundApplicationVo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.barrel.mapper.toGraphQL
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 管理端退押流转(R-PRO-04/F-PRO-012,设计02 §5.1;PERMISSION_PRODUCT_WRITE):
 * 空桶回收确认(PENDING→RECOVERED)→ 审核退还(RECOVERED→REFUNDED,微信退款 DEPOSIT_REFUND)
 * / 驳回(PENDING/RECOVERED→REJECTED 留原因)。
 */
@Controller
@AdminService
class AdminDepositRefundMutationResolver(
  private val depositRefundMutationService: DepositRefundMutationService,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminDepositRefundMutationResolver::class.java)

  /** 空桶回收确认 */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  fun confirmBarrelRecovered(
    @Argument applicationId: Long,
    @AuthenticationPrincipal principal: UserPrincipal?,
  ): DepositRefundApplicationVo {
    val adminId = requireAdminId(principal)
    return depositRefundMutationService.confirmRecovered(applicationId, adminId).toGraphQL()
  }

  /** 退押审核通过(执行微信退款 + 押金记录 HELD→REFUNDED) */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  fun approveDepositRefund(
    @Argument applicationId: Long,
    @AuthenticationPrincipal principal: UserPrincipal?,
  ): DepositRefundApplicationVo {
    val adminId = requireAdminId(principal)
    val app = depositRefundMutationService.approve(applicationId, adminId)
    logger.info("退押审核通过: applicationId={}, adminId={}", applicationId, adminId)
    return app.toGraphQL()
  }

  /** 退押驳回(留原因) */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PRODUCT_WRITE')")
  fun rejectDepositRefund(
    @Argument applicationId: Long,
    @Argument reason: String,
    @AuthenticationPrincipal principal: UserPrincipal?,
  ): DepositRefundApplicationVo {
    val adminId = requireAdminId(principal)
    return depositRefundMutationService.reject(applicationId, adminId, reason).toGraphQL()
  }

  private fun requireAdminId(principal: UserPrincipal?): Long =
    principal?.id ?: throw dev.yidafu.aqua.common.exception
      .UnauthorizedException("请先登录")
}
