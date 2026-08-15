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
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 满减/折扣与首单立减规则单元测试(R-MKT-04/10,聚焦引擎测试未覆盖分支)。 */
class PromotionActivityRuleTest {
  private lateinit var promotionActivityRepository: PromotionActivityRepository
  private lateinit var promotionActivityTierRepository: PromotionActivityTierRepository
  private lateinit var firstOrderActivityRepository: FirstOrderActivityRepository
  private lateinit var orderQueryApiService: OrderQueryApiService
  private lateinit var marketingPricingQueryApiService: MarketingPricingQueryApiService
  private lateinit var rule: PromotionActivityRule

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
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

  private fun ctx(): PricingContext =
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

  private fun state(cashSubtotal: Long = 10500): PricingState = PricingState(cashSubtotalCents = cashSubtotal)

  private fun activity(
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

  private fun tier(
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

  @Test
  @DisplayName("分组范围:命中用户分组出优惠,未命中不参与")
  fun `group scope matches user group`() {
    every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
      listOf(
        activity(1L, scope = PromotionScope.GROUP, customerGroupId = 5L),
        activity(2L, scope = PromotionScope.GROUP, customerGroupId = 9L),
      )
    every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(1000, amount = 1000))
    every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(2L) } returns listOf(tier(1000, amount = 2000))

    val s = state()
    rule.evaluate(ctx(), s)

    assertEquals(1000, s.promotionDiscountCents)
    assertEquals(1L, s.promotionActivityId)
  }

  @Test
  @DisplayName("满折档位缺折扣率:跳过该活动")
  fun `full discount tier without rate skipped`() {
    every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
      listOf(activity(1L, type = PromotionActivityType.FULL_DISCOUNT))
    every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(1000, amount = 500))

    val s = state()
    rule.evaluate(ctx(), s)

    assertEquals(0, s.promotionDiscountCents)
    assertNull(s.promotionActivityId)
  }

  @Test
  @DisplayName("无达标档位:活动不命中")
  fun `no matched tier skips activity`() {
    every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns listOf(activity(1L))
    every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(50000, amount = 1000))

    val s = state(cashSubtotal = 10500)
    rule.evaluate(ctx(), s)

    assertEquals(0, s.promotionDiscountCents)
  }

  @Test
  @DisplayName("首单立减:GROUP 范围不支持(过滤)")
  fun `first order group scope filtered out`() {
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

    assertEquals(0, s.firstOrderDiscountCents)
    assertNull(s.firstOrderActivityId)
  }

  @Test
  @DisplayName("首单立减:现金小计未达门槛 → 不命中")
  fun `first order below threshold not applied`() {
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

    assertEquals(0, s.firstOrderDiscountCents)
  }

  @Test
  @DisplayName("首单立减:金额与折扣率均缺失 → 跳过")
  fun `first order without amount and rate skipped`() {
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

    assertEquals(0, s.firstOrderDiscountCents)
  }

  @Test
  @DisplayName("首单立减:满减更高时首单清零,满减胜出")
  fun `promotion wins over first order`() {
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

    assertEquals(2000, s.promotionDiscountCents)
    assertEquals(0, s.firstOrderDiscountCents)
    assertNull(s.firstOrderActivityId)
  }

  @Test
  @DisplayName("满折:按现金小计折算并封顶")
  fun `full discount computes and caps`() {
    every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, now) } returns
      listOf(activity(1L, type = PromotionActivityType.FULL_DISCOUNT))
    every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns listOf(tier(1000, permille = 9000))

    val s = state(cashSubtotal = 10500)
    rule.evaluate(ctx(), s)

    // 10500 × 10% = 1050(向下取整)
    assertEquals(1050, s.promotionDiscountCents)
  }
}
