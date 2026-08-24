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

package dev.yidafu.aqua.marketing.flashsale.domain.repository

import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import java.time.LocalDateTime

/**
 * 限时特价活动仓库 QueryDSL 自定义查询(FlashSaleActivityRepository 中手写 @Query 迁移至此)。
 */
interface FlashSaleActivityRepositoryCustom {
  /**
   * 某商品当前生效的特价(ACTIVE 且时间窗内;到期自动失效,供营销引擎单价链③取候选)
   */
  fun findActiveByProductId(
    productId: Long,
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): FlashSaleActivityModel?

  /**
   * 同商品时间窗重叠校验(仅限启用活动;已结束的历史活动不冲突)
   */
  fun existsOverlap(
    productId: Long,
    status: PromotionActivityStatus,
    newValidFrom: LocalDateTime,
    newValidTo: LocalDateTime,
    excludeId: Long?,
  ): Boolean
}
