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
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

/**
 * 客户分组仓库(F-MKT-001)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删。
 */
interface CustomerGroupRepository {
  fun findByName(name: String): CustomerGroupModel?

  fun existsByName(name: String): Boolean

  fun findById(id: Long): Optional<CustomerGroupModel>

  fun existsById(id: Long): Boolean

  fun findAllById(ids: List<Long>): List<CustomerGroupModel>

  /** 后台分组分页:name 关键词(大小写不敏感);固定 createdAt 降序、id 破平。 */
  fun searchGroups(
    keyword: String?,
    pageable: Pageable,
  ): Page<CustomerGroupModel>

  /** 语义对齐 @SoftDelete 下的 delete:软删。 */
  fun delete(entity: CustomerGroupModel)

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: CustomerGroupModel): CustomerGroupModel
}
