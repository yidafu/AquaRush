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

import dev.yidafu.aqua.api.service.marketing.CouponQueryService
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.CouponTemplateRepository
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * 优惠券模板查询服务实现(管理后台,F-MKT-003)
 */
@Service
class CouponQueryServiceImpl(
  private val couponTemplateRepository: CouponTemplateRepository,
  private val userCouponRepository: UserCouponRepository,
) : CouponQueryService {
  override fun listTemplates(
    keyword: String?,
    status: CouponTemplateStatus?,
    pageable: Pageable,
  ): Page<CouponTemplateModel> {
    val spec =
      Specification<CouponTemplateModel> { root, _, cb ->
        val predicates = mutableListOf<jakarta.persistence.criteria.Predicate>()
        if (!keyword.isNullOrBlank()) {
          predicates.add(cb.like(cb.lower(root.get("name")), "%${keyword.lowercase()}%"))
        }
        if (status != null) {
          predicates.add(cb.equal(root.get<Any>("status"), status))
        }
        cb.and(*predicates.toTypedArray())
      }
    return couponTemplateRepository.findAll(spec, pageable)
  }

  override fun getTemplate(id: Long): CouponTemplateModel? = couponTemplateRepository.findById(id).orElse(null)

  /**
   * 已领未用数(RECEIVED + RETURNED 可复用)
   */
  override fun receivedCount(templateId: Long): Long =
    userCouponRepository.countByCouponTemplateIdAndStatus(templateId, UserCouponStatus.RECEIVED) +
      userCouponRepository.countByCouponTemplateIdAndStatus(templateId, UserCouponStatus.RETURNED)

  override fun usedCount(templateId: Long): Long = userCouponRepository.countByCouponTemplateIdAndStatus(templateId, UserCouponStatus.USED)
}
