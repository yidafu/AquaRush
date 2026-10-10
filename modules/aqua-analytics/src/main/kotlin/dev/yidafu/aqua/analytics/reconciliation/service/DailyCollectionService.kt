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

package dev.yidafu.aqua.analytics.reconciliation.service

import dev.yidafu.aqua.analytics.reconciliation.dto.DailyCollectionSummaryDTO
import dev.yidafu.aqua.analytics.reconciliation.dto.OrderStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.dto.PaymentStatsDTO
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.reconciliation.DailyCollectionMutationService
import dev.yidafu.aqua.api.service.reconciliation.DailyCollectionQueryApiService
import dev.yidafu.aqua.api.service.reconciliation.DailyReconciliationMutationService
import dev.yidafu.aqua.api.service.reconciliation.DailyReconciliationQueryApiService
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.common.exception.BadRequestException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 每日收银对账服务
 */
@Service
class DailyCollectionService(
  private val dailyCollectionQuery: DailyCollectionQueryApiService,
  private val dailyReconciliationQuery: DailyReconciliationQueryApiService,
  private val orderQueryService: OrderQueryService,
  private val dailyCollectionMutation: DailyCollectionMutationService,
  private val dailyReconciliationMutation: DailyReconciliationMutationService,
) {
  private val logger = LoggerFactory.getLogger(DailyCollectionService::class.java)

  companion object {
    // 容差金额（1元 = 100分）
    private const val TOLERANCE_AMOUNT_CENTS = 100L
  }

  /**
   * 获取配送员今日按支付类型的收款统计
   * 从已送达的订单中按支付类型统计(含待确认收货 + 已完成,R-ORD-11 ——
   * 收款在送达当场发生,按送达时间计入,不受用户确认收货延迟影响)
   */
  fun getTodayPaymentStats(
    deliveryWorkerId: Long,
    date: LocalDate = LocalDate.now(),
  ): PaymentStatsDTO {
    val startOfDay = date.atStartOfDay()
    val endOfDay = date.atTime(LocalTime.MAX)

    // 统计现金订单
    val cashResult =
      orderQueryService.countAndSumDeliveredByPaymentType(
        deliveryWorkerId,
        PaymentType.CASH,
        startOfDay,
        endOfDay,
      )
    val cashAmountCents = if (cashResult[0] != null && cashResult[0] > 0) cashResult[1] ?: 0L else 0L
    val cashOrderCount = (cashResult[0] ?: 0L).toInt()

    // 统计水票订单
    val waterTicketResult =
      orderQueryService.countAndSumDeliveredByPaymentType(
        deliveryWorkerId,
        PaymentType.WATER_TICKET,
        startOfDay,
        endOfDay,
      )
    val waterTicketCount = (waterTicketResult[0] ?: 0L).toInt()

    // 统计扫码订单
    val qrCodeResult =
      orderQueryService.countAndSumDeliveredByPaymentType(
        deliveryWorkerId,
        PaymentType.QR_CODE,
        startOfDay,
        endOfDay,
      )
    val qrCodeAmountCents = if (qrCodeResult[0] != null && qrCodeResult[0] > 0) qrCodeResult[1] ?: 0L else 0L

    return PaymentStatsDTO(
      cashAmountCents = cashAmountCents,
      waterTicketCount = waterTicketCount,
      qrCodeAmountCents = qrCodeAmountCents,
    )
  }

  /**
   * 获取配送员今日订单统计（系统自动计算,已送达口径含待确认收货,R-ORD-11）
   */
  fun getTodayOrderStats(
    deliveryWorkerId: Long,
    date: LocalDate = LocalDate.now(),
  ): OrderStatsDTO {
    val startOfDay = date.atStartOfDay()
    val endOfDay = date.atTime(LocalTime.MAX)

    val orderCount = orderQueryService.countDeliveredByDateRange(startOfDay, endOfDay, deliveryWorkerId).toInt()

    val orderAmountCents = orderQueryService.sumDeliveredAmountCentsByDateRange(startOfDay, endOfDay, deliveryWorkerId)

    return OrderStatsDTO(orderCount, orderAmountCents)
  }

  /**
   * 配送员录入今日收款
   *
   * 差异口径(R-COL-02/R-COL-04):填报(现金+扫码) 与 系统计算(现金+扫码) 比对;
   * 水票订单金额(应收但不由送水工收钱)不计入金额差异基数,水票按张数精确比对。
   * 超容差或水票数量不符 → 「有争议」(DISPUTED,G28),差异说明(notes)随提交落库(G30),
   * 管理员确认/驳回;驳回退回草稿可改可重提,重新提交清空驳回留痕。
   */
  @Transactional
  fun submitDailyCollection(
    deliveryWorkerId: Long,
    cashAmountCents: Long,
    waterTicketCount: Int,
    qrCodeAmountCents: Long,
    collectionDate: LocalDate = LocalDate.now(),
    notes: String? = null,
  ): DailyCollectionRecordModel {
    logger.info(
      "配送员 {} 提交每日收款: cash={}, waterTicket={}, qrCode={}, date={}",
      deliveryWorkerId,
      cashAmountCents,
      waterTicketCount,
      qrCodeAmountCents,
      collectionDate,
    )

    // 已确认(CONFIRMED)日报为终态,不可被提交覆盖(裁决缺陷 #4,保留设计;
    // 前置拦截,统计查询与落库均不执行;驳回/确认流转不受影响)
    val existingRecord =
      dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(
        deliveryWorkerId,
        collectionDate,
      )
    if (existingRecord?.status == DailyCollectionStatus.CONFIRMED) {
      throw BadRequestException("该日收款记录已确认,不可修改;如需修改请先联系管理员驳回")
    }

    // 查询当日完成的订单数量和金额
    val startOfDay = collectionDate.atStartOfDay()
    val endOfDay = collectionDate.atTime(LocalTime.MAX)

    // 获取按支付类型的收款统计
    val paymentStats = getTodayPaymentStats(deliveryWorkerId, collectionDate)

    // 统计已送达订单数量和金额(含待确认收货 + 已完成,按送达时间,R-ORD-11)
    val orderCount = orderQueryService.countDeliveredByDateRange(startOfDay, endOfDay, deliveryWorkerId).toInt()

    val orderAmountCents = orderQueryService.sumDeliveredAmountCentsByDateRange(startOfDay, endOfDay, deliveryWorkerId)

    // 计算实际收款金额（现金 + 扫码）
    val totalCollectionAmount = cashAmountCents + qrCodeAmountCents

    // 差异口径:填报(现金+扫码) vs 系统计算(现金+扫码) —— 水票订单金额不计入应收基数,
    // 避免使用水票的日期产生系统性「负差异」(此前错误地以全部订单金额作基数)
    val calculatedCollectionAmount = paymentStats.cashAmountCents + paymentStats.qrCodeAmountCents
    val differenceAmountCents = totalCollectionAmount - calculatedCollectionAmount

    // 判断状态:超容差或水票数量不符 → 「有争议」(DISPUTED,G28/R-COL-02)
    val status =
      when {
        kotlin.math.abs(differenceAmountCents) <= TOLERANCE_AMOUNT_CENTS &&
          waterTicketCount == paymentStats.waterTicketCount -> {
          DailyCollectionStatus.SUBMITTED // 容差范围内且水票精确匹配，标记为已提交
        }

        else -> {
          DailyCollectionStatus.DISPUTED // 差异过大/水票不符，进入争议流程由管理员处理(G28)
        }
      }

    // 创建或更新记录(现有记录已在方法开头查得并完成 CONFIRMED 守卫)
    val record =
      existingRecord?.apply {
        this.cashAmountCents = cashAmountCents
        this.waterTicketCount = waterTicketCount
        this.qrCodeAmountCents = qrCodeAmountCents
        this.orderCount = orderCount
        this.orderAmountCents = orderAmountCents
        this.differenceAmountCents = differenceAmountCents
        this.status = status
        this.notes = notes
        this.updatedAt = LocalDateTime.now()
        // 重新提交清空驳回留痕(驳回退回草稿可改可重提,G28)
        this.rejectedAt = null
        this.rejectedBy = null
        this.rejectReason = null
        // 填充计算字段
        this.calculatedCashAmountCents = paymentStats.cashAmountCents
        this.calculatedQrCodeAmountCents = paymentStats.qrCodeAmountCents
        this.calculatedWaterTicketCount = paymentStats.waterTicketCount
        this.calculatedOrderAmountCents = orderAmountCents
      }
        ?: DailyCollectionRecordModel(
          deliveryWorkerId = deliveryWorkerId,
          collectionDate = collectionDate,
          cashAmountCents = cashAmountCents,
          waterTicketCount = waterTicketCount,
          qrCodeAmountCents = qrCodeAmountCents,
          orderCount = orderCount,
          orderAmountCents = orderAmountCents,
          differenceAmountCents = differenceAmountCents,
          status = status,
          notes = notes,
          // 填充计算字段
          calculatedCashAmountCents = paymentStats.cashAmountCents,
          calculatedQrCodeAmountCents = paymentStats.qrCodeAmountCents,
          calculatedWaterTicketCount = paymentStats.waterTicketCount,
          calculatedOrderAmountCents = orderAmountCents,
        )

    return dailyCollectionMutation.save(record)
  }

  /**
   * 管理员确认收款记录
   */
  @Transactional
  fun confirmCollection(
    recordId: Long,
    confirmedBy: Long,
    notes: String? = null,
  ): DailyCollectionRecordModel {
    val record =
      dailyCollectionQuery.findById(recordId)
        ?: throw IllegalArgumentException("收款记录不存在: $recordId")

    record.status = DailyCollectionStatus.CONFIRMED
    record.confirmedAt = LocalDateTime.now()
    record.confirmedBy = confirmedBy
    record.notes = notes
    record.updatedAt = LocalDateTime.now()

    logger.info("管理员 {} 确认收款记录 {}", confirmedBy, recordId)

    return dailyCollectionMutation.save(record)
  }

  /**
   * 管理员驳回收款记录(G28/R-COL-02:争议日报驳回,退回草稿可改可重提)
   *
   * 仅允许驳回「已提交」(SUBMITTED)与「有争议」(DISPUTED)状态;已确认(CONFIRMED)
   * 不可驳回(终态),草稿(DRAFT)无需驳回。驳回置状态为「草稿」并留痕
   * rejected_at/by/reject_reason;配送员重新提交时清空驳回留痕(见 submitDailyCollection)。
   */
  @Transactional
  fun rejectCollection(
    recordId: Long,
    rejectedBy: Long,
    reason: String,
  ): DailyCollectionRecordModel {
    if (reason.isBlank()) {
      throw BadRequestException("驳回原因不能为空")
    }
    val record =
      dailyCollectionQuery.findById(recordId)
        ?: throw BadRequestException("收款记录不存在: $recordId")

    if (record.status != DailyCollectionStatus.SUBMITTED && record.status != DailyCollectionStatus.DISPUTED) {
      throw BadRequestException("仅「已提交/有争议」状态的收款记录可驳回,当前状态: ${record.status}")
    }

    record.status = DailyCollectionStatus.DRAFT
    record.rejectedAt = LocalDateTime.now()
    record.rejectedBy = rejectedBy
    record.rejectReason = reason
    record.updatedAt = LocalDateTime.now()

    logger.info("管理员 {} 驳回收款记录 {}: {}", rejectedBy, recordId, reason)

    return dailyCollectionMutation.save(record)
  }

  /**
   * 获取配送员的收款记录
   */
  fun getCollectionByWorkerAndDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
  ): DailyCollectionRecordModel? =
    dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(
      deliveryWorkerId,
      collectionDate,
    )

  /**
   * 获取指定日期的所有收款记录（管理员用）
   */
  fun getCollectionsByDate(
    adminId: Long,
    collectionDate: LocalDate,
  ): List<DailyCollectionRecordModel> = dailyCollectionQuery.findByCollectionDate(collectionDate).ifEmpty { emptyList() }

  /**
   * 获取指定日期范围的所有收款记录
   */
  fun getCollectionsByDateRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyCollectionRecordModel> = dailyCollectionQuery.findByCollectionDateBetween(startDate, endDate)

  /**
   * 获取对账记录
   */
  fun getReconciliationByDate(reconciliationDate: LocalDate): DailyReconciliationModel? =
    dailyReconciliationQuery.findByReconciliationDate(reconciliationDate)

  /**
   * 定时任务：执行每日对账
   * 对账指定日期的收款和订单数据
   * 如果记录已存在，则更新（而不是删除后重建，避免唯一约束冲突）
   *
   * 差异口径(R-COL-02/R-COL-04):填报(现金+扫码)合计 vs 系统计算(现金+扫码)合计,
   * 水票按张数精确比对;任一超容差/不符 → 「有差异」。水票订单金额(应收但不由
   * 送水工收钱)不计入差异基数,避免使用水票的日期产生系统性「负差异」。
   * 重新执行对账会重算结果并清除复核痕迹(状态与复核字段保持一致)。
   */
  @Transactional
  fun executeDailyReconciliation(reconciliationDate: LocalDate = LocalDate.now().minusDays(1)) {
    logger.info("开始执行每日对账: {}", reconciliationDate)

    // 获取当日所有收款记录
    val collections = dailyCollectionQuery.findByCollectionDate(reconciliationDate)

    // 汇总数据
    var totalCashAmountCents = 0L
    var totalWaterTicketCount = 0
    var totalQrCodeAmountCents = 0L
    var totalOrderCount = 0
    var totalOrderAmountCents = 0L
    var totalCollectionAmountCents = 0L
    var totalCalculatedCashAmountCents = 0L
    var totalCalculatedQrCodeAmountCents = 0L
    var totalCalculatedWaterTicketCount = 0

    val workerReports = mutableListOf<Map<String, Any>>()

    for (collection in collections) {
      totalCashAmountCents += collection.cashAmountCents
      totalWaterTicketCount += collection.waterTicketCount
      totalQrCodeAmountCents += collection.qrCodeAmountCents
      totalOrderCount += collection.orderCount
      totalOrderAmountCents += collection.orderAmountCents
      totalCollectionAmountCents += collection.cashAmountCents + collection.qrCodeAmountCents
      totalCalculatedCashAmountCents += collection.calculatedCashAmountCents
      totalCalculatedQrCodeAmountCents += collection.calculatedQrCodeAmountCents
      totalCalculatedWaterTicketCount += collection.calculatedWaterTicketCount

      workerReports.add(
        mapOf(
          "deliveryWorkerId" to collection.deliveryWorkerId,
          "cashAmountCents" to collection.cashAmountCents,
          "waterTicketCount" to collection.waterTicketCount,
          "qrCodeAmountCents" to collection.qrCodeAmountCents,
          "orderCount" to collection.orderCount,
          "orderAmountCents" to collection.orderAmountCents,
          "differenceAmountCents" to collection.differenceAmountCents,
          "calculatedCashAmountCents" to collection.calculatedCashAmountCents,
          "calculatedQrCodeAmountCents" to collection.calculatedQrCodeAmountCents,
          "calculatedWaterTicketCount" to collection.calculatedWaterTicketCount,
          "status" to collection.status.name,
        ),
      )
    }

    // 计算差异:填报(现金+扫码) vs 系统计算(现金+扫码)
    val calculatedCollectionAmountCents = totalCalculatedCashAmountCents + totalCalculatedQrCodeAmountCents
    val discrepancyAmountCents = totalCollectionAmountCents - calculatedCollectionAmountCents
    val waterTicketMismatch = totalWaterTicketCount != totalCalculatedWaterTicketCount

    // 判断状态:金额超容差或水票数量不符 → 「有差异」
    val status =
      when {
        kotlin.math.abs(discrepancyAmountCents) <= TOLERANCE_AMOUNT_CENTS && !waterTicketMismatch -> {
          DailyReconciliationStatus.MATCHED
        }

        else -> {
          DailyReconciliationStatus.DISCREPANCY
        }
      }

    val reportData =
      mapOf(
        "workerReports" to workerReports,
        "totalWorkers" to collections.size,
        "totalCalculatedCashAmountCents" to totalCalculatedCashAmountCents,
        "totalCalculatedQrCodeAmountCents" to totalCalculatedQrCodeAmountCents,
        "totalCalculatedWaterTicketCount" to totalCalculatedWaterTicketCount,
      )

    // 检查是否已存在对账记录，存在则更新，不存在则创建
    val existingReconciliation = dailyReconciliationQuery.findByReconciliationDate(reconciliationDate)
    val reconciliation =
      existingReconciliation?.apply {
        this.totalCashAmountCents = totalCashAmountCents
        this.totalWaterTicketCount = totalWaterTicketCount
        this.totalQrCodeAmountCents = totalQrCodeAmountCents
        this.totalOrderCount = totalOrderCount
        this.totalOrderAmountCents = totalOrderAmountCents
        this.totalCollectionAmountCents = totalCollectionAmountCents
        this.discrepancyAmountCents = discrepancyAmountCents
        this.status = status
        this.reportData = reportData
        // 重新执行对账会重算结果,清除复核痕迹,避免「状态未复核但带复核字段」的不一致
        this.reviewedAt = null
        this.reviewedBy = null
        this.reviewNotes = null
        this.updatedAt = LocalDateTime.now()
      }
        ?: DailyReconciliationModel(
          reconciliationDate = reconciliationDate,
          totalCashAmountCents = totalCashAmountCents,
          totalWaterTicketCount = totalWaterTicketCount,
          totalQrCodeAmountCents = totalQrCodeAmountCents,
          totalOrderCount = totalOrderCount,
          totalOrderAmountCents = totalOrderAmountCents,
          totalCollectionAmountCents = totalCollectionAmountCents,
          discrepancyAmountCents = discrepancyAmountCents,
          status = status,
          reportData = reportData,
        )

    dailyReconciliationMutation.save(reconciliation)

    logger.info(
      "每日对账完成: date={}, totalOrderAmount={}, totalCollection={}, discrepancy={}, status={}",
      reconciliationDate,
      totalOrderAmountCents,
      totalCollectionAmountCents,
      discrepancyAmountCents,
      status,
    )
  }

  /**
   * 分页查询收款记录(后台收银对账列表,CLAUDE.md 规则 13):
   * 支持按收款日期/送水工筛选,恒按收款日期倒序 + 送水工升序。
   */
  fun getCollectionsPage(
    collectionDate: LocalDate?,
    deliveryWorkerId: Long?,
    page: Int,
    size: Int,
  ): Page<DailyCollectionRecordModel> {
    val pageable =
      PageRequest.of(
        page,
        size,
        Sort.by(Sort.Direction.DESC, "collectionDate").and(Sort.by(Sort.Direction.ASC, "deliveryWorkerId")),
      )
    return when {
      collectionDate != null && deliveryWorkerId != null ->
        dailyCollectionQuery.findByDeliveryWorkerIdAndCollectionDate(deliveryWorkerId, collectionDate, pageable)
      collectionDate != null -> dailyCollectionQuery.findByCollectionDate(collectionDate, pageable)
      deliveryWorkerId != null -> dailyCollectionQuery.findByDeliveryWorkerId(deliveryWorkerId, pageable)
      else -> dailyCollectionQuery.findAll(pageable)
    }
  }

  /**
   * 分页查询对账记录(后台/送水工端,CLAUDE.md 规则 13):恒按对账日期倒序。
   */
  fun getReconciliationsPage(
    page: Int,
    size: Int,
  ): Page<DailyReconciliationModel> =
    dailyReconciliationQuery.findAll(
      PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "reconciliationDate")),
    )

  /**
   * 指定日期收银汇总(后台顶部汇总卡):按该日全部日报记录聚合。
   */
  fun getDailyCollectionSummary(collectionDate: LocalDate): DailyCollectionSummaryDTO {
    val records = dailyCollectionQuery.findByCollectionDate(collectionDate)
    var totalCashAmountCents = 0L
    var totalQrCodeAmountCents = 0L
    var totalWaterTicketCount = 0
    var totalOrderCount = 0
    var totalOrderAmountCents = 0L
    var totalSubmittedCount = 0
    for (record in records) {
      totalCashAmountCents += record.cashAmountCents
      totalQrCodeAmountCents += record.qrCodeAmountCents
      totalWaterTicketCount += record.waterTicketCount
      totalOrderCount += record.orderCount
      totalOrderAmountCents += record.orderAmountCents
      if (record.status != DailyCollectionStatus.DRAFT) {
        totalSubmittedCount++
      }
    }
    return DailyCollectionSummaryDTO(
      collectionDate = collectionDate,
      totalCashAmountCents = totalCashAmountCents,
      totalQrCodeAmountCents = totalQrCodeAmountCents,
      totalWaterTicketCount = totalWaterTicketCount,
      totalOrderCount = totalOrderCount,
      totalOrderAmountCents = totalOrderAmountCents,
      totalSubmittedCount = totalSubmittedCount,
    )
  }

  /**
   * 分页查询对账日期窗口内「已送达」订单明细(G29 逐单差异明细):
   * 订单流水按送达时间口径(含待确认收货 + 已完成),与日报系统计算值同源。
   */
  fun getReconciliationOrderDetails(
    reconciliationDate: LocalDate,
    deliveryWorkerId: Long?,
    page: Int,
    size: Int,
  ): Page<OrderModel> {
    val startOfDay = reconciliationDate.atStartOfDay()
    val endOfDay = reconciliationDate.atTime(LocalTime.MAX)
    return orderQueryService.findDeliveredOrdersByDateRange(
      deliveryWorkerId = deliveryWorkerId,
      startDate = startOfDay,
      endDate = endOfDay,
      pageable = PageRequest.of(page, size),
    )
  }

  /**
   * 管理员复核对账记录
   */
  @Transactional
  fun reviewReconciliation(
    reconciliationId: Long,
    reviewedBy: Long,
    reviewNotes: String,
  ): DailyReconciliationModel {
    val reconciliation =
      dailyReconciliationQuery.findById(reconciliationId)
        ?: throw IllegalArgumentException("对账记录不存在: $reconciliationId")

    reconciliation.status = DailyReconciliationStatus.REVIEWED
    reconciliation.reviewedAt = LocalDateTime.now()
    reconciliation.reviewedBy = reviewedBy
    reconciliation.reviewNotes = reviewNotes
    reconciliation.updatedAt = LocalDateTime.now()

    logger.info("管理员 {} 复核对账记录 {}", reviewedBy, reconciliationId)

    return dailyReconciliationMutation.save(reconciliation)
  }
}
