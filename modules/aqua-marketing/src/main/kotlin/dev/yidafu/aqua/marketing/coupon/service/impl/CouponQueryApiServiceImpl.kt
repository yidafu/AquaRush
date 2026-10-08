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

package dev.yidafu.aqua.marketing.coupon.service.impl

import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 优惠券用户侧查询服务实现(领券中心/我的券包;促销引擎「最优优惠组合」评估数据源)
 */
@Service
class CouponQueryApiServiceImpl(
  private val couponTemplateRepository: CouponTemplateRepository,
  private val userCouponRepository: UserCouponRepository,
) : CouponQueryApiService {
  override fun findClaimableTemplates(userId: Long): List<CouponTemplateModel> =
    couponTemplateRepository
      .findByStatus(CouponTemplateStatus.ACTIVE)
      .filter { it.issuanceType == CouponIssuanceType.AUTO_CLAIM }

  override fun findUserCoupons(
    userId: Long,
    status: UserCouponStatus?,
  ): List<UserCouponModel> =
    if (status == null) {
      userCouponRepository.findByUserId(userId)
    } else {
      userCouponRepository.findByUserIdAndStatus(userId, status)
    }

  override fun findUsableCoupons(userId: Long): List<UserCouponModel> {
    val now = LocalDateTime.now()
    return userCouponRepository
      .findByUserIdAndStatusIn(userId, listOf(UserCouponStatus.RECEIVED, UserCouponStatus.RETURNED))
      .filter { it.expireTime.isAfter(now) }
  }

  override fun findUserCouponByIdAndUser(
    userId: Long,
    couponId: Long,
  ): UserCouponModel? =
    userCouponRepository
      .findById(couponId)
      .filter { it.userId == userId }
      .orElse(null)

  override fun countUserCouponsByTemplate(
    userId: Long,
    templateId: Long,
  ): Long = userCouponRepository.countByUserIdAndCouponTemplateId(userId, templateId)
}
