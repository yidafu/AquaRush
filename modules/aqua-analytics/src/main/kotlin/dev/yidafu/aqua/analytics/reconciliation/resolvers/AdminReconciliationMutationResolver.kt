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

import dev.yidafu.aqua.analytics.reconciliation.mapper.toGraphQL
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.reconciliation.ReconciliationService
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyResolveAction
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTriggerType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.ExecuteReconciliationInput
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskType
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskVo
import dev.yidafu.aqua.common.graphql.generated.ResolveDiscrepancyInput
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
 * 管理端微信资金对账变更解析器
 * 手动执行对账(创建任务并异步执行) + 差异处置(解决/忽略)。
 * 权限:PERMISSION_PAYMENT_RECONCILIATION_EXECUTE(F-ORD-031/G93,
 * 与 RECONCILIATION_* 日报对账相互独立,见 spec/prd/01-overview.md §权限码目录)。
 */
@Controller
class AdminReconciliationMutationResolver(
  private val reconciliationService: ReconciliationService,
  private val adminService: AdminService,
  private val bizLogger: BizLogger,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminReconciliationMutationResolver::class.java)

  /**
   * 手动执行对账:创建任务并异步派发执行(账单日窗口 T-1 ~ T-30,R-PAY-04)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_RECONCILIATION_EXECUTE')")
  fun executeReconciliation(
    @Argument input: ExecuteReconciliationInput,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): ReconciliationTaskVo {
    if (reconciliationService.hasRunningTask()) {
      throw BadRequestException("已有对账任务正在运行，请稍后再试")
    }
    val date =
      try {
        LocalDate.parse(input.reconciliationDate)
      } catch (e: Exception) {
        throw BadRequestException("对账日期格式不正确，应为 yyyy-MM-dd")
      }
    // 账单日回溯窗口 [T-30, T-1](开放决策 §13-3 保守值;窗口外拒绝)
    val today = LocalDate.now()
    if (date.isAfter(today.minusDays(1)) || date.isBefore(today.minusDays(30))) {
      throw BadRequestException("仅支持近 30 天账单")
    }

    logger.info("管理员 {} 手动执行{}对账, 日期: {}", userDetails.username, input.taskType, date)

    val admin = adminService.findByUsername(userDetails.username)
    val task =
      when (input.taskType) {
        ReconciliationTaskType.PAYMENT ->
          reconciliationService.createPaymentReconciliationTask(
            date,
            ReconciliationTriggerType.MANUAL,
            admin?.id,
          )
        ReconciliationTaskType.REFUND ->
          reconciliationService.createRefundReconciliationTask(
            date,
            ReconciliationTriggerType.MANUAL,
            admin?.id,
          )
        ReconciliationTaskType.SETTLEMENT ->
          reconciliationService.createSettlementReconciliationTask(
            date,
            ReconciliationTriggerType.MANUAL,
            admin?.id,
          )
      }

    // 派发异步执行(不阻塞请求线程)
    when (input.taskType) {
      ReconciliationTaskType.PAYMENT -> reconciliationService.executePaymentReconciliation(task)
      ReconciliationTaskType.REFUND -> reconciliationService.executeRefundReconciliation(task)
      ReconciliationTaskType.SETTLEMENT -> reconciliationService.executeSettlementReconciliation(task)
    }

    // 业务日志(F-LOG-003 RECONCILIATION 域):执行微信资金对账
    bizLogger.logOperation(
      operation = "RECON_EXECUTE",
      module = "RECONCILIATION",
      result = "SUCCESS",
      target = task.taskId,
      userId = admin?.id,
      username = userDetails.username,
      additionalData = mapOf("taskType" to input.taskType.name, "reconciliationDate" to date.toString()),
    )
    return task.toGraphQL()
  }

  /**
   * 差异处置(解决/忽略,备注必填留痕;资金相关写操作,已处置不可再改)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_RECONCILIATION_EXECUTE')")
  fun resolveDiscrepancy(
    @Argument input: ResolveDiscrepancyInput,
    @AuthenticationPrincipal userDetails: UserDetails,
  ): Boolean {
    val admin =
      adminService.findByUsername(userDetails.username)
        ?: throw UserNotFoundException("管理员不存在")

    val resolved =
      reconciliationService.resolveDiscrepancy(
        discrepancyId = input.discrepancyId,
        action = input.action.toDomain(),
        resolutionNotes = input.resolutionNotes,
        resolvedBy = userDetails.username,
      )
    if (!resolved) {
      throw BadRequestException("对账差异不存在或处置失败")
    }

    // 业务日志(F-LOG-003 RECONCILIATION 域):差异处置
    bizLogger.logOperation(
      operation = "RECON_DISCREPANCY_RESOLVE",
      module = "RECONCILIATION",
      result = "SUCCESS",
      target = input.discrepancyId.toString(),
      userId = admin.id,
      username = userDetails.username,
      additionalData = mapOf("action" to input.action.name, "resolutionNotes" to input.resolutionNotes),
    )
    return true
  }

  private fun dev.yidafu.aqua.common.graphql.generated.DiscrepancyResolveAction.toDomain(): DiscrepancyResolveAction =
    when (this) {
      dev.yidafu.aqua.common.graphql.generated.DiscrepancyResolveAction.RESOLVE -> DiscrepancyResolveAction.RESOLVE
      dev.yidafu.aqua.common.graphql.generated.DiscrepancyResolveAction.IGNORE -> DiscrepancyResolveAction.IGNORE
    }
}
