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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.PromotionActivityMutationService
import dev.yidafu.aqua.api.service.marketing.PromotionActivityQueryService
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreatePromotionActivityInput
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityVo
import dev.yidafu.aqua.common.graphql.generated.UpdatePromotionActivityInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.promotion.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

/**
 * 满减/折扣活动变更 Resolver(管理后台,PERMISSION_MKT_WRITE)
 * 配置变更不影响已生成订单(成交价按下单时快照);停用立即失效(PRD 10-marketing.md §F-MKT-004)。
 */
@Controller
class AdminPromotionActivityMutationResolver(
  private val promotionActivityMutationService: PromotionActivityMutationService,
  private val promotionActivityQueryService: PromotionActivityQueryService,
) {
  private val logger = LoggerFactory.getLogger(AdminPromotionActivityMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun createPromotionActivity(
    @Argument @Valid input: CreatePromotionActivityInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): PromotionActivityVo {
    logger.info("Admin {} creating promotion activity: {}", principal.username, input.name)
    val activity =
      promotionActivityMutationService.createActivity(
        adminId = principal.id,
        adminName = principal.username,
        input = input,
      )
    return toPromotionActivity(activity)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun updatePromotionActivity(
    @Argument id: Long,
    @Argument @Valid input: UpdatePromotionActivityInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): PromotionActivityVo {
    logger.info("Admin {} updating promotion activity {}", principal.username, id)
    val activity =
      promotionActivityMutationService.updateActivity(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        input = input,
      )
    return toPromotionActivity(activity)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun disablePromotionActivity(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): PromotionActivityVo {
    logger.info("Admin {} disabling promotion activity {}", principal.username, id)
    val activity =
      promotionActivityMutationService.disableActivity(principal.id, principal.username, id)
    return toPromotionActivity(activity)
  }

  private fun toPromotionActivity(model: PromotionActivityModel): PromotionActivityVo {
    val tiers = promotionActivityQueryService.listTiers(model.id ?: 0L)
    val now = LocalDateTime.now()
    val currentStatus =
      when {
        model.status == PromotionActivityStatus.DISABLED -> PromotionActivityDisplayStatus.DISABLED
        now.isBefore(model.validFrom) -> PromotionActivityDisplayStatus.UPCOMING
        now.isAfter(model.validTo) -> PromotionActivityDisplayStatus.ENDED
        else -> PromotionActivityDisplayStatus.ONGOING
      }
    return model.toGraphQL(tiers, currentStatus)
  }
}
