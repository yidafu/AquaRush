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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

/**
 * 首单立减/新人专享价活动仓库(F-MKT-009)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删;含当前生效活动/重叠校验查询。
 */
interface FirstOrderActivityRepository {
  fun findAllById(ids: List<Long>): List<FirstOrderActivityModel>

  fun findById(id: Long): Optional<FirstOrderActivityModel>

  /** 后台活动分页:name 关键词(大小写不敏感);固定 id 升序破平。 */
  fun searchActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<FirstOrderActivityModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: FirstOrderActivityModel): FirstOrderActivityModel

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
