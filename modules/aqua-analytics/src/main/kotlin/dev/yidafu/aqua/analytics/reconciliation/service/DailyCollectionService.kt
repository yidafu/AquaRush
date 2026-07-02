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

import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PaymentType
import dev.yidafu.aqua.delivery.domain.repository.DailyCollectionRecordRepository
import dev.yidafu.aqua.delivery.domain.repository.DailyReconciliationRepository
import dev.yidafu.aqua.analytics.reconciliation.dto.OrderStatsDTO
import dev.yidafu.aqua.analytics.reconciliation.dto.PaymentStatsDTO
import org.slf4j.LoggerFactory
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
  private val collectionRecordRepository: DailyCollectionRecordRepository,
  private val reconciliationRepository: DailyReconciliationRepository,
  private val orderQueryService: dev.yidafu.aqua.api.service.order.OrderQueryService,
  private val orderRepository: dev.yidafu.aqua.order.domain.repository.OrderRepository,
) {
  private val logger = LoggerFactory.getLogger(DailyCollectionService::class.java)

  companion object {
    // 容差金额（1元 = 100分）
    private const val TOLERANCE_AMOUNT_CENTS = 100L
  }

  /**
   * 获取配送员今日按支付类型的收款统计
   * 从已完成的订单中按支付类型统计
   */
  fun getTodayPaymentStats(
    deliveryWorkerId: Long,
    date: LocalDate = LocalDate.now(),
  ): PaymentStatsDTO {
    val startOfDay = date.atStartOfDay()
    val endOfDay = date.atTime(LocalTime.MAX)
    val completedStatus = OrderModelStatus.COMPLETED

    // 统计现金订单
    val cashResult = orderRepository.countAndSumByPaymentType(
      deliveryWorkerId,
      completedStatus,
      PaymentType.CASH,
      startOfDay,
      endOfDay,
    )
    val cashAmountCents = if (cashResult[0] != null && cashResult[0] > 0) cashResult[1] ?: 0L else 0L
    val cashOrderCount = (cashResult[0] ?: 0L).toInt()

    // 统计水票订单
    val waterTicketResult = orderRepository.countAndSumByPaymentType(
      deliveryWorkerId,
      completedStatus,
      PaymentType.WATER_TICKET,
      startOfDay,
      endOfDay,
    )
    val waterTicketCount = (waterTicketResult[0] ?: 0L).toInt()

    // 统计扫码订单
    val qrCodeResult = orderRepository.countAndSumByPaymentType(
      deliveryWorkerId,
      completedStatus,
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
   * 获取配送员今日订单统计（系统自动计算）
   */
  fun getTodayOrderStats(
    deliveryWorkerId: Long,
    date: LocalDate = LocalDate.now(),
  ): OrderStatsDTO {
    val startOfDay = date.atStartOfDay()
    val endOfDay = date.atTime(LocalTime.MAX)
    val completedStatuses = listOf(OrderModelStatus.COMPLETED)

    val orderCount =
      orderQueryService
        .countOrdersByDateRange(
          startOfDay,
          endOfDay,
          deliveryWorkerId,
          completedStatuses,
        ).toInt()

    val orderAmountCents =
      orderQueryService.sumAmountCentsByStatusAndDateRange(
        completedStatuses,
        startOfDay,
        endOfDay,
        deliveryWorkerId,
      )

    return OrderStatsDTO(orderCount, orderAmountCents)
  }

  /**
   * 配送员录入今日收款
   */
  @Transactional
  fun submitDailyCollection(
    deliveryWorkerId: Long,
    cashAmountCents: Long,
    waterTicketCount: Int,
    qrCodeAmountCents: Long,
    collectionDate: LocalDate = LocalDate.now(),
  ): DailyCollectionRecordModel {
    logger.info(
      "配送员 {} 提交每日收款: cash={}, waterTicket={}, qrCode={}, date={}",
      deliveryWorkerId,
      cashAmountCents,
      waterTicketCount,
      qrCodeAmountCents,
      collectionDate,
    )

    // 查询当日完成的订单数量和金额
    val startOfDay = collectionDate.atStartOfDay()
    val endOfDay = collectionDate.atTime(LocalTime.MAX)

    // 获取按支付类型的收款统计
    val paymentStats = getTodayPaymentStats(deliveryWorkerId, collectionDate)

    // 统计已完成订单数量和金额
    val completedStatuses = listOf(OrderModelStatus.COMPLETED)
    val orderCount =
      orderQueryService
        .countOrdersByDateRange(
          startOfDay,
          endOfDay,
          deliveryWorkerId,
          completedStatuses,
        ).toInt()

    val orderAmountCents =
      orderQueryService.sumAmountCentsByStatusAndDateRange(
        completedStatuses,
        startOfDay,
        endOfDay,
        deliveryWorkerId,
      )

    // 计算实际收款金额（现金 + 扫码）
    val totalCollectionAmount = cashAmountCents + qrCodeAmountCents

    // 计算差异金额
    val differenceAmountCents = totalCollectionAmount - orderAmountCents

    // 判断状态
    val status =
      when {
        kotlin.math.abs(differenceAmountCents) <= TOLERANCE_AMOUNT_CENTS -> {
          DailyCollectionStatus.SUBMITTED // 容差范围内，标记为已提交
        }

        else -> {
          DailyCollectionStatus.DRAFT // 差异过大，保留草稿状态让配送员确认
        }
      }

    // 创建或更新记录
    val existingRecord =
      collectionRecordRepository.findByDeliveryWorkerIdAndCollectionDate(
        deliveryWorkerId,
        collectionDate,
      )

    val record =
      existingRecord?.apply {
        this.cashAmountCents = cashAmountCents
        this.waterTicketCount = waterTicketCount
        this.qrCodeAmountCents = qrCodeAmountCents
        this.orderCount = orderCount
        this.orderAmountCents = orderAmountCents
        this.differenceAmountCents = differenceAmountCents
        this.status = status
        this.updatedAt = LocalDateTime.now()
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
          // 填充计算字段
          calculatedCashAmountCents = paymentStats.cashAmountCents,
          calculatedQrCodeAmountCents = paymentStats.qrCodeAmountCents,
          calculatedWaterTicketCount = paymentStats.waterTicketCount,
          calculatedOrderAmountCents = orderAmountCents,
        )

    return collectionRecordRepository.save(record)
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
      collectionRecordRepository
        .findById(recordId)
        .orElseThrow { IllegalArgumentException("收款记录不存在: $recordId") }

    record.status = DailyCollectionStatus.CONFIRMED
    record.confirmedAt = LocalDateTime.now()
    record.confirmedBy = confirmedBy
    record.notes = notes
    record.updatedAt = LocalDateTime.now()

    logger.info("管理员 {} 确认收款记录 {}", confirmedBy, recordId)

    return collectionRecordRepository.save(record)
  }

  /**
   * 获取配送员的收款记录
   */
  fun getCollectionByWorkerAndDate(
    deliveryWorkerId: Long,
    collectionDate: LocalDate,
  ): DailyCollectionRecordModel? =
    collectionRecordRepository.findByDeliveryWorkerIdAndCollectionDate(
      deliveryWorkerId,
      collectionDate,
    )

  /**
   * 获取指定日期的所有收款记录（管理员用）
   */
  fun getCollectionsByDate(
    adminId: Long,
    collectionDate: LocalDate,
  ): List<DailyCollectionRecordModel> = collectionRecordRepository.findByCollectionDate(collectionDate).ifEmpty { emptyList() }

  /**
   * 获取所有收款记录（管理员用）
   */
  fun getAllCollections(): List<DailyCollectionRecordModel> = collectionRecordRepository.findAll().ifEmpty { emptyList() }

  /**
   * 获取指定日期范围的所有收款记录
   */
  fun getCollectionsByDateRange(
    startDate: LocalDate,
    endDate: LocalDate,
  ): List<DailyCollectionRecordModel> = collectionRecordRepository.findByCollectionDateBetween(startDate, endDate)

  /**
   * 获取对账记录
   */
  fun getReconciliationByDate(reconciliationDate: LocalDate): DailyReconciliationModel? =
    reconciliationRepository.findByReconciliationDate(reconciliationDate)

  /**
   * 获取所有对账记录
   */
  fun getAllReconciliations(): List<DailyReconciliationModel> = reconciliationRepository.findAll().ifEmpty { emptyList() }

  /**
   * 定时任务：执行每日对账
   * 对账指定日期的收款和订单数据
   * 如果记录已存在，则更新（而不是删除后重建，避免唯一约束冲突）
   */
  @Transactional
  fun executeDailyReconciliation(reconciliationDate: LocalDate = LocalDate.now().minusDays(1)) {
    logger.info("开始执行每日对账: {}", reconciliationDate)

    // 获取当日所有收款记录
    val collections = collectionRecordRepository.findByCollectionDate(reconciliationDate)

    // 汇总数据
    var totalCashAmountCents = 0L
    var totalWaterTicketCount = 0
    var totalQrCodeAmountCents = 0L
    var totalOrderCount = 0
    var totalOrderAmountCents = 0L
    var totalCollectionAmountCents = 0L

    val workerReports = mutableListOf<Map<String, Any>>()

    for (collection in collections) {
      totalCashAmountCents += collection.cashAmountCents
      totalWaterTicketCount += collection.waterTicketCount
      totalQrCodeAmountCents += collection.qrCodeAmountCents
      totalOrderCount += collection.orderCount
      totalOrderAmountCents += collection.orderAmountCents
      totalCollectionAmountCents += collection.cashAmountCents + collection.qrCodeAmountCents

      workerReports.add(
        mapOf(
          "deliveryWorkerId" to collection.deliveryWorkerId,
          "cashAmountCents" to collection.cashAmountCents,
          "waterTicketCount" to collection.waterTicketCount,
          "qrCodeAmountCents" to collection.qrCodeAmountCents,
          "orderCount" to collection.orderCount,
          "orderAmountCents" to collection.orderAmountCents,
          "differenceAmountCents" to collection.differenceAmountCents,
          "status" to collection.status.name,
        ),
      )
    }

    // 计算差异
    val discrepancyAmountCents = totalCollectionAmountCents - totalOrderAmountCents

    // 判断状态
    val status =
      when {
        kotlin.math.abs(discrepancyAmountCents) <= TOLERANCE_AMOUNT_CENTS -> {
          DailyReconciliationStatus.MATCHED
        }

        else -> {
          DailyReconciliationStatus.DISCREPANCY
        }
      }

    // 检查是否已存在对账记录，存在则更新，不存在则创建
    val existingReconciliation = reconciliationRepository.findByReconciliationDate(reconciliationDate)
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
        this.reportData =
          mapOf(
            "workerReports" to workerReports,
            "totalWorkers" to collections.size,
          )
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
          reportData =
            mapOf(
              "workerReports" to workerReports,
              "totalWorkers" to collections.size,
            ),
        )

    reconciliationRepository.save(reconciliation)

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
   * 管理员复核对账记录
   */
  @Transactional
  fun reviewReconciliation(
    reconciliationId: Long,
    reviewedBy: Long,
    reviewNotes: String,
  ): DailyReconciliationModel {
    val reconciliation =
      reconciliationRepository
        .findById(reconciliationId)
        .orElseThrow { IllegalArgumentException("对账记录不存在: $reconciliationId") }

    reconciliation.status = DailyReconciliationStatus.REVIEWED
    reconciliation.reviewedAt = LocalDateTime.now()
    reconciliation.reviewedBy = reviewedBy
    reconciliation.reviewNotes = reviewNotes
    reconciliation.updatedAt = LocalDateTime.now()

    logger.info("管理员 {} 复核对账记录 {}", reviewedBy, reconciliationId)

    return reconciliationRepository.save(reconciliation)
  }
}
