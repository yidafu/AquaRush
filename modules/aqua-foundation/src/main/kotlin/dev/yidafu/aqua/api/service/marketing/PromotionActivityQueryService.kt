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

package dev.yidafu.aqua.api.service.marketing

import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 满减/折扣活动查询服务(管理后台,PRD 10-marketing.md §F-MKT-004)
 */
interface PromotionActivityQueryService {
  fun listActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<PromotionActivityModel>

  fun getActivity(id: Long): PromotionActivityModel?

  /**
   * 活动档位(按 sort 升序)
   */
  fun listTiers(activityId: Long): List<PromotionActivityTierModel>

  /**
   * 当前进行中的活动(ACTIVE 且 validFrom ≤ now ≤ validTo,供展示与引擎)
   */
  fun findActiveActivities(now: java.time.LocalDateTime): List<PromotionActivityModel>
}
