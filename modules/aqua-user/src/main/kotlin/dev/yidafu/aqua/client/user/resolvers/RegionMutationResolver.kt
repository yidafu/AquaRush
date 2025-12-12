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

package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.common.graphql.BaseGraphQLResolver
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.model.RegionModel
import dev.yidafu.aqua.user.domain.repository.RegionRepository
import dev.yidafu.aqua.user.service.CreateRegionInput
import dev.yidafu.aqua.user.service.RegionService
import dev.yidafu.aqua.user.service.UpdateRegionInput
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

@Controller
class RegionMutationResolver(
    private val regionService: RegionService,
    private val regionRepository: RegionRepository,
) : BaseGraphQLResolver() {

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun createRegion(
    @Argument input: CreateRegionInput,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?
  ): RegionModel {
    // 记录操作日志
    logOperation(userPrincipal, "createRegion", mapOf<String, Any>(
      "name" to input.name,
      "code" to input.code,
      "level" to input.level,
      "parentCode" to (input.parentCode ?: "")
    ))

    return regionService.createRegion(input)
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun updateRegion(
    @Argument code: String,
    @Argument input: UpdateRegionInput,
    @AuthenticationPrincipal userPrincipal: UserPrincipal?
  ): RegionModel {
    // 记录操作日志
    logOperation(userPrincipal, "updateRegion", mapOf<String, Any>(
      "code" to code,
      "name" to (input.name ?: ""),
      "parentCode" to (input.parentCode ?: "")
    ))

    return regionService.updateRegion(code, input)
  }

  @MutationMapping
  @PreAuthorize("hasRole('ADMIN')")
  fun deleteRegion(
      @Argument code: String,
      @AuthenticationPrincipal userPrincipal: UserPrincipal?
  ): Boolean {
    // 记录操作日志
    logOperation(userPrincipal, "deleteRegion", mapOf<String, Any>(
      "code" to code
    ))

    return regionService.deleteRegion(code)
  }
}

