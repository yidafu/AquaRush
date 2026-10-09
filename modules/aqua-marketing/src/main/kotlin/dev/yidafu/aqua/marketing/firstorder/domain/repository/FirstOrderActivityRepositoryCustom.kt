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
import java.time.LocalDateTime

/**
 * 首单立减/新人专享价活动仓库 自定义查询(FirstOrderActivityRepository 中手写 @Query 迁移至此)。
 */
interface FirstOrderActivityRepositoryCustom {
  /**
   * 当前生效活动(ACTIVE 且时间窗内;到期自动失效,供营销引擎取候选)
   */
  fun findActiveActivities(
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): List<FirstOrderActivityModel>

  /**
   * 某商品当前生效的新人专享价(单价链④;仅当存在配置才触发新用户判定,避免列表页每商品查订单)
   */
  fun findActiveNewcomerPriceByProductId(
    productId: Long,
    activityType: FirstOrderActivityType,
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): FirstOrderActivityModel?

  /**
   * 同商品新人专享价重叠校验(仅限启用活动)
   */
  fun existsOverlap(
    productId: Long,
    activityType: FirstOrderActivityType,
    status: PromotionActivityStatus,
    newValidFrom: LocalDateTime,
    newValidTo: LocalDateTime,
    excludeId: Long?,
  ): Boolean
}
