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

package dev.yidafu.aqua.marketing.gift.domain.repository

import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 买赠/满赠活动仓库(F-MKT-008)
 */
@Repository
interface GiftActivityRepository :
  JpaRepository<GiftActivityModel, Long>,
  JpaSpecificationExecutor<GiftActivityModel> {
  /**
   * 当前生效活动(ACTIVE 且时间窗内;到期自动失效,供营销引擎取候选后内存过滤 scope)
   */
  @Query(
    """
    select a from GiftActivityModel a
    where a.status = :status and a.validFrom <= :now and a.validTo >= :now
    """,
  )
  fun findActiveActivities(
    @Param("status") status: PromotionActivityStatus,
    @Param("now") now: LocalDateTime,
  ): List<GiftActivityModel>
}
