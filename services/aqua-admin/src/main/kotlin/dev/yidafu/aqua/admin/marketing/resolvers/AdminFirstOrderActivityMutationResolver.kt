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

import dev.yidafu.aqua.api.service.marketing.FirstOrderActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreateFirstOrderActivityInput
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityVo
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateFirstOrderActivityInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.firstorder.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class AdminFirstOrderActivityMutationResolver(
  private val firstOrderActivityMutationService: FirstOrderActivityMutationService,
  private val productQueryApiService: ProductQueryApiService,
) {
  private val logger = LoggerFactory.getLogger(AdminFirstOrderActivityMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun createFirstOrderActivity(
    @Argument @Valid input: CreateFirstOrderActivityInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): FirstOrderActivityVo {
    logger.info("Admin {} creating first order activity: {}", principal.username, input.name)
    val activity =
      firstOrderActivityMutationService.createActivity(
        adminId = principal.id,
        adminName = principal.username,
        input = input,
      )
    return toFirstOrderActivity(activity)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun updateFirstOrderActivity(
    @Argument id: Long,
    @Argument @Valid input: UpdateFirstOrderActivityInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): FirstOrderActivityVo {
    logger.info("Admin {} updating first order activity {}", principal.username, id)
    val activity =
      firstOrderActivityMutationService.updateActivity(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        input = input,
      )
    return toFirstOrderActivity(activity)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun disableFirstOrderActivity(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): FirstOrderActivityVo {
    logger.info("Admin {} disabling first order activity {}", principal.username, id)
    val activity =
      firstOrderActivityMutationService.disableActivity(principal.id, principal.username, id)
    return toFirstOrderActivity(activity)
  }

  private fun toFirstOrderActivity(model: FirstOrderActivityModel): FirstOrderActivityVo {
    val now = LocalDateTime.now()
    val currentStatus =
      when {
        model.status == PromotionActivityStatus.DISABLED -> PromotionActivityDisplayStatus.DISABLED
        now.isBefore(model.validFrom) -> PromotionActivityDisplayStatus.UPCOMING
        now.isAfter(model.validTo) -> PromotionActivityDisplayStatus.ENDED
        else -> PromotionActivityDisplayStatus.ONGOING
      }
    return model.toGraphQL(
      currentStatus,
      productName = model.productId?.let { productQueryApiService.findById(it)?.name },
    )
  }
}
