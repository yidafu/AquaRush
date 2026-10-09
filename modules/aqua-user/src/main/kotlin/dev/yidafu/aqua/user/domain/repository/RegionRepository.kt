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

package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.common.domain.model.RegionModel

/** 行政区划仓储(JPA→Exposed 迁移 P3 收尾)。 */
interface RegionRepository : RegionRepositoryCustom {
  fun findByCode(code: String): RegionModel?

  fun findByParentCodeAndLevel(
    parentCode: String,
    level: Int,
  ): List<RegionModel>

  fun findByLevel(level: Int): List<RegionModel>

  fun existsByCode(code: String): Boolean

  fun existsByCodeAndLevel(
    code: String,
    level: Int,
  ): Boolean

  fun findByNameContainingAndLevelOrderByCode(
    name: String,
    level: Int,
  ): List<RegionModel>

  fun findByNameContainingOrderByCode(name: String): List<RegionModel>

  fun findByParentCodeOrderByCode(parentCode: String): List<RegionModel>

  fun findByLevelOrderByCode(level: Int): List<RegionModel>

  // ===== 基础操作(JpaRepository 显式化)=====
  fun findAll(): List<RegionModel>

  fun findById(id: Long): RegionModel?

  fun save(entity: RegionModel): RegionModel

  /** 语义对齐 @SoftDelete 下的 delete:软删。 */
  fun delete(entity: RegionModel)
}
