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

package dev.yidafu.aqua.marketing.pricing.domain.repository

import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 分组商品专属价仓库(F-MKT-002):无行=回退目录价。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删。
 */
interface GroupProductPriceRepository {
  fun findByCustomerGroupIdAndProductId(
    groupId: Long,
    productId: Long,
  ): GroupProductPriceModel?

  fun findByCustomerGroupId(groupId: Long): List<GroupProductPriceModel>

  /** 后台分组价分页;固定 id 升序破平。 */
  fun findByCustomerGroupId(
    groupId: Long,
    pageable: Pageable,
  ): Page<GroupProductPriceModel>

  /** 语义对齐 @SoftDelete 下的 delete:软删。 */
  fun delete(entity: GroupProductPriceModel)

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: GroupProductPriceModel): GroupProductPriceModel
}
