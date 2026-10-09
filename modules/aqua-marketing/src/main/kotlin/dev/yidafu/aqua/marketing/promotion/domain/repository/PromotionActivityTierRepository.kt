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

package dev.yidafu.aqua.marketing.promotion.domain.repository

import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel

/**
 * 满减/折扣活动档位仓库(档位递增且互斥)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删。
 */
interface PromotionActivityTierRepository {
  /** 按档位 sort 升序(引擎取档位口径);id 破平。 */
  fun findByActivityIdOrderBySortAsc(activityId: Long): List<PromotionActivityTierModel>

  fun findByActivityId(activityId: Long): List<PromotionActivityTierModel>

  /** 语义对齐 @SoftDelete 下的 delete:软删。 */
  fun delete(entity: PromotionActivityTierModel)

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: PromotionActivityTierModel): PromotionActivityTierModel
}
