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

import dev.yidafu.aqua.api.service.marketing.FlashSaleActivityMutationService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreateFlashSaleActivityInput
import dev.yidafu.aqua.common.graphql.generated.FlashSaleActivity
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityDisplayStatus
import dev.yidafu.aqua.common.graphql.generated.UpdateFlashSaleActivityInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.marketing.flashsale.mapper.CreateFlashSaleActivityInputMapper
import dev.yidafu.aqua.marketing.flashsale.mapper.FlashSaleActivityMapper
import dev.yidafu.aqua.marketing.flashsale.mapper.UpdateFlashSaleActivityInputMapper
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.time.LocalDateTime

@Controller
class AdminFlashSaleMutationResolver(
  private val flashSaleActivityMutationService: FlashSaleActivityMutationService,
  private val productQueryApiService: ProductQueryApiService,
) {
  private val logger = LoggerFactory.getLogger(AdminFlashSaleMutationResolver::class.java)

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun createFlashSaleActivity(
    @Argument @Valid input: CreateFlashSaleActivityInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): FlashSaleActivity {
    logger.info("Admin {} creating flash sale activity: {}", principal.username, input.name)
    val activity =
      flashSaleActivityMutationService.createActivity(
        adminId = principal.id,
        adminName = principal.username,
        request = CreateFlashSaleActivityInputMapper.map(input),
      )
    return toFlashSaleActivity(activity)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun updateFlashSaleActivity(
    @Argument id: Long,
    @Argument @Valid input: UpdateFlashSaleActivityInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): FlashSaleActivity {
    logger.info("Admin {} updating flash sale activity {}", principal.username, id)
    val activity =
      flashSaleActivityMutationService.updateActivity(
        adminId = principal.id,
        adminName = principal.username,
        id = id,
        request = UpdateFlashSaleActivityInputMapper.map(input),
      )
    return toFlashSaleActivity(activity)
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_MKT_WRITE')")
  fun disableFlashSaleActivity(
    @Argument id: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): FlashSaleActivity {
    logger.info("Admin {} disabling flash sale activity {}", principal.username, id)
    val activity =
      flashSaleActivityMutationService.disableActivity(principal.id, principal.username, id)
    return toFlashSaleActivity(activity)
  }

  private fun toFlashSaleActivity(model: FlashSaleActivityModel): FlashSaleActivity {
    val product = productQueryApiService.findById(model.productId)
    val now = LocalDateTime.now()
    val currentStatus =
      when {
        model.status == PromotionActivityStatus.DISABLED -> PromotionActivityDisplayStatus.DISABLED
        now.isBefore(model.validFrom) -> PromotionActivityDisplayStatus.UPCOMING
        now.isAfter(model.validTo) -> PromotionActivityDisplayStatus.ENDED
        else -> PromotionActivityDisplayStatus.ONGOING
      }
    return FlashSaleActivityMapper.toGraphQL(
      model,
      currentStatus,
      productName = product?.name,
      catalogPriceCents = product?.price,
    )
  }
}
