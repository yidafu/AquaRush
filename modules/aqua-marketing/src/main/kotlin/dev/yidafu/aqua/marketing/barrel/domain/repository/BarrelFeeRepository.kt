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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

/**
 * 水桶费价目仓储(G06:独立价目表,按规格,与商品解耦)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删。
 */
interface BarrelFeeRepository {
  fun findBySpecification(specification: String): BarrelFeeModel?

  fun findAllByStatusOrderBySortOrderAscIdAsc(status: BarrelFeeStatus): List<BarrelFeeModel>

  fun findById(id: Long): Optional<BarrelFeeModel>

  /** 后台价目分页:status 精确 + specification 关键词(大小写不敏感);固定 sortOrder 升序、id 破平。 */
  fun searchFees(
    status: BarrelFeeStatus?,
    keyword: String?,
    pageable: Pageable,
  ): Page<BarrelFeeModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: BarrelFeeModel): BarrelFeeModel
}
