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
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

/** 满减/折扣与首单立减规则单元测试(R-MKT-04/10,聚焦引擎测试未覆盖分支)。 */
class PromotionActivityRuleTest :
  FunSpec({
    var promotionActivityRepository: PromotionActivityRepository = mockk()
    var promotionActivityTierRepository: PromotionActivityTierRepository = mockk()
    var firstOrderActivityRepository: FirstOrderActivityRepository = mockk()
    var orderQueryApiService: OrderQueryApiService = mockk()
    var marketingPricingQueryApiService: MarketingPricingQueryApiService = mockk()
    var rule: PromotionActivityRule = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun ctx(): PricingContext =
      PricingContext(
        userId = 1L,
        items =
          listOf(
            PricingItemContext(
              productId = 1L,
              quantity = 5,
              product = ProductModel(id = 1L, name = "水", price = 2100, specification = "19L"),
            ),
          ),
        isSelfCollect = false,
        preferredCouponId = null,
        ticketRedeemCount = 0,
        now = now,
      )

    fun state(cashSubtotal: Long = 10500): PricingState = PricingState(cashSubtotalCents = cashSubtotal)

    fun activity(
      id: Long,
      type: PromotionActivityType = PromotionActivityType.FULL_REDUCE,
      scope: PromotionScope = PromotionScope.ALL,
      productId: Long? = null,
      customerGroupId: Long? = null,
    ): PromotionActivityModel =
      PromotionActivityModel(
        id = id,
        name = "活动$id",
        activityType = type,
        scope = scope,
        productId = productId,
        customerGroupId = customerGroupId,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
        status = PromotionActivityStatus.ACTIVE,
      )

    fun tier(
      threshold: Long,
      amount: Long? = null,
      permille: Long? = null,
    ): PromotionActivityTierModel =
      PromotionActivityTierModel(
        id = null,
        activityId = 0,
        thresholdCents = threshold,
        discountAmountCents = amount,
        discountRatePermille = permille,
        sort = 0,
      )

    beforeTest {
      promotionActivityRepository = mockk()
      promotionActivityTierRepository = mockk()
      firstOrderActivityRepository = mockk()
      orderQueryApiService = mockk()
      marketingPricingQueryApiService = mockk()
      rule =
        PromotionActivityRule(
          promotionActivityRepository,
          promotionActivityTierRepository,
          firstOrderActivityRepository,
          orderQueryApiService,
          marketingPricingQueryApiService,
        )
      every { marketingPricingQueryApiService.findGroupId(1L) } returns 5L
      every { orderQueryApiService.hasCompletedOrders(1L) } returns false
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns emptyList()
      every { firstOrderActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns emptyList()
    }
    test("group scope matches user group") {
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(
          activity(1L, scope = PromotionScope.GROUP, customerGroupId = 5L),
          activity(2L, scope = PromotionScope.GROUP, customerGroupId = 9L),
        )
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(1000, amount = 1000))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(2L) } returns listOf(tier(1000, amount = 2000))

      val s = state()
      rule.evaluate(ctx(), s)

      s.promotionDiscountCents shouldBe 1000
      s.promotionActivityId shouldBe 1L
    }
    test("full discount tier without rate skipped") {
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(activity(1L, type = PromotionActivityType.FULL_DISCOUNT))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(1000, amount = 500))

      val s = state()
      rule.evaluate(ctx(), s)

      s.promotionDiscountCents shouldBe 0
      s.promotionActivityId.shouldBeNull()
    }
    test("no matched tier skips activity") {
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns listOf(activity(1L))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(50000, amount = 1000))

      val s = state(cashSubtotal = 10500)
      rule.evaluate(ctx(), s)

      s.promotionDiscountCents shouldBe 0
    }
    test("first order group scope filtered out") {
      every { firstOrderActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(
          FirstOrderActivityModel(
            id = 1L,
            name = "首单",
            activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
            thresholdCents = 5000,
            discountAmountCents = 1000,
            scope = PromotionScope.GROUP,
            status = PromotionActivityStatus.ACTIVE,
          ),
        )

      val s = state()
      rule.evaluate(ctx(), s)

      s.firstOrderDiscountCents shouldBe 0
      s.firstOrderActivityId.shouldBeNull()
    }
    test("first order below threshold not applied") {
      every { firstOrderActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(
          FirstOrderActivityModel(
            id = 1L,
            name = "首单",
            activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
            thresholdCents = 50000,
            discountAmountCents = 1000,
            scope = PromotionScope.ALL,
            status = PromotionActivityStatus.ACTIVE,
          ),
        )

      val s = state(cashSubtotal = 10500)
      rule.evaluate(ctx(), s)

      s.firstOrderDiscountCents shouldBe 0
    }
    test("first order without amount and rate skipped") {
      every { firstOrderActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(
          FirstOrderActivityModel(
            id = 1L,
            name = "首单",
            activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
            thresholdCents = 5000,
            scope = PromotionScope.ALL,
            status = PromotionActivityStatus.ACTIVE,
          ),
        )

      val s = state()
      rule.evaluate(ctx(), s)

      s.firstOrderDiscountCents shouldBe 0
    }
    test("promotion wins over first order") {
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns listOf(activity(1L))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(1000, amount = 2000))
      every { firstOrderActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(
          FirstOrderActivityModel(
            id = 1L,
            name = "首单",
            activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
            thresholdCents = 5000,
            discountAmountCents = 1000,
            scope = PromotionScope.ALL,
            status = PromotionActivityStatus.ACTIVE,
          ),
        )

      val s = state()
      rule.evaluate(ctx(), s)

      s.promotionDiscountCents shouldBe 2000
      s.firstOrderDiscountCents shouldBe 0
      s.firstOrderActivityId.shouldBeNull()
    }
    test("full discount computes and caps") {
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
        listOf(activity(1L, type = PromotionActivityType.FULL_DISCOUNT))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(1000, permille = 9000))

      val s = state(cashSubtotal = 10500)
      rule.evaluate(ctx(), s)

      // 10500 × 10% = 1050(向下取整)
      s.promotionDiscountCents shouldBe 1050
    }
  })
