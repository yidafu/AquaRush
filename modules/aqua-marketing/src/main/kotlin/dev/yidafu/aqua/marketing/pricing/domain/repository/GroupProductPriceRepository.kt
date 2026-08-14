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
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

/**
 * 分组商品专属价仓库(F-MKT-002);无行=回退目录价;@SoftDelete 自动过滤软删行。
 */
@Repository
interface GroupProductPriceRepository :
  JpaRepository<GroupProductPriceModel, Long>,
  JpaSpecificationExecutor<GroupProductPriceModel> {
  fun findByCustomerGroupIdAndProductId(
    groupId: Long,
    productId: Long,
  ): GroupProductPriceModel?

  fun findByCustomerGroupId(
    groupId: Long,
    pageable: Pageable,
  ): Page<GroupProductPriceModel>

  fun findByCustomerGroupId(groupId: Long): List<GroupProductPriceModel>
}
