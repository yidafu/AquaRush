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

package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

/** 买赠/满赠规则单元测试(R-MKT-09,聚焦引擎测试未覆盖分支:分组范围/赠品缺失)。 */
class GiftRuleTest :
  FunSpec({
    var giftActivityRepository: GiftActivityRepository = mockk()
    var marketingPricingQueryApiService: MarketingPricingQueryApiService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var rule: GiftRule = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun ctx(cashQuantity: Int = 5): PricingContext =
      PricingContext(
        userId = 1L,
        items =
          listOf(
            PricingItemContext(
              productId = 1L,
              quantity = cashQuantity,
              product = ProductModel(id = 1L, name = "水", price = 2100, specification = "19L"),
            ),
          ),
        isSelfCollect = false,
        preferredCouponId = null,
        ticketRedeemCount = 0,
        now = now,
      )

    fun state(cashSubtotal: Long = 10500): PricingState = PricingState(cashSubtotalCents = cashSubtotal)

    fun giftActivity(
      id: Long,
      type: GiftActivityType = GiftActivityType.FULL_GIFT,
      scope: PromotionScope = PromotionScope.ALL,
      productId: Long? = null,
      customerGroupId: Long? = null,
      thresholdCents: Long? = 10000,
      thresholdQuantity: Int? = null,
    ): GiftActivityModel =
      GiftActivityModel(
        id = id,
        name = "赠品活动$id",
        giftType = type,
        thresholdCents = thresholdCents,
        thresholdQuantity = thresholdQuantity,
        scope = scope,
        productId = productId,
        customerGroupId = customerGroupId,
        giftProductId = 2L,
        giftQuantity = 1,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
        status = PromotionActivityStatus.ACTIVE,
      )

    beforeTest {
      giftActivityRepository = mockk()
      marketingPricingQueryApiService = mockk()
      productQueryApiService = mockk()
      rule = GiftRule(giftActivityRepository, marketingPricingQueryApiService, productQueryApiService)
      every { marketingPricingQueryApiService.findGroupId(1L) } returns 5L
    }
    test("group scope matches user group") {
      every { giftActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(
          giftActivity(1L, scope = PromotionScope.GROUP, customerGroupId = 5L, thresholdCents = 10000),
          giftActivity(2L, scope = PromotionScope.GROUP, customerGroupId = 9L, thresholdCents = 10000),
        )
      every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)

      val s = state()
      rule.evaluate(ctx(), s)

      s.giftItems.size shouldBe 1
      s.giftItems[0].giftActivityId shouldBe 1L
    }
    test("product scope without matching product") {
      every { giftActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(giftActivity(1L, scope = PromotionScope.PRODUCT, productId = 99L, thresholdCents = 10000))

      val s = state()
      rule.evaluate(ctx(), s)

      s.giftItems.isEmpty() shouldBe true
    }
    test("missing gift product falls back to gift name") {
      every { giftActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(giftActivity(1L, thresholdCents = 10000))
      every { productQueryApiService.findById(2L) } returns null

      val s = state()
      rule.evaluate(ctx(), s)

      s.giftItems.size shouldBe 1
      s.giftItems[0].productName shouldBe "赠品"
    }
    test("full gift with null threshold always matches") {
      every { giftActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(giftActivity(1L, thresholdCents = null))
      every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)

      val s = state(cashSubtotal = 0)
      rule.evaluate(ctx(), s)

      s.giftItems.size shouldBe 1
    }
  })
