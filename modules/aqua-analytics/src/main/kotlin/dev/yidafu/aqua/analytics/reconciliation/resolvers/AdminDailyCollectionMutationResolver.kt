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

package dev.yidafu.aqua.analytics.reconciliation.resolvers

import dev.yidafu.aqua.analytics.reconciliation.service.DailyCollectionService
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerQueryService
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.ConfirmDailyCollectionInput
import dev.yidafu.aqua.common.graphql.generated.ReviewReconciliationInput
import dev.yidafu.aqua.common.graphql.generated.SubmitDailyCollectionInput
import dev.yidafu.aqua.logging.util.BizLogger
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
  private val bizLogger: BizLogger,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminDailyCollectionMutationResolver::class.java)

  /**
   * 配送员或管理员提交每日收款
   * 配送员录入时会自动从当前用户关联的配送员获取ID
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_SUBMIT')")
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

    val record =
      dailyCollectionService.submitDailyCollection(
        deliveryWorkerId = workerId,
        cashAmountCents = input.cashAmountCents,
        waterTicketCount = input.waterTicketCount,
        qrCodeAmountCents = input.qrCodeAmountCents,
        collectionDate = date,
      )

    // 业务日志(F-LOG-003 RECONCILIATION 域):提交每日收款
    bizLogger.logOperation(
      operation = "RECON_DAILY_SUBMIT",
      module = "RECONCILIATION",
      result = "SUCCESS",
      target = record.id?.toString() ?: "daily-collection",
      userId = adminId,
      username = userDetails.username,
      additionalData =
        mapOf(
          "deliveryWorkerId" to workerId,
          "collectionDate" to date.toString(),
          "cashAmountCents" to input.cashAmountCents,
          "waterTicketCount" to input.waterTicketCount,
          "qrCodeAmountCents" to input.qrCodeAmountCents,
        ),
    )
    return record
  }

  /**
   * 管理员确认收款记录
   */
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_CONFIRM')")
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
    val admin =
      adminService.findByUsername(userDetails.username)
        ?: throw BadRequestException("管理员不存在")
    val adminId = admin.id ?: throw BadRequestException("管理员ID无效")
    val record =
      dailyCollectionService.confirmCollection(
        recordId = input.recordId,
        confirmedBy = adminId,
        notes = input.notes,
      )

    // 业务日志(F-LOG-003 RECONCILIATION 域):确认每日收款
    bizLogger.logOperation(
      operation = "RECON_DAILY_CONFIRM",
      module = "RECONCILIATION",
      result = "SUCCESS",
      target = record.id?.toString() ?: input.recordId.toString(),
      userId = adminId,
      username = userDetails.username,
      additionalData = mapOf("recordId" to input.recordId, "notes" to (input.notes ?: "")),
    )
    return record
  }

  /**
   * 管理员复核对账记录
   */
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_REVIEW')")
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
    val admin =
      adminService.findByUsername(userDetails.username)
        ?: throw BadRequestException("管理员不存在")
    val adminId = admin.id ?: throw BadRequestException("管理员ID无效")
    val model =
      dailyCollectionService.reviewReconciliation(
        reconciliationId = input.reconciliationId,
        reviewedBy = adminId,
        reviewNotes = input.reviewNotes,
      )

    // 业务日志(F-LOG-003 RECONCILIATION 域):对账复核
    bizLogger.logOperation(
      operation = "RECON_REVIEW",
      module = "RECONCILIATION",
      result = "SUCCESS",
      target = input.reconciliationId.toString(),
      userId = adminId,
      username = userDetails.username,
      additionalData = mapOf("reviewNotes" to input.reviewNotes),
    )
    return model
  }

  /**
   * 手动执行每日对账
   * 管理员可以手动触发对账任务
   * 如果记录已存在会更新（删除后重新创建）
   */
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_EXECUTE')")
  @MutationMapping
  fun executeDailyReconciliation(
    @Argument reconciliationDate: String?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): DailyReconciliationModel {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val date =
      reconciliationDate?.let {
        LocalDate.parse(it)
      } ?: LocalDate.now().minusDays(1)

    logger.info("管理员 {} 手动触发对账: {}", userDetails.username, date)
    val admin = adminService.findByUsername(userDetails.username)
    dailyCollectionService.executeDailyReconciliation(date)
    // 返回更新后的对账记录
    val result = dailyCollectionService.getReconciliationByDate(date) ?: throw BadRequestException("对账执行失败")

    // 业务日志(F-LOG-003 RECONCILIATION 域):执行每日对账
    bizLogger.logOperation(
      operation = "RECON_EXECUTE",
      module = "RECONCILIATION",
      result = "SUCCESS",
      target = result.id?.toString() ?: date.toString(),
      userId = admin?.id,
      username = userDetails.username,
      additionalData = mapOf("reconciliationDate" to date.toString()),
    )
    return result
  }
}
