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

package dev.yidafu.aqua.user.graphql.resolvers

import dev.yidafu.aqua.user.domain.model.Region
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class RegionQueryResolver(
  private val regionRepository: RegionRepository,
) {

  @QueryMapping
  fun regions(
    @Argument level: Int?,
    @Argument parentCode: String?
  ): List<Region> {
    return when {
      level != null && parentCode != null -> {
        regionRepository.findByParentCodeAndLevel(parentCode, level)
      }
      level != null -> {
        if (level == 1) {
          regionRepository.findRootRegions(level)
        } else {
          regionRepository.findByLevel(level)
        }
      }
      else -> {
        regionRepository.findAll()
      }
    }
  }

  @QueryMapping
  fun region(
    @Argument code: String
  ): Region? {
    return regionRepository.findByCode(code)
  }
}