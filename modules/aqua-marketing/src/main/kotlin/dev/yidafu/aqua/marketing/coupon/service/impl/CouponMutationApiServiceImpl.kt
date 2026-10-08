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

import dev.yidafu.aqua.api.dto.ClaimCouponRequest
import dev.yidafu.aqua.api.service.marketing.CouponMutationApiService
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponLimitExceededException
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponNotClaimableException
import dev.yidafu.aqua.marketing.coupon.domain.exception.CouponTemplateNotFoundException
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 优惠券用户侧变更服务实现(用户端领券/取消退券):
 * 领取校验(ACTIVE+自动领取+限领数+有效期快照);退回幂等(未过期可复用)。
 */
@Service
class CouponMutationApiServiceImpl(
  private val couponTemplateRepository: CouponTemplateRepository,
  private val userCouponRepository: UserCouponRepository,
) : CouponMutationApiService {
  @Transactional
  override fun claimCoupon(
    userId: Long,
    request: ClaimCouponRequest,
  ): UserCouponModel {
    val template =
      couponTemplateRepository.findById(request.couponTemplateId).orElseThrow {
        CouponTemplateNotFoundException(request.couponTemplateId)
      }
    if (template.status != CouponTemplateStatus.ACTIVE) {
      throw CouponNotClaimableException("券已停用,不可领取")
    }
    if (template.issuanceType != CouponIssuanceType.AUTO_CLAIM) {
      throw CouponNotClaimableException("该券为定向发放,不可领取")
    }
    val claimed = userCouponRepository.countByUserIdAndCouponTemplateId(userId, template.id!!)
    if (claimed >= template.perUserLimit) {
      throw CouponLimitExceededException("已达领取上限(每人限领${template.perUserLimit}张)")
    }
    val now = LocalDateTime.now()
    // 有效期快照校验:固定区间已结束或领取后N天为0 → 拒领
    val expireTime = CouponSnapshotSupport.computeExpireTime(template, now)
    if (!expireTime.isAfter(now)) {
      throw CouponNotClaimableException("券已过期,不可领取")
    }
    return userCouponRepository.save(CouponSnapshotSupport.buildUserCoupon(userId, template, now))
  }

  @Transactional
  override fun returnCouponsByOrder(orderId: Long): Int {
    val now = LocalDateTime.now()
    return userCouponRepository.returnByOrder(orderId, now)
  }
}
