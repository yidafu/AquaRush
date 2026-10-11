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
import dev.yidafu.aqua.api.service.reconciliation.ReconciliationService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.DiscrepancyStatus
import dev.yidafu.aqua.common.graphql.generated.DiscrepancyType
import dev.yidafu.aqua.common.graphql.generated.ReconciliationDiscrepancyVoPage
import dev.yidafu.aqua.common.graphql.generated.ReconciliationReportVo
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskStatus
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskType
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskVo
import dev.yidafu.aqua.common.graphql.generated.ReconciliationTaskVoPage
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import java.time.LocalDate

/**
 * 管理端微信资金对账查询解析器
 * 对账任务/差异/报表查询;权限:PERMISSION_PAYMENT_RECONCILIATION_READ(F-ORD-031/G93,
 * 与 RECONCILIATION_* 日报对账相互独立,见 spec/prd/01-overview.md §权限码目录)。
 */
@Controller
class AdminReconciliationQueryResolver(
  private val reconciliationService: ReconciliationService,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminReconciliationQueryResolver::class.java)

  /**
   * 对账任务分页查询(类型/状态/对账日期范围筛选)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_RECONCILIATION_READ')")
  fun reconciliationTasks(
    @Argument taskType: ReconciliationTaskType?,
    @Argument status: ReconciliationTaskStatus?,
    @Argument dateFrom: String?,
    @Argument dateTo: String?,
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): ReconciliationTaskVoPage {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    logger.info(
      "管理员查询对账任务列表, taskType={}, status={}, dateFrom={}, dateTo={}, page={}, size={}",
      taskType,
      status,
      dateFrom,
      dateTo,
      page,
      size,
    )
    val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
    val dateFromParsed = dateFrom?.let { parseDate(it, "dateFrom") }
    val dateToParsed = dateTo?.let { parseDate(it, "dateTo") }
    // GraphQL 生成枚举 → 域枚举(同名常量)
    val taskTypeDomain =
      taskType?.let { it.toDomain() }
    val statusDomain =
      status?.let { it.toDomain() }
    val result =
      reconciliationService.getReconciliationTasksPaged(
        taskType = taskTypeDomain,
        status = statusDomain,
        dateFrom = dateFromParsed,
        dateTo = dateToParsed,
        pageable = pageable,
      )
    return ReconciliationTaskVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /**
   * 对账任务详情
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_RECONCILIATION_READ')")
  fun reconciliationTask(
    @Argument taskId: String,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): ReconciliationTaskVo? {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    if (taskId.isBlank()) {
      throw BadRequestException("任务ID不能为空")
    }
    return reconciliationService
      .getReconciliationTask(taskId)
      ?.toGraphQL()
  }

  /**
   * 对账差异分页查询(任务ID/差异类型/解决状态筛选)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_RECONCILIATION_READ')")
  fun reconciliationDiscrepancies(
    @Argument taskId: String?,
    @Argument discrepancyType: DiscrepancyType?,
    @Argument status: DiscrepancyStatus?,
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): ReconciliationDiscrepancyVoPage {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    logger.info(
      "管理员查询对账差异列表, taskId={}, discrepancyType={}, status={}, page={}, size={}",
      taskId,
      discrepancyType,
      status,
      page,
      size,
    )
    val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))
    // GraphQL 生成枚举 → 域枚举(同名常量)
    val typeDomain = discrepancyType?.let { it.toDomain() }
    val statusDomain =
      status?.let { it.toDomain() }
    val result =
      reconciliationService.getDiscrepanciesPaged(
        taskId = taskId,
        discrepancyType = typeDomain,
        status = statusDomain,
        pageable = pageable,
      )
    return ReconciliationDiscrepancyVoPage(
      content = result.content.map { it.toGraphQL() },
      totalElements = result.totalElements.toInt(),
      totalPages = result.totalPages,
      size = result.size,
      number = result.number,
    )
  }

  /**
   * 对账报表列表(按任务,可选报表类型过滤)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_PAYMENT_RECONCILIATION_READ')")
  fun reconciliationReports(
    @Argument taskId: String,
    @Argument reportType: String?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): List<ReconciliationReportVo> {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    if (taskId.isBlank()) {
      throw BadRequestException("任务ID不能为空")
    }
    return reconciliationService
      .getReports(taskId, reportType)
      .map { it.toGraphQL() }
  }

  private fun parseDate(
    value: String,
    argName: String,
  ): LocalDate =
    try {
      LocalDate.parse(value)
    } catch (e: Exception) {
      throw BadRequestException("参数 $argName 日期格式不正确，应为 yyyy-MM-dd")
    }
}

private fun ReconciliationTaskType.toDomain(): dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType =
  when (this) {
    ReconciliationTaskType.PAYMENT -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.PAYMENT
    ReconciliationTaskType.REFUND -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.REFUND
    ReconciliationTaskType.SETTLEMENT -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskType.SETTLEMENT
  }

private fun ReconciliationTaskStatus.toDomain(): dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus =
  when (this) {
    ReconciliationTaskStatus.FAILED -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.FAILED
    ReconciliationTaskStatus.PENDING -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.PENDING
    ReconciliationTaskStatus.RUNNING -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.RUNNING
    ReconciliationTaskStatus.SUCCESS -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.SUCCESS
    ReconciliationTaskStatus.SKIPPED -> dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus.SKIPPED
  }

private fun DiscrepancyStatus.toDomain(): dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus =
  when (this) {
    DiscrepancyStatus.RESOLVED -> dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.RESOLVED
    DiscrepancyStatus.UNRESOLVED -> dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.UNRESOLVED
    DiscrepancyStatus.IGNORED -> dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus.IGNORED
  }

private fun DiscrepancyType.toDomain(): dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType =
  when (this) {
    DiscrepancyType.MISSING -> dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.MISSING
    DiscrepancyType.MISMATCH -> dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.MISMATCH
    DiscrepancyType.EXTRA -> dev.yidafu.aqua.common.domain.model.enums.DiscrepancyType.EXTRA
  }
