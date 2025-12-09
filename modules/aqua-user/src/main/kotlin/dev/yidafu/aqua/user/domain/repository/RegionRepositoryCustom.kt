/*
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

import dev.yidafu.aqua.user.domain.model.RegionModel

/**
 * Custom repository interface for Region entity with QueryDSL implementations
 */
interface RegionRepositoryCustom {
  /**
   * Find root regions (parentCode = '0') at a specific level
   * @param level the region level
   * @return list of root regions
   */
  fun findRootRegions(level: Int): List<RegionModel>

  /**
   * Check if a region exists with given name, level, and parent code
   * @param name the region name
   * @param level the region level
   * @param parentCode the parent region code
   * @return true if region exists, false otherwise
   */
  fun existsByNameAndLevelAndParentCode(
    name: String,
    level: Int,
    parentCode: String
  ): Boolean
}