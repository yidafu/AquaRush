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
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.SubmitDailyCollectionInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.reconciliation.service.DailyCollectionService
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
    @Argument recordId: Long,
    @Argument notes: String?,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): DailyCollectionRecordModel =
    dailyCollectionService.confirmCollection(
      recordId = recordId,
      confirmedBy = userPrincipal.id,
      notes = notes,
    )

  /**
   * 管理员复核对账记录
   */
  @PreAuthorize("hasRole('ADMIN')")
  @MutationMapping
  fun reviewReconciliation(
    @Argument reconciliationId: Long,
    @Argument reviewNotes: String,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): DailyReconciliationModel =
    dailyCollectionService.reviewReconciliation(
      reconciliationId = reconciliationId,
      reviewedBy = userPrincipal.id,
      reviewNotes = reviewNotes,
    )
}
