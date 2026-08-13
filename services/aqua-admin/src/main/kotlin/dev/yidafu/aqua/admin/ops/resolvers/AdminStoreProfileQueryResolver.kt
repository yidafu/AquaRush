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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.api.service.ops.StoreProfileQueryService
import dev.yidafu.aqua.common.graphql.generated.StoreProfile
import dev.yidafu.aqua.ops.storeprofile.mapper.StoreProfileMapper
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * 店铺信息查询 Resolver(管理后台,PERMISSION_OPS_CONFIG_READ)
 * 与 PRD 12-operations-config.md §F-OPS-004 保持一致(单行配置,未配置返回 null)。
 */
@Controller
class AdminStoreProfileQueryResolver(
  private val storeProfileQueryService: StoreProfileQueryService,
) {
  /**
   * 获取当前店铺配置(未配置返回 null)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_READ')")
  fun storeProfile(): StoreProfile? = storeProfileQueryService.getStoreProfile()?.let { StoreProfileMapper.map(it) }
}
