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

package dev.yidafu.aqua.marketing.customer.domain.repository

import dev.yidafu.aqua.common.domain.model.CustomerGroupModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * 客户分组仓库(F-MKT-001);@SoftDelete 自动过滤软删行。
 */
@Repository
interface CustomerGroupRepository :
  JpaRepository<CustomerGroupModel, Long>,
  JpaSpecificationExecutor<CustomerGroupModel> {
  fun findByName(name: String): CustomerGroupModel?

  fun existsByName(name: String): Boolean

  /**
   * 名称关键字模糊查询(分页,按创建时间倒序)
   */
  fun findByNameContainingIgnoreCaseOrderByCreatedAtDesc(
    @Param("keyword") keyword: String,
    pageable: org.springframework.data.domain.Pageable,
  ): org.springframework.data.domain.Page<CustomerGroupModel>
}
