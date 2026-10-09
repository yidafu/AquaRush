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

package dev.yidafu.aqua.review.domain.repository

import dev.yidafu.aqua.common.domain.model.ReviewModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 订单评价仓储(JPA→Exposed 迁移 P4):剥离 JpaRepository,仅保留实际被调用的方法;
 * 查询过滤 is_deleted(对齐 @SoftDelete)。
 */
interface ReviewRepository : ReviewRepositoryCustom {
  fun findByOrderId(orderId: Long): ReviewModel?

  fun existsByOrderId(orderId: Long): Boolean

  /** 用户评价分页(用户详情页 Tab);固定 createdAt 降序,id 降序破平防跨页重复。 */
  fun findByUserIdOrderByCreatedAtDesc(
    userId: Long,
    pageable: Pageable,
  ): Page<ReviewModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: ReviewModel): ReviewModel
}
