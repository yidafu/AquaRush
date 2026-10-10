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

import dev.yidafu.aqua.api.service.ops.StoreProfileMutationService
import dev.yidafu.aqua.common.graphql.generated.SaveStoreProfileInput
import dev.yidafu.aqua.common.graphql.generated.StoreProfileVo
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.ops.storeprofile.mapper.toGraphQL
import dev.yidafu.aqua.ops.storeprofile.mapper.toRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 店铺信息变更 Resolver(管理后台,PERMISSION_OPS_CONFIG_WRITE)
 * 配置即展示(R-OPS-01),每次保存留审计日志;操作人取自登录主体。
 */
@Controller
class AdminStoreProfileMutationResolver(
  private val storeProfileMutationService: StoreProfileMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminStoreProfileMutationResolver::class.java)

  /**
   * 保存店铺信息(单行配置,整体覆盖)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun saveStoreProfile(
    @Argument @Valid input: SaveStoreProfileInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): StoreProfileVo {
    logger.info("Admin {} saving store profile", principal.username)
    val profile =
      storeProfileMutationService.saveStoreProfile(
        adminId = principal.id,
        adminName = principal.username,
        request = input.toRequest(),
      )
    return profile.toGraphQL()
  }
}
