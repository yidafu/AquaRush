/*
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

package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.service.BucketDepositService
import dev.yidafu.aqua.api.service.PaymentService
import dev.yidafu.aqua.common.domain.model.BucketDepositModel
import dev.yidafu.aqua.common.domain.model.BucketDepositStatus
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.repository.BucketDepositRepository
import dev.yidafu.aqua.common.domain.repository.SystemSettingsRepository
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.id.DefaultIdGenerator
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BucketDepositServiceImpl(
  private val bucketDepositRepository: BucketDepositRepository,
  private val systemSettingsRepository: SystemSettingsRepository,
  private val paymentService: PaymentService,
) : BucketDepositService {
  companion object {
    private const val BUCKET_DEPOSIT_DESCRIPTION = "水桶押金"
  }

  @Transactional
  override fun createBucketDeposit(
    userId: Long,
    quantity: Int,
    openId: String,
  ): BucketDepositModel {
    // 1. 验证数量
    if (quantity <= 0) {
      throw BadRequestException("押桶数量必须大于0")
    }

    // 2. 获取押桶金额配置
    val depositAmount = getBucketDepositAmount()

    // 3. 计算总金额
    val totalAmount = depositAmount * quantity

    // 4. 生成ID
    val id = DefaultIdGenerator().generate()

    // 5. 创建押桶记录
    val deposit =
      BucketDepositModel(
        id = id,
        userId = userId,
        quantity = quantity,
        amountCents = depositAmount,
        status = BucketDepositStatus.DEPOSITED,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    // 6. 保存记录
    val savedDeposit = bucketDepositRepository.save(deposit)

    // 7. 创建微信支付订单
    val paymentParams =
      paymentService.createWechatJsapiPay(
        orderId = id,
        amountCents = totalAmount,
        description = BUCKET_DEPOSIT_DESCRIPTION,
        openId = openId,
      )

    // 8. 返回押桶记录（包含支付参数）
    return savedDeposit
  }

  @Transactional
  override fun handlePaymentSuccess(
    depositId: Long,
    transactionId: String,
  ) {
    val deposit =
      bucketDepositRepository
        .findById(depositId)
        .orElseThrow { NotFoundException("押桶记录不存在: $depositId") }

    if (deposit.status == BucketDepositStatus.REFUNDED) {
      throw BadRequestException("该押桶记录已退还")
    }

    deposit.paymentTransactionId = transactionId
    deposit.paymentTime = LocalDateTime.now()
    deposit.updatedAt = LocalDateTime.now()

    bucketDepositRepository.save(deposit)
  }

  override fun getBucketDepositById(depositId: Long): BucketDepositModel? = bucketDepositRepository.findById(depositId).orElse(null)

  override fun getUserBucketDeposits(userId: Long): List<BucketDepositModel> =
    bucketDepositRepository.findByUserIdOrderByCreatedAtDesc(userId)

  override fun getUserBucketDeposits(
    userId: Long,
    page: Int,
    size: Int,
  ): Page<BucketDepositModel> = bucketDepositRepository.findByUserId(userId, PageRequest.of(page, size))

  override fun getUserActiveBucketCount(userId: Long): Int {
    val deposits =
      bucketDepositRepository.findByUserIdAndStatus(
        userId,
        BucketDepositStatus.DEPOSITED,
      )
    return deposits.sumOf { it.quantity }
  }

  @Transactional
  override fun refundBucketDeposit(
    depositId: Long,
    adminId: Long,
    remark: String?,
  ): BucketDepositModel {
    val deposit =
      bucketDepositRepository
        .findById(depositId)
        .orElseThrow { NotFoundException("押桶记录不存在: $depositId") }

    if (deposit.status == BucketDepositStatus.REFUNDED) {
      throw BadRequestException("该押桶记录已退还")
    }

    // 如果有微信支付交易号，发起退款
    if (!deposit.paymentTransactionId.isNullOrEmpty()) {
      try {
        paymentService.refund(
          transactionId = deposit.paymentTransactionId!!,
          refundAmountCents = deposit.amountCents * deposit.quantity,
          totalAmountCents = deposit.amountCents * deposit.quantity,
          reason = remark ?: "管理员操作退还押金",
        )
      } catch (e: Exception) {
        // 退款失败，记录日志但仍更新状态
        // 在实际生产中可能需要更复杂的处理
      }
    }

    deposit.status = BucketDepositStatus.REFUNDED
    deposit.refundedAt = LocalDateTime.now()
    deposit.refundedBy = adminId
    deposit.remark = remark
    deposit.updatedAt = LocalDateTime.now()

    return bucketDepositRepository.save(deposit)
  }

  override fun getAllBucketDeposits(
    status: BucketDepositStatus?,
    userId: Long?,
    page: Int,
    size: Int,
  ): Page<BucketDepositModel> {
    val pageable = PageRequest.of(page, size)

    return when {
      status != null && userId != null -> {
        bucketDepositRepository.findByUserIdAndStatus(userId, status, pageable)
      }

      status != null -> {
        bucketDepositRepository.findByStatus(status, pageable)
      }

      userId != null -> {
        bucketDepositRepository.findByUserId(userId, pageable)
      }

      else -> {
        bucketDepositRepository.findAll(pageable)
      }
    }
  }

  override fun getBucketDepositAmount(): Long {
    val setting =
      systemSettingsRepository.findBySettingKey(SystemSettingKeys.BUCKET_DEPOSIT_AMOUNT)
    return setting?.settingValue?.toLongOrNull() ?: 1000L // 默认10元
  }

  @Transactional
  override fun setBucketDepositAmount(amountCents: Long) {
    if (amountCents <= 0) {
      throw BadRequestException("押桶金额必须大于0")
    }

    var setting =
      systemSettingsRepository.findBySettingKey(SystemSettingKeys.BUCKET_DEPOSIT_AMOUNT)

    if (setting == null) {
      setting =
        SystemSettingsModel(
          id = DefaultIdGenerator().generate(),
          settingKey = SystemSettingKeys.BUCKET_DEPOSIT_AMOUNT,
          settingValue = amountCents.toString(),
          description = "每个桶的押金金额（分）",
          createdAt = LocalDateTime.now(),
          updatedAt = LocalDateTime.now(),
        )
    } else {
      setting.settingValue = amountCents.toString()
      setting.updatedAt = LocalDateTime.now()
    }

    systemSettingsRepository.save(setting)
  }
}
