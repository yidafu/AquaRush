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

package dev.yidafu.aqua.user.domain.model

import dev.yidafu.aqua.common.domain.model.RegionModel

/**
 * Region hierarchy data model for default region configuration
 * Represents the complete province-city-district hierarchy with
 * all available options for selection
 */
data class RegionHierarchyModel(
  /**
     * The default selected province
     */
    val province: RegionModel,

  /**
     * The default selected city
     */
    val city: RegionModel,

  /**
     * The default selected district
     */
    val district: RegionModel,

  /**
     * All available provinces for selection
     */
    val provinces: List<RegionModel>,

  /**
     * All cities in the selected province
     */
    val cities: List<RegionModel>,

  /**
     * All districts in the selected city
     */
    val districts: List<RegionModel>
)
