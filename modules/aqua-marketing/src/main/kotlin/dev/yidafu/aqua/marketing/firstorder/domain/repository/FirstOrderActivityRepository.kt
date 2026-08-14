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

package dev.yidafu.aqua.marketing.firstorder.domain.repository

import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * 首单立减/新人专享价活动仓库(F-MKT-009)
 */
@Repository
interface FirstOrderActivityRepository :
  JpaRepository<FirstOrderActivityModel, Long>,
  JpaSpecificationExecutor<FirstOrderActivityModel> {
  /**
   * 当前生效活动(ACTIVE 且时间窗内;到期自动失效,供营销引擎取候选)
   */
  @Query(
    """
    select a from FirstOrderActivityModel a
    where a.status = :status and a.validFrom <= :now and a.validTo >= :now
    """,
  )
  fun findActiveActivities(
    @Param("status") status: PromotionActivityStatus,
    @Param("now") now: LocalDateTime,
  ): List<FirstOrderActivityModel>

  /**
   * 某商品当前生效的新人专享价(单价链④;仅当存在配置才触发新用户判定,避免列表页每商品查订单)
   */
  @Query(
    """
    select a from FirstOrderActivityModel a
    where a.status = :status and a.validFrom <= :now and a.validTo >= :now
      and a.activityType = :activityType and a.productId = :productId
    order by a.validFrom desc
    """,
  )
  fun findActiveNewcomerPriceByProductId(
    @Param("productId") productId: Long,
    @Param("activityType") activityType: FirstOrderActivityType,
    @Param("status") status: PromotionActivityStatus,
    @Param("now") now: LocalDateTime,
  ): FirstOrderActivityModel?

  /**
   * 同商品新人专享价重叠校验(仅限启用活动)
   */
  @Query(
    """
    select count(a) > 0 from FirstOrderActivityModel a
    where a.productId = :productId and a.status = :status and a.activityType = :activityType
      and a.validFrom < :newValidTo and a.validTo > :newValidFrom
      and (:excludeId is null or a.id <> :excludeId)
    """,
  )
  fun existsOverlap(
    @Param("productId") productId: Long,
    @Param("activityType") activityType: FirstOrderActivityType,
    @Param("status") status: PromotionActivityStatus,
    @Param("newValidFrom") newValidFrom: LocalDateTime,
    @Param("newValidTo") newValidTo: LocalDateTime,
    @Param("excludeId") excludeId: Long?,
  ): Boolean
}
