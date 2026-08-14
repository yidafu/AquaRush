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

package dev.yidafu.aqua.marketing.promotion.service.impl

import dev.yidafu.aqua.api.service.marketing.PromotionActivityQueryService
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 满减/折扣活动查询服务实现(管理后台,F-MKT-004)
 */
@Service
class PromotionActivityQueryServiceImpl(
  private val promotionActivityRepository: PromotionActivityRepository,
  private val promotionActivityTierRepository: PromotionActivityTierRepository,
) : PromotionActivityQueryService {
  override fun listActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<PromotionActivityModel> {
    val spec =
      Specification<PromotionActivityModel> { root, _, cb ->
        if (keyword.isNullOrBlank()) {
          null
        } else {
          cb.like(cb.lower(root.get("name")), "%${keyword.lowercase()}%")
        }
      }
    return promotionActivityRepository.findAll(spec, pageable)
  }

  override fun getActivity(id: Long): PromotionActivityModel? = promotionActivityRepository.findById(id).orElse(null)

  override fun listTiers(activityId: Long): List<PromotionActivityTierModel> =
    promotionActivityTierRepository.findByActivityIdOrderBySortAsc(activityId)

  override fun findActiveActivities(now: LocalDateTime): List<PromotionActivityModel> =
    promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now)
}
