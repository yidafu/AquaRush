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

package dev.yidafu.aqua.analytics.admin.reconciliation.resolvers

import dev.yidafu.aqua.analytics.reconciliation.dto.OrderStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.dto.PaymentStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.mapper.MyTodayCollectionResultMapper
import dev.yidafu.aqua.analytics.reconciliation.service.DailyCollectionService
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.UnauthorizedException
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.graphql.generated.MyTodayCollectionVo
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
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
  private val deliveryWorkerRepository: DeliveryWorkerRepository,
  private val adminService: AdminService,
) : BaseGraphQLResolver() {
  private val logger = LoggerFactory.getLogger(AdminDailyCollectionQueryResolver::class.java)

  /**
   * 获取指定日期的收款记录列表
   * 如果不传日期，则返回所有记录
   */
  @QueryMapping
  fun dailyCollections(
    @Argument collectionDate: String?,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): List<DailyCollectionRecordModel> {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    logger.info("管理员查询收款记录, date={}", collectionDate)

    // 如果不传日期，返回所有记录
    if (collectionDate == null) {
      return dailyCollectionService.getAllCollections().ifEmpty { emptyList() }
    }

    val date = LocalDate.parse(collectionDate, DateTimeFormatter.ISO_LOCAL_DATE)

    // 获取当前管理员
    val admin =
      adminService.findByUsername(userDetails.username)
        ?: throw UserNotFoundException("管理员不存在")

    val adminId = admin.id ?: throw BadRequestException("管理员ID无效")

    return dailyCollectionService.getCollectionsByDate(adminId, date).ifEmpty { emptyList() }
  }

  /**
   * 获取对账记录列表
   * 注意: 当前返回全部数据，前端分页处理
   * TODO: 后续可添加后端分页支持
   */
  @QueryMapping
  fun dailyReconciliations(
    @Argument page: Int = 0,
    @Argument size: Int = 20,
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): List<DailyReconciliationModel> {
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    logger.info("管理员查询对账记录列表, page={}, size={}", page, size)

    val allReconciliations =
      dailyCollectionService
        .getAllReconciliations()
        .sortedByDescending { it.reconciliationDate }

    // TODO: 实现后端分页
    return allReconciliations
  }

  /**
   * 获取配送员今日收款预计算结果
   * 返回系统计算的订单统计和已填写的收款数据
   */
  @QueryMapping
  fun myTodayCollection(
    @AuthenticationPrincipal userDetails: UserDetails?,
  ): MyTodayCollectionVo {
    // 检查用户是否已认证
    if (userDetails == null) {
      throw UnauthorizedException("请先登录")
    }
    logger.info("配送员 {} 查询今日收款预计算结果", userDetails.username)

    // 1. 获取当前配送员
    val adminId = adminService.findByUsername(userDetails.username)?.id ?: throw UserNotFoundException("配送员 ${userDetails.username} 不存在")
    val worker =
      deliveryWorkerRepository.findByAdminId(adminId)
        ?: throw BadRequestException("您还没有绑定配送员账号")

    val workerId = worker.id ?: throw BadRequestException("配送员ID无效")
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
    return MyTodayCollectionResultMapper.map(result)
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
