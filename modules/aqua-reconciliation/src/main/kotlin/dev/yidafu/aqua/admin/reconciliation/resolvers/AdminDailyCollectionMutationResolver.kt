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

package dev.yidafu.aqua.admin.reconciliation.resolvers

import dev.yidafu.aqua.api.service.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerQueryService
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.ConfirmDailyCollectionInput
import dev.yidafu.aqua.common.graphql.generated.ReviewReconciliationInput
import dev.yidafu.aqua.common.graphql.generated.SubmitDailyCollectionInput
import dev.yidafu.aqua.reconciliation.service.DailyCollectionService
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import java.time.LocalDate

/**
 * 管理端每日收款和对账变更解析器
 */
@Controller
class AdminDailyCollectionMutationResolver(
  private val dailyCollectionService: DailyCollectionService,
  private val deliveryWorkerQueryResolver: DeliveryWorkerQueryService,
  private val adminService: AdminService,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminDailyCollectionMutationResolver::class.java)

  /**
   * 配送员或管理员提交每日收款
   * 配送员录入时会自动从当前用户关联的配送员获取ID
   */
  @MutationMapping
  fun submitDailyCollection(
    @Argument input: SubmitDailyCollectionInput,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): DailyCollectionRecordModel {
    // 从当前用户获取配送员ID
    val adminId = adminService.findByUsername(userDetails.username)?.id ?: throw BadRequestException("用户不存在")
    val workerId =
      deliveryWorkerQueryResolver.getWorkerByAdminId(adminId).id
        ?: throw BadRequestException("未绑定配送员")

    val date = input.collectionDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

    return dailyCollectionService.submitDailyCollection(
      deliveryWorkerId = workerId,
      cashAmountCents = input.cashAmountCents,
      waterTicketCount = input.waterTicketCount,
      qrCodeAmountCents = input.qrCodeAmountCents,
      collectionDate = date,
    )
  }

  /**
   * 管理员确认收款记录
   */
  @PreAuthorize("hasRole('ADMIN')")
  @MutationMapping
  fun confirmDailyCollection(
    @Argument input: ConfirmDailyCollectionInput?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): DailyCollectionRecordModel {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    if (input == null || input.recordId == null) {
      throw BadRequestException("收款记录ID不能为空")
    }
    val admin = adminService.findByUsername(userDetails.username)
      ?: throw BadRequestException("管理员不存在")
    val adminId = admin.id ?: throw BadRequestException("管理员ID无效")
    return dailyCollectionService.confirmCollection(
      recordId = input.recordId,
      confirmedBy = adminId,
      notes = input.notes,
    )
  }

  /**
   * 管理员复核对账记录
   */
  @PreAuthorize("hasRole('ADMIN')")
  @MutationMapping
  fun reviewReconciliation(
    @Argument input: ReviewReconciliationInput?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): DailyReconciliationModel {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    if (input == null || input.reconciliationId == null) {
      throw BadRequestException("对账记录ID不能为空")
    }
    if (input.reviewNotes.isNullOrBlank()) {
      throw BadRequestException("复核备注不能为空")
    }
    val admin = adminService.findByUsername(userDetails.username)
      ?: throw BadRequestException("管理员不存在")
    val adminId = admin.id ?: throw BadRequestException("管理员ID无效")
    return dailyCollectionService.reviewReconciliation(
      reconciliationId = input.reconciliationId,
      reviewedBy = adminId,
      reviewNotes = input.reviewNotes,
    )
  }

  /**
   * 手动执行每日对账
   * 管理员可以手动触发对账任务
   * 如果记录已存在会更新（删除后重新创建）
   */
  @PreAuthorize("hasRole('ADMIN')")
  @MutationMapping
  fun executeDailyReconciliation(
    @Argument reconciliationDate: String?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): DailyReconciliationModel {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val date = reconciliationDate?.let {
      LocalDate.parse(it)
    } ?: LocalDate.now().minusDays(1)

    logger.info("管理员 {} 手动触发对账: {}", userDetails.username, date)
    dailyCollectionService.executeDailyReconciliation(date)
    // 返回更新后的对账记录
    return dailyCollectionService.getReconciliationByDate(date)
      ?: throw BadRequestException("对账执行失败")
  }
}
