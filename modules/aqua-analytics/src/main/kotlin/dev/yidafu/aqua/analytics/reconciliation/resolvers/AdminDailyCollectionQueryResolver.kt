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

import dev.yidafu.aqua.analytics.reconciliation.dto.OrderStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.dto.PaymentStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.mapper.toGraphQL
import dev.yidafu.aqua.analytics.reconciliation.service.DailyCollectionService
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.DailyCollectionListInput
import dev.yidafu.aqua.common.graphql.generated.DailyCollectionSummaryVo
import dev.yidafu.aqua.common.graphql.generated.DailyCollectionVoPage
import dev.yidafu.aqua.common.graphql.generated.DailyReconciliationListInput
import dev.yidafu.aqua.common.graphql.generated.DailyReconciliationVoPage
import dev.yidafu.aqua.common.graphql.generated.MyTodayCollectionVo
import dev.yidafu.aqua.common.graphql.generated.PageInfoVo
import dev.yidafu.aqua.common.graphql.generated.ReconciliationOrderDetailVo
import dev.yidafu.aqua.common.graphql.generated.ReconciliationOrderDetailVoPage
import dev.yidafu.aqua.order.mapper.toGraphQL
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 管理员端每日收款查询解析器（配送员小程序使用）
 */
@Controller
class AdminDailyCollectionQueryResolver(
  private val dailyCollectionService: DailyCollectionService,
  private val adminService: AdminService,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminDailyCollectionQueryResolver::class.java)

  /** Spring Data Page → GraphQL PageInfoVo(与 businessLogs 等分页查询同款映射) */
  private fun pageInfoOf(page: Page<*>): PageInfoVo =
    PageInfoVo(
      total = page.totalElements.toInt(),
      pageSize = page.size,
      pageNum = page.number,
      hasNext = page.hasNext(),
      hasPrevious = page.hasPrevious(),
      totalPages = page.totalPages,
    )

  /**
   * 分页查询收款记录(后台收银对账列表)
   * 支持按收款日期/送水工筛选;不传筛选条件时返回全部(分页,禁止全量拉取 —— AGENTS.md 规则 10)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_READ')")
  fun dailyCollections(
    @Argument input: DailyCollectionListInput?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): DailyCollectionVoPage {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val collectionDate = input?.collectionDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
    logger.info(
      "管理员查询收款记录, date={}, deliveryWorkerId={}, page={}, size={}",
      input?.collectionDate,
      input?.deliveryWorkerId,
      input?.page ?: 0,
      input?.size ?: 10,
    )

    val page =
      dailyCollectionService.getCollectionsPage(
        collectionDate = collectionDate,
        deliveryWorkerId = input?.deliveryWorkerId,
        page = input?.page ?: 0,
        size = input?.size ?: 10,
      )

    return DailyCollectionVoPage(
      list = page.content.map { it.toGraphQL() },
      pageInfo = pageInfoOf(page),
    )
  }

  /**
   * 分页查询对账记录(后台/送水工端,恒按对账日期倒序)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_READ')")
  fun dailyReconciliations(
    @Argument input: DailyReconciliationListInput?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): DailyReconciliationVoPage {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    logger.info("管理员查询对账记录列表, page={}, size={}", input?.page ?: 0, input?.size ?: 10)

    val page =
      dailyCollectionService.getReconciliationsPage(
        page = input?.page ?: 0,
        size = input?.size ?: 10,
      )

    return DailyReconciliationVoPage(
      list = page.content.map { it.toGraphQL() },
      pageInfo = pageInfoOf(page),
    )
  }

  /**
   * 指定日期收银汇总(后台收银对账顶部汇总卡):默认今日
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_READ')")
  fun dailyCollectionSummary(
    @Argument collectionDate: String?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): DailyCollectionSummaryVo {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val date = collectionDate?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) } ?: LocalDate.now()
    val summary = dailyCollectionService.getDailyCollectionSummary(date)
    return DailyCollectionSummaryVo(
      collectionDate = summary.collectionDate.toString(),
      totalCashAmountCents = summary.totalCashAmountCents,
      totalQrCodeAmountCents = summary.totalQrCodeAmountCents,
      totalWaterTicketCount = summary.totalWaterTicketCount,
      totalOrderCount = summary.totalOrderCount,
      totalOrderAmountCents = summary.totalOrderAmountCents,
      totalSubmittedCount = summary.totalSubmittedCount,
    )
  }

  /**
   * 对账逐单差异明细(G29):按对账日期分页返回当日「已送达」订单流水,
   * 与日报系统计算值同源,差异可定位到具体订单;可按送水工筛选。
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_READ')")
  fun dailyReconciliationOrderDetails(
    @Argument reconciliationDate: String,
    @Argument deliveryWorkerId: Long?,
    @Argument page: Int,
    @Argument size: Int,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): ReconciliationOrderDetailVoPage {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    val date = LocalDate.parse(reconciliationDate, DateTimeFormatter.ISO_LOCAL_DATE)
    logger.info(
      "查询对账逐单明细, date={}, deliveryWorkerId={}, page={}, size={}",
      reconciliationDate,
      deliveryWorkerId,
      page,
      size,
    )

    val orderPage =
      dailyCollectionService.getReconciliationOrderDetails(
        reconciliationDate = date,
        deliveryWorkerId = deliveryWorkerId,
        page = page,
        size = size,
      )

    return ReconciliationOrderDetailVoPage(
      list = orderPage.content.map { order -> orderToDetail(order) },
      pageInfo = pageInfoOf(orderPage),
    )
  }

  private fun orderToDetail(order: OrderModel): ReconciliationOrderDetailVo =
    ReconciliationOrderDetailVo(
      orderId = order.id ?: 0L,
      orderNo = order.orderNo,
      deliveryWorkerId = order.deliveryWorkerId,
      amountCents = order.amountCents,
      paymentType = order.paymentType?.let { it.toGraphQL() },
      status = order.status.toGraphQL(),
      deliveredAt = order.completedAt,
    )

  /**
   * 获取配送员今日收款预计算结果
   * 返回系统计算的订单统计和已填写的收款数据
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_RECONCILIATION_READ')")
  fun myTodayCollection(
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): MyTodayCollectionVo {
    // 检查用户是否已认证
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    logger.info("配送员 {} 查询今日收款预计算结果", userDetails.username)

    // 1. 获取当前配送员(账号 id 即送水员 id,单表)
    val workerId = adminService.findByUsername(userDetails.username)?.id ?: throw UserNotFoundException("配送员 ${userDetails.username} 不存在")
    val today = LocalDate.now()

    // 2. 查询今日订单统计（系统自动计算）
    val orderStats: OrderStatsDTO = dailyCollectionService.getTodayOrderStats(workerId, today)

    // 3. 查询已存在的记录（如果有）
    val existingRecord = dailyCollectionService.getCollectionByWorkerAndDate(workerId, today)

    // 4. 查询今日支付统计（从订单计算）
    val paymentStats: PaymentStatsDTO = dailyCollectionService.getTodayPaymentStats(workerId, today)

    val result =
      if (existingRecord != null) {
        MyTodayCollectionResult(
          orderCount = orderStats.orderCount,
          orderAmountCents = orderStats.orderAmountCents,
          cashAmountCents = existingRecord.cashAmountCents,
          waterTicketCount = existingRecord.waterTicketCount,
          qrCodeAmountCents = existingRecord.qrCodeAmountCents,
          differenceAmountCents = existingRecord.differenceAmountCents,
          // 如果已提交过（不是 DRAFT 状态），使用已有状态；否则返回 null 表示未提交
          status = existingRecord.status.takeIf { it != DailyCollectionStatus.DRAFT },
          // 系统计算的理论值
          calculatedCashAmountCents = existingRecord.calculatedCashAmountCents,
          calculatedQrCodeAmountCents = existingRecord.calculatedQrCodeAmountCents,
          calculatedWaterTicketCount = existingRecord.calculatedWaterTicketCount,
          calculatedOrderAmountCents = existingRecord.calculatedOrderAmountCents,
        )
      } else {
        // 没有记录，返回从订单计算的支付统计
        MyTodayCollectionResult(
          orderCount = orderStats.orderCount,
          orderAmountCents = orderStats.orderAmountCents,
          cashAmountCents = paymentStats.cashAmountCents,
          waterTicketCount = paymentStats.waterTicketCount,
          qrCodeAmountCents = paymentStats.qrCodeAmountCents,
          differenceAmountCents = null,
          status = null,
          // 系统计算的理论值（从 paymentStats 填充）
          calculatedCashAmountCents = paymentStats.cashAmountCents,
          calculatedQrCodeAmountCents = paymentStats.qrCodeAmountCents,
          calculatedWaterTicketCount = paymentStats.waterTicketCount,
          calculatedOrderAmountCents = orderStats.orderAmountCents,
        )
      }
    // 4. 构建返回对象
    return result.toGraphQL()
  }
}

/**
 * 配送员今日收款预计算结果
 */
data class MyTodayCollectionResult(
  val orderCount: Int,
  val orderAmountCents: Long,
  val cashAmountCents: Long?,
  val waterTicketCount: Int?,
  val qrCodeAmountCents: Long?,
  val differenceAmountCents: Long?,
  val status: DailyCollectionStatus?,
  // 系统计算的理论值
  val calculatedCashAmountCents: Long?,
  val calculatedQrCodeAmountCents: Long?,
  val calculatedWaterTicketCount: Int?,
  val calculatedOrderAmountCents: Long?,
)
