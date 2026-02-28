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

package dev.yidafu.aqua.api.service

import dev.yidafu.aqua.common.domain.model.BucketDepositModel
import dev.yidafu.aqua.common.domain.model.BucketDepositStatus
import org.springframework.data.domain.Page

/**
 * 押桶服务接口
 */
interface BucketDepositService {

  /**
   * 创建押桶记录
   * @param userId 用户ID
   * @param quantity 押桶数量
   * @param openId 微信OpenID（用于支付）
   * @return 押桶记录（支付前）
   */
  fun createBucketDeposit(
    userId: Long,
    quantity: Int,
    openId: String,
  ): BucketDepositModel

  /**
   * 处理押桶支付成功回调
   * @param depositId 押桶记录ID
   * @param transactionId 微信支付交易号
   */
  fun handlePaymentSuccess(depositId: Long, transactionId: String)

  /**
   * 获取押桶记录详情
   */
  fun getBucketDepositById(depositId: Long): BucketDepositModel?

  /**
   * 获取用户的押桶记录列表
   */
  fun getUserBucketDeposits(userId: Long): List<BucketDepositModel>

  /**
   * 获取用户的押桶记录列表（分页）
   */
  fun getUserBucketDeposits(userId: Long, page: Int, size: Int): Page<BucketDepositModel>

  /**
   * 获取用户当前有效的押桶数量
   */
  fun getUserActiveBucketCount(userId: Long): Int

  /**
   * 管理员退还押金
   * @param depositId 押桶记录ID
   * @param adminId 管理员ID
   * @param remark 备注
   */
  fun refundBucketDeposit(
    depositId: Long,
    adminId: Long,
    remark: String? = null,
  ): BucketDepositModel

  /**
   * 获取所有押桶记录（管理员）
   */
  fun getAllBucketDeposits(
    status: BucketDepositStatus? = null,
    userId: Long? = null,
    page: Int,
    size: Int,
  ): Page<BucketDepositModel>

  /**
   * 获取押桶金额配置
   */
  fun getBucketDepositAmount(): Long

  /**
   * 设置押桶金额配置
   */
  fun setBucketDepositAmount(amountCents: Long)
}
