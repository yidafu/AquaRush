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

package dev.yidafu.aqua.marketing.usage.service.impl

import dev.yidafu.aqua.api.dto.MarketingUsageItemRow
import dev.yidafu.aqua.api.dto.MarketingUsageKind
import dev.yidafu.aqua.api.dto.MarketingUsageOrderCriteria
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.util.Optional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

/** 营销使用情况查询服务单元测试(PRD 10-marketing.md 营销方式详情,F-MKT-003~009)。 */
class MarketingUsageQueryServiceImplTest :
  FunSpec({
    var orderQueryApiService: OrderQueryApiService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var userCouponRepository: UserCouponRepository = mockk()
    var flashSaleActivityRepository: FlashSaleActivityRepository = mockk()
    var giftActivityRepository: GiftActivityRepository = mockk()
    var firstOrderActivityRepository: FirstOrderActivityRepository = mockk()
    var service: MarketingUsageQueryServiceImpl = mockk()

    val from = LocalDateTime.of(2025, 1, 1, 0, 0)
    val to = LocalDateTime.of(2025, 2, 1, 0, 0)

    fun orderPage(vararg ids: Long): Page<OrderModel> =
      PageImpl(ids.map { OrderModel(id = it, orderNo = "ORD-$it") }, PageRequest.of(0, 20), ids.size.toLong())

    beforeTest {
      orderQueryApiService = mockk()
      productQueryApiService = mockk()
      userCouponRepository = mockk()
      flashSaleActivityRepository = mockk()
      giftActivityRepository = mockk()
      firstOrderActivityRepository = mockk()
      service =
        MarketingUsageQueryServiceImpl(
          orderQueryApiService = orderQueryApiService,
          productQueryApiService = productQueryApiService,
          userCouponRepository = userCouponRepository,
          flashSaleActivityRepository = flashSaleActivityRepository,
          giftActivityRepository = giftActivityRepository,
          firstOrderActivityRepository = firstOrderActivityRepository,
        )
    }

    test("couponTemplateUsage returns summary and orders") {
      val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.COUPON_TEMPLATE, 1L)
      every { orderQueryApiService.countOrdersByMarketingUsage(criteria) } returns 3L
      every { orderQueryApiService.sumMarketingDiscountCents(criteria) } returns 1500L
      every { orderQueryApiService.findOrdersByMarketingUsage(criteria, 0, 20) } returns orderPage(1L, 2L)
      every { userCouponRepository.countByCouponTemplateIdAndStatus(1L, UserCouponStatus.USED) } returns 5L

      val result = service.couponTemplateUsage(1L, 0, 20)

      result.summary.orderCount shouldBe 3L
      result.summary.benefitCents shouldBe 1500L
      result.summary.usageCount shouldBe 5L
      result.orders.content.size shouldBe 2
    }

    test("promotionActivityUsage uses order count as usage count") {
      val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.PROMOTION_ACTIVITY, 2L)
      every { orderQueryApiService.countOrdersByMarketingUsage(criteria) } returns 4L
      every { orderQueryApiService.sumMarketingDiscountCents(criteria) } returns 4000L
      every { orderQueryApiService.findOrdersByMarketingUsage(criteria, 0, 20) } returns orderPage(1L)

      val result = service.promotionActivityUsage(2L, 0, 20)

      result.summary.orderCount shouldBe 4L
      result.summary.benefitCents shouldBe 4000L
      result.summary.usageCount shouldBe 4L
    }

    test("flashSaleActivityUsage matches items by product+price+window and estimates benefit") {
      val activity =
        FlashSaleActivityModel(id = 3L, productId = 9L, salePriceCents = 1500L, validFrom = from, validTo = to)
      every { flashSaleActivityRepository.findById(3L) } returns Optional.of(activity)
      val criteria =
        MarketingUsageOrderCriteria(
          kind = MarketingUsageKind.FLASH_SALE_ITEM,
          refId = 3L,
          productId = 9L,
          unitPriceCents = 1500L,
          validFrom = from,
          validTo = to,
        )
      every { orderQueryApiService.aggregateMarketingUsageItems(criteria) } returns
        listOf(MarketingUsageItemRow(productId = 9L, unitPriceCents = 1500L, quantitySum = 3L))
      every { orderQueryApiService.countOrdersByMarketingUsage(criteria) } returns 2L
      every { orderQueryApiService.findOrdersByMarketingUsage(criteria, 0, 20) } returns orderPage(1L, 2L)
      every { productQueryApiService.findById(9L) } returns ProductModel(price = 2100L)

      val result = service.flashSaleActivityUsage(3L, 0, 20)

      result.summary.orderCount shouldBe 2L
      result.summary.usageCount shouldBe 3L
      result.summary.benefitCents shouldBe 3L * (2100L - 1500L)
    }

    test("flashSaleActivityUsage returns empty when activity missing") {
      every { flashSaleActivityRepository.findById(99L) } returns Optional.empty()

      val result = service.flashSaleActivityUsage(99L, 0, 20)

      result.summary.orderCount shouldBe 0L
      result.summary.benefitCents shouldBe 0L
      result.orders.content shouldBe emptyList()
    }

    test("giftActivityUsage sums gift quantity and estimates benefit at current catalog price") {
      val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.GIFT_ACTIVITY, 4L)
      every { giftActivityRepository.findById(4L) } returns Optional.of(GiftActivityModel(id = 4L))
      every { orderQueryApiService.aggregateMarketingUsageItems(criteria) } returns
        listOf(MarketingUsageItemRow(productId = 8L, unitPriceCents = null, quantitySum = 5L))
      every { orderQueryApiService.countOrdersByMarketingUsage(criteria) } returns 2L
      every { orderQueryApiService.findOrdersByMarketingUsage(criteria, 0, 20) } returns orderPage(1L, 2L)
      every { productQueryApiService.findById(8L) } returns ProductModel(price = 1000L)

      val result = service.giftActivityUsage(4L, 0, 20)

      result.summary.orderCount shouldBe 2L
      result.summary.usageCount shouldBe 5L
      result.summary.benefitCents shouldBe 5L * 1000L
    }

    test("firstOrderActivityUsage FIRST_ORDER_REDUCE sums first-order discount") {
      val activity = FirstOrderActivityModel(id = 5L, activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE)
      every { firstOrderActivityRepository.findById(5L) } returns Optional.of(activity)
      val criteria = MarketingUsageOrderCriteria(MarketingUsageKind.FIRST_ORDER_REDUCE, 5L)
      every { orderQueryApiService.countOrdersByMarketingUsage(criteria) } returns 2L
      every { orderQueryApiService.sumMarketingDiscountCents(criteria) } returns 2000L
      every { orderQueryApiService.findOrdersByMarketingUsage(criteria, 0, 20) } returns orderPage(1L, 2L)

      val result = service.firstOrderActivityUsage(5L, 0, 20)

      result.summary.orderCount shouldBe 2L
      result.summary.benefitCents shouldBe 2000L
      result.summary.usageCount shouldBe 2L
    }

    test("firstOrderActivityUsage NEWCOMER_PRICE matches items by product+newcomer price+window") {
      val activity =
        FirstOrderActivityModel(
          id = 6L,
          activityType = FirstOrderActivityType.NEWCOMER_PRICE,
          productId = 7L,
          newcomerPriceCents = 1200L,
          validFrom = from,
          validTo = to,
        )
      every { firstOrderActivityRepository.findById(6L) } returns Optional.of(activity)
      val criteria =
        MarketingUsageOrderCriteria(
          kind = MarketingUsageKind.NEWCOMER_PRICE_ITEM,
          refId = 6L,
          productId = 7L,
          unitPriceCents = 1200L,
          validFrom = from,
          validTo = to,
        )
      every { orderQueryApiService.aggregateMarketingUsageItems(criteria) } returns
        listOf(MarketingUsageItemRow(productId = 7L, unitPriceCents = 1200L, quantitySum = 2L))
      every { orderQueryApiService.countOrdersByMarketingUsage(criteria) } returns 1L
      every { orderQueryApiService.findOrdersByMarketingUsage(criteria, 0, 20) } returns orderPage(1L)
      every { productQueryApiService.findById(7L) } returns ProductModel(price = 2000L)

      val result = service.firstOrderActivityUsage(6L, 0, 20)

      result.summary.orderCount shouldBe 1L
      result.summary.usageCount shouldBe 2L
      result.summary.benefitCents shouldBe 2L * (2000L - 1200L)
    }

    test("firstOrderActivityUsage returns empty when activity missing") {
      every { firstOrderActivityRepository.findById(99L) } returns Optional.empty()

      val result = service.firstOrderActivityUsage(99L, 0, 20)

      result.summary.orderCount shouldBe 0L
      result.orders.content shouldBe emptyList()
    }
  })
