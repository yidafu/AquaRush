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

import dev.yidafu.aqua.api.service.marketing.GiftActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreateGiftActivityInput
import dev.yidafu.aqua.common.graphql.generated.GiftActivity
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateGiftActivityInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.gift.mapper.toGraphQL
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class AdminGiftActivityMutationResolver(
  private val giftActivityMutationService: GiftActivityMutationService,
  private val productQueryApiService: ProductQueryApiService,
) {
  private val logger = LoggerFactory.getLogger(AdminGiftActivityMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun createGiftActivity(
    @Argument @Valid input: CreateGiftActivityInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): GiftActivity {
    logger.info("Admin {} creating gift activity: {}", principal.username, input.name)
    val activity =
      giftActivityMutationService.createActivity(
        adminId = principal.id,
        adminName = principal.username,
        input = input,
      )
    return toGiftActivity(activity)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun updateGiftActivity(
    @Argument id: Long,
    @Argument @Valid input: UpdateGiftActivityInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): GiftActivity {
    logger.info("Admin {} updating gift activity {}", principal.username, id)
    val activity =
      giftActivityMutationService.updateActivity(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        input = input,
      )
    return toGiftActivity(activity)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun disableGiftActivity(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): GiftActivity {
    logger.info("Admin {} disabling gift activity {}", principal.username, id)
    val activity =
      giftActivityMutationService.disableActivity(principal.id, principal.username, id)
    return toGiftActivity(activity)
  }

  private fun toGiftActivity(model: GiftActivityModel): GiftActivity {
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
      giftProductName = productQueryApiService.findById(model.giftProductId)?.name,
    )
  }
}
