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

package dev.yidafu.aqua.marketing.barrel.service.impl

import dev.yidafu.aqua.api.dto.SubmitDepositRefundRequest
import dev.yidafu.aqua.api.service.deposit.DepositRefundMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.common.domain.model.DepositRecordModel
import dev.yidafu.aqua.common.domain.model.DepositRefundApplicationModel
import dev.yidafu.aqua.common.domain.model.enums.DepositRefundApplicationStatus
import dev.yidafu.aqua.common.domain.model.enums.DepositStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRecordRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositRefundApplicationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 退押申请变更服务实现(F-PRO-010/R-PRO-04 状态机,设计03 §9 押金桶生命周期):
 * - 提交:桶数 ≤ 在租(HELD)押金桶数,应退 = Σ 覆盖记录 桶数×单桶押金快照(服务端计算 R-MON-01);
 * - 审核通过:微信退款 DEPOSIT_REFUND(独立账本不计退款统计)+ 覆盖记录 HELD→REFUNDED
 *   (状态谓词条件更新幂等,防并发重复退还);部分退还时切分押金记录(余量保 HELD,已退切片落 REFUNDED);
 *   现金/无交易号订单仅落账本(线下退款)。微信退款失败抛异常,申请保持 RECOVERED 可重试。
 */
@Service
class DepositRefundMutationServiceImpl(
  private val applicationRepository: DepositRefundApplicationRepository,
  private val depositRecordRepository: DepositRecordRepository,
  private val depositPriceRepository: DepositPriceRepository,
  private val orderQueryService: OrderQueryService,
  private val paymentService: PaymentService,
  private val bizLogger: BizLogger,
) : DepositRefundMutationService {
  private val logger = LoggerFactory.getLogger(DepositRefundMutationServiceImpl::class.java)

  @Transactional
  override fun submit(
    userId: Long,
    request: SubmitDepositRefundRequest,
  ): DepositRefundApplicationModel {
    if (request.specification.isBlank()) {
      throw BadRequestException("规格不能为空")
    }
    if (request.bucketCount <= 0) {
      throw BadRequestException("退还桶数必须大于 0")
    }
    applicationRepository.findActiveByUserAndSpec(userId, request.specification)?.let {
      throw BadRequestException("该规格已有处理中的退押申请,请勿重复提交")
    }
    val held = heldRecords(userId, request.specification)
    val plan = planCoverage(held, request.bucketCount, request.specification)
    val app =
      applicationRepository.save(
        DepositRefundApplicationModel(
          userId = userId,
          specification = request.specification,
          bucketCount = request.bucketCount,
          depositCents = plan.amountCents,
          receiverName = request.receiverName,
          receiverPhone = request.receiverPhone,
          receiverDetail = request.receiverDetail,
          remark = request.remark,
          status = DepositRefundApplicationStatus.PENDING,
        ),
      )
    logger.info(
      "提交退押申请: id={}, userId={}, spec={}, bucketCount={}, depositCents={}",
      app.id,
      userId,
      request.specification,
      request.bucketCount,
      plan.amountCents,
    )
    bizLogger.logOperation(
      operation = "DEPOSIT_REFUND_SUBMITTED",
      module = "PRODUCT",
      result = "SUCCESS",
      target = app.id?.toString() ?: "deposit-refund",
      userId = userId,
      additionalData =
        mapOf(
          "specification" to request.specification,
          "bucketCount" to request.bucketCount,
          "depositCents" to plan.amountCents,
        ),
    )
    return app
  }

  @Transactional
  override fun confirmRecovered(
    applicationId: Long,
    adminId: Long,
  ): DepositRefundApplicationModel {
    applicationRepository.findById(applicationId) ?: throw BadRequestException("退押申请不存在: $applicationId")
    val ok =
      applicationRepository.transition(
        id = applicationId,
        fromStatuses = listOf(DepositRefundApplicationStatus.PENDING),
        to = DepositRefundApplicationStatus.RECOVERED,
        handlerAdminId = adminId,
        rejectReason = null,
      )
    if (!ok) {
      throw BadRequestException("退押申请当前状态不可确认回收")
    }
    logger.info("空桶回收确认: applicationId={}, adminId={}", applicationId, adminId)
    bizLogger.logOperation(
      operation = "DEPOSIT_RECOVERY_CONFIRMED",
      module = "PRODUCT",
      result = "SUCCESS",
      target = applicationId.toString(),
      userId = adminId,
    )
    return applicationRepository.findById(applicationId)!!
  }

  @Transactional
  override fun approve(
    applicationId: Long,
    adminId: Long,
  ): DepositRefundApplicationModel {
    val app = applicationRepository.findById(applicationId) ?: throw BadRequestException("退押申请不存在: $applicationId")
    if (app.status != DepositRefundApplicationStatus.RECOVERED) {
      throw BadRequestException("退押申请状态为 ${app.status},不可审核退还(需先空桶回收确认)")
    }

    // 覆盖计划:按 HELD 记录顺序覆盖退还桶数,金额 = Σ 覆盖桶数 × 单桶押金快照
    val held = heldRecords(app.userId, app.specification)
    val plan = planCoverage(held, app.bucketCount, app.specification)
    if (plan.amountCents <= 0) {
      throw BadRequestException("可退押金为 0,请核对押金价目与押金记录")
    }

    // 微信退款(DEPOSIT_REFUND 独立账本,不计营收/退款统计;现金/历史无交易号仅线下退)
    val sourceOrder = orderQueryService.getOrderById(plan.coveringRecords.first().orderId)
    val transactionId = sourceOrder.paymentTransactionId
    if (!transactionId.isNullOrBlank()) {
      paymentService.refund(
        transactionId = transactionId,
        refundAmountCents = plan.amountCents,
        totalAmountCents = sourceOrder.payAmountCents,
        reason = "退押审核退还 - ${app.specification}×${app.bucketCount}桶",
        refundType = RefundType.DEPOSIT_REFUND,
      )
    } else {
      logger.info("押金退还无微信交易号,按线下退款落账本: applicationId={}, orderId={}", applicationId, sourceOrder.id)
    }

    // 账本联动:整额覆盖记录条件更新 HELD→REFUNDED;最后一条部分覆盖 → 切分
    // (原记录缩量保 HELD,切片新落 REFUNDED 行,保证 Σ REFUNDED 口径准确)
    val now = LocalDateTime.now()
    plan.fullCoveredIds.forEach { id ->
      depositRecordRepository.markRefundedIfHeld(listOf(id), adminId, "退押审核退还")
    }
    plan.partial?.let { (record, coveredBuckets) ->
      val unit = unitOf(record, app.specification)
      val coveredCents = unit * coveredBuckets
      // 原记录缩量保 HELD(用户仍持有余量桶的押金),金额按切片扣减保持总账守恒
      record.bucketCount = (record.bucketCount ?: 0) - coveredBuckets
      record.depositAmountCents -= coveredCents
      record.updatedAt = now
      depositRecordRepository.saveAll(listOf(record))
      // 已退切片独立落 REFUNDED 行(Σ REFUNDED 口径准确)
      depositRecordRepository.saveAll(
        listOf(
          DepositRecordModel(
            orderId = record.orderId,
            userId = record.userId,
            depositAmountCents = coveredCents,
            specification = record.specification,
            unitDepositCents = unit,
            bucketCount = coveredBuckets,
            status = DepositStatus.REFUNDED,
            refundedAt = now,
            refundedBy = adminId,
            refundRemark = "退押审核部分退还",
          ),
        ),
      )
    }

    val ok =
      applicationRepository.transition(
        id = applicationId,
        fromStatuses = listOf(DepositRefundApplicationStatus.RECOVERED),
        to = DepositRefundApplicationStatus.REFUNDED,
        handlerAdminId = adminId,
        rejectReason = null,
      )
    if (!ok) {
      // 资金已出且押金记录已翻转,申请流转失败属并发异常:留错误日志由人工核对(对齐 RefundReviewService 口径)
      logger.error("退押申请流转 REFUNDED 失败(请人工核对): applicationId={}", applicationId)
    }
    logger.info(
      "退押审核通过: applicationId={}, amountCents={}, transactionId={}",
      applicationId,
      plan.amountCents,
      transactionId ?: "CASH",
    )
    bizLogger.logOperation(
      operation = "DEPOSIT_REFUND_APPROVED",
      module = "PRODUCT",
      result = "SUCCESS",
      target = applicationId.toString(),
      userId = adminId,
      additionalData =
        mapOf(
          "depositCents" to plan.amountCents,
          "specification" to app.specification,
          "bucketCount" to app.bucketCount,
          "wechatRefunded" to (!transactionId.isNullOrBlank()),
        ),
    )
    return applicationRepository.findById(applicationId)!!
  }

  @Transactional
  override fun reject(
    applicationId: Long,
    adminId: Long,
    reason: String,
  ): DepositRefundApplicationModel {
    if (reason.isBlank()) {
      throw BadRequestException("驳回原因不能为空")
    }
    applicationRepository.findById(applicationId) ?: throw BadRequestException("退押申请不存在: $applicationId")
    val ok =
      applicationRepository.transition(
        id = applicationId,
        fromStatuses =
          listOf(DepositRefundApplicationStatus.PENDING, DepositRefundApplicationStatus.RECOVERED),
        to = DepositRefundApplicationStatus.REJECTED,
        handlerAdminId = adminId,
        rejectReason = reason,
      )
    if (!ok) {
      throw BadRequestException("退押申请当前状态不可驳回")
    }
    logger.info("退押驳回: applicationId={}, reason={}", applicationId, reason)
    bizLogger.logOperation(
      operation = "DEPOSIT_REFUND_REJECTED",
      module = "PRODUCT",
      result = "SUCCESS",
      target = applicationId.toString(),
      userId = adminId,
      additionalData = mapOf("reason" to reason),
    )
    return applicationRepository.findById(applicationId)!!
  }

  // ---------- 内部 ----------

  private fun heldRecords(
    userId: Long,
    specification: String,
  ): List<DepositRecordModel> =
    depositRecordRepository
      .findByUserIdAndSpecification(userId, specification)
      .filter { it.status == DepositStatus.HELD }

  /** 单桶押金快照(历史行可空回退启用价目) */
  private fun unitOf(
    record: DepositRecordModel,
    specification: String,
  ): Long {
    record.unitDepositCents?.let { return it }
    return depositPriceRepository
      .findBySpecification(specification)
      ?.takeIf { it.status == dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus.ENABLED }
      ?.priceCents
      ?: 0L
  }

  /**
   * 覆盖计划:按记录顺序覆盖 bucketCount 桶——
   * fullCoveredIds = 整额覆盖(整行 HELD→REFUNDED);partial = 最后一条部分覆盖(切分);
   * amountCents = Σ 覆盖桶数 × 单桶押金快照。
   */
  private data class CoveragePlan(
    val coveringRecords: List<DepositRecordModel>,
    val fullCoveredIds: List<Long>,
    val partial: Pair<DepositRecordModel, Int>?,
    val amountCents: Long,
  )

  private fun planCoverage(
    held: List<DepositRecordModel>,
    bucketCount: Int,
    specification: String,
  ): CoveragePlan {
    var remaining = bucketCount
    var amount = 0L
    val covering = mutableListOf<DepositRecordModel>()
    val full = mutableListOf<Long>()
    var partial: Pair<DepositRecordModel, Int>? = null
    for (record in held) {
      if (remaining <= 0) break
      val available = record.bucketCount ?: 0
      if (available <= 0) continue
      val covered = minOf(remaining, available)
      val unit = unitOf(record, specification)
      if (unit <= 0) {
        throw BadRequestException("该规格押金价目未配置或为 0,无法退押")
      }
      amount += unit * covered
      covering += record
      if (covered == available) {
        record.id?.let { full += it }
      } else {
        partial = record to covered
      }
      remaining -= covered
    }
    if (remaining > 0) {
      throw BadRequestException("退还桶数不能超过在租押金桶数(在租 ${bucketCount - remaining} 桶)")
    }
    return CoveragePlan(covering, full, partial, amount)
  }
}
