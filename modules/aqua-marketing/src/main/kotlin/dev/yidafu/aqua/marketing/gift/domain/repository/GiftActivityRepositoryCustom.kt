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
import java.time.LocalDateTime

/**
 * 买赠/满赠活动仓库 QueryDSL 自定义查询(GiftActivityRepository 中手写 @Query 迁移至此)。
 */
interface GiftActivityRepositoryCustom {
  /**
   * 当前生效活动(ACTIVE 且时间窗内;到期自动失效,供营销引擎取候选后内存过滤 scope)
   */
  fun findActiveActivities(
    status: PromotionActivityStatus,
    now: LocalDateTime,
  ): List<GiftActivityModel>
}
