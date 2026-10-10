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

import dev.yidafu.aqua.api.service.ops.BannerMutationService
import dev.yidafu.aqua.common.graphql.generated.BannerVo
import dev.yidafu.aqua.common.graphql.generated.CreateBannerInput
import dev.yidafu.aqua.common.graphql.generated.ReorderDirection
import dev.yidafu.aqua.common.graphql.generated.UpdateBannerInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.ops.banner.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 轮播图变更 Resolver(管理后台,PERMISSION_OPS_CONFIG_WRITE)
 * 操作人取自登录主体,由服务落审计日志(R-OPS-01)。
 */
@Controller
class AdminBannerMutationResolver(
  private val bannerMutationService: BannerMutationService,
) {
  private val logger = LoggerFactory.getLogger(AdminBannerMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun createBanner(
    @Argument @Valid input: CreateBannerInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): BannerVo {
    logger.info("Admin {} creating banner: {}", principal.username, input.title)
    val banner =
      bannerMutationService.createBanner(
        adminId = principal.id,
        adminName = principal.username,
        input = input,
      )
    return banner.toGraphQL()
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun updateBanner(
    @Argument id: Long,
    @Argument @Valid input: UpdateBannerInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): BannerVo {
    logger.info("Admin {} updating banner {}", principal.username, id)
    val banner =
      bannerMutationService.updateBanner(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        input = input,
      )
    return banner.toGraphQL()
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun deleteBanner(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Boolean {
    logger.info("Admin {} deleting banner {}", principal.username, id)
    return bannerMutationService.deleteBanner(principal.id, principal.username, id)
  }

  /**
   * 上移/下移调整排序,返回重排后的全量列表(按排序号升序,前端直接重渲染)
   */
  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_OPS_CONFIG_WRITE')")
  fun reorderBanner(
    @Argument id: Long,
    @Argument direction: ReorderDirection,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): List<BannerVo> {
    logger.info("Admin {} reordering banner {} direction {}", principal.username, id, direction)
    val directionEnum =
      dev.yidafu.aqua.api.dto.ReorderDirection
        .valueOf(direction.name)
    return bannerMutationService
      .reorderBanner(principal.id, principal.username, id, directionEnum)
      .map { it.toGraphQL() }
  }
}
