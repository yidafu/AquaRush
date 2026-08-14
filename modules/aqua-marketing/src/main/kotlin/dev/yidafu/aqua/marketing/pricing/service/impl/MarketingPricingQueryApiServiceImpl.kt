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

package dev.yidafu.aqua.marketing.pricing.service.impl

import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import dev.yidafu.aqua.api.service.marketing.PriceResolution
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
import dev.yidafu.aqua.marketing.pricing.domain.repository.GroupProductPriceRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 营销取价服务实现(R-MKT-01/02 成交单价解析):
 * ① 手动实收价(预留,未接入)→ ② 用户所属分组商品专属价(熟客价)→ ③ 限时特价(R-MKT-05)
 * → ④ 新人专享价(R-MKT-10,仅无已完成订单的新用户)→ ⑤ 商品目录价。
 */
@Service
class MarketingPricingQueryApiServiceImpl(
  private val userQueryApiService: UserQueryApiService,
  private val productQueryApiService: ProductQueryApiService,
  private val groupProductPriceRepository: GroupProductPriceRepository,
  private val flashSaleActivityRepository: FlashSaleActivityRepository,
  private val firstOrderActivityRepository: FirstOrderActivityRepository,
  private val orderQueryApiService: OrderQueryApiService,
) : MarketingPricingQueryApiService {
  override fun resolveUnitPrice(
    userId: Long,
    productId: Long,
  ): PriceResolution {
    val product =
      productQueryApiService.findById(productId)
        ?: throw NotFoundException("产品不存在: $productId")

    // ② 用户所属分组商品专属价(熟客价)
    val groupId = findGroupId(userId)
    if (groupId != null) {
      val groupPrice = groupProductPriceRepository.findByCustomerGroupIdAndProductId(groupId, productId)
      if (groupPrice != null) {
        return PriceResolution(
          unitPriceCents = groupPrice.priceCents,
          priceSource = PriceSource.GROUP_PRICE,
          hasGroupPrice = true,
        )
      }
    }
    // ③ 限时特价(活动期自动按特价计价,到期自动失效)
    val now = LocalDateTime.now()
    val flashSale =
      flashSaleActivityRepository.findActiveByProductId(productId, PromotionActivityStatus.ACTIVE, now)
    if (flashSale != null) {
      return PriceResolution(
        unitPriceCents = flashSale.salePriceCents,
        priceSource = PriceSource.FLASH_SALE_PRICE,
        hasGroupPrice = false,
        hasFlashSale = true,
      )
    }
    // ④ 新人专享价(仅当存在进行中配置才判定新用户,避免列表页每商品查订单)
    val newcomerPrice =
      firstOrderActivityRepository.findActiveNewcomerPriceByProductId(
        productId,
        FirstOrderActivityType.NEWCOMER_PRICE,
        PromotionActivityStatus.ACTIVE,
        now,
      )
    if (newcomerPrice != null && !orderQueryApiService.hasCompletedOrders(userId)) {
      return PriceResolution(
        unitPriceCents = newcomerPrice.newcomerPriceCents ?: product.price,
        priceSource = PriceSource.NEWCOMER_PRICE,
        hasGroupPrice = false,
        hasNewcomerPrice = true,
      )
    }
    // ⑤ 商品目录价
    return PriceResolution(
      unitPriceCents = product.price,
      priceSource = PriceSource.CATALOG_PRICE,
      hasGroupPrice = false,
    )
  }

  override fun findGroupId(userId: Long): Long? {
    val user = userQueryApiService.findById(userId) ?: return null
    return user.customerGroupId
  }
}
