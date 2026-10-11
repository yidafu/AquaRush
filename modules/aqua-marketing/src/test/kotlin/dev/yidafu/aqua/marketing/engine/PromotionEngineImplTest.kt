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

import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import dev.yidafu.aqua.api.service.marketing.PriceResolution
import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime

/**
 * 促销引擎单元测试(PRD 10-marketing.md §R-MKT-02/04/07/08):
 * 单价解析 → 水票核销(现金小计剔除)→ 满减/折扣(多活动取最大)→ 优惠券(最优组合,用户可改)
 * → 封顶。
 */
class PromotionEngineImplTest :
  FunSpec({
    var productQueryApiService: ProductQueryApiService = mockk()
    var marketingPricingQueryApiService: MarketingPricingQueryApiService = mockk()
    var ticketQueryApiService: TicketQueryApiService = mockk()
    var userTicketRepository: UserTicketRepository = mockk()
    var ticketTypeRepository: TicketTypeRepository = mockk()
    var promotionActivityRepository: PromotionActivityRepository = mockk()
    var promotionActivityTierRepository: PromotionActivityTierRepository = mockk()
    var firstOrderActivityRepository: FirstOrderActivityRepository = mockk()
    var giftActivityRepository: GiftActivityRepository = mockk()
    var orderQueryApiService: OrderQueryApiService = mockk()
    var couponQueryApiService: CouponQueryApiService = mockk()
    var userCouponRepository: UserCouponRepository = mockk()
    var engine: PromotionEngineImpl = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun product(
      id: Long = 1L,
      price: Long = 2100,
      specification: String = "19L",
    ): ProductModel =
      ProductModel(
        id = id,
        name = "测试桶装水",
        price = price,
        originalPrice = null,
        coverImageUrl = "http://x/cover.png",
        imageGallery = null,
        specification = specification,
        waterSource = "山泉水",
        mineralContent = null,
        stock = 100,
        salesVolume = 0,
        status = dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus.ONLINE,
        sortOrder = 1,
        tags = null,
        detailContent = null,
        certificateImages = null,
      )

    fun ticket(
      id: Long,
      typeId: Long,
      expireAt: LocalDateTime = now.plusDays(30),
    ): UserTicketModel =
      UserTicketModel(
        id = id,
        userId = 1L,
        ticketTypeId = typeId,
        purchaseId = 1L,
        status = UserTicketStatus.UNUSED,
        expireTime = expireAt,
        isGift = false,
      )

    fun ticketType(
      id: Long,
      kind: TicketTypeKind,
      productId: Long? = null,
      specification: String? = null,
    ): TicketTypeModel =
      TicketTypeModel(
        id = id,
        name = "票种$id",
        ticketType = kind,
        productId = productId,
        specification = specification,
        packageCount = 10,
        packagePriceCents = 10000,
        giftCount = 0,
        validityType = ValidityType.FIXED_RANGE,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(365),
        perUserLimit = 0,
        status = TicketTypeStatus.ACTIVE,
      )

    fun coupon(
      id: Long,
      type: CouponType = CouponType.CASH,
      amount: Long = 1000,
      threshold: Long? = 5000,
      scope: CouponScope = CouponScope.ALL,
      productId: Long? = null,
    ): UserCouponModel =
      UserCouponModel(
        id = id,
        userId = 1L,
        couponTemplateId = 100L + id,
        status = UserCouponStatus.RECEIVED,
        name = "测试券$id",
        couponType = type,
        thresholdCents = threshold,
        discountAmountCents = amount,
        discountRatePermille = null,
        scope = scope,
        productId = productId,
        expireTime = now.plusDays(30),
      )

    fun activity(
      id: Long,
      type: PromotionActivityType = PromotionActivityType.FULL_REDUCE,
      scope: PromotionScope = PromotionScope.ALL,
      productId: Long? = null,
      groupId: Long? = null,
    ): PromotionActivityModel =
      PromotionActivityModel(
        id = id,
        name = "活动$id",
        activityType = type,
        scope = scope,
        productId = productId,
        customerGroupId = groupId,
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

    fun request(
      quantity: Int = 5,
      preferredCouponId: Long? = null,
      ticketRedeemCount: Int = 0,
    ): OrderPricingRequest =
      OrderPricingRequest(
        userId = 1L,
        items =
          listOf(
            dev.yidafu.aqua.api.dto
              .OrderPricingItemRequest(productId = 1L, quantity = quantity),
          ),
        preferredCouponId = preferredCouponId,
        ticketRedeemCount = ticketRedeemCount,
      )

    fun multiRequest(
      q1: Int,
      q2: Int,
      ticketRedeemCount: Int = 0,
    ): OrderPricingRequest =
      OrderPricingRequest(
        userId = 1L,
        items =
          listOf(
            dev.yidafu.aqua.api.dto
              .OrderPricingItemRequest(productId = 1L, quantity = q1),
            dev.yidafu.aqua.api.dto
              .OrderPricingItemRequest(productId = 2L, quantity = q2),
          ),
        ticketRedeemCount = ticketRedeemCount,
      )

    fun stubNoTickets() {
      every { ticketQueryApiService.findUsableTickets(1L) } returns emptyList()
    }

    fun stubNoPromotions() {
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, any()) } returns emptyList()
    }

    fun stubNoCoupons() {
      every { couponQueryApiService.findUsableCoupons(1L) } returns emptyList()
    }

    fun giftActivity(
      id: Long,
      type: GiftActivityType = GiftActivityType.FULL_GIFT,
      thresholdCents: Long? = 10000,
      thresholdQuantity: Int? = null,
      scope: PromotionScope = PromotionScope.ALL,
      productId: Long? = null,
      groupId: Long? = null,
    ): GiftActivityModel =
      GiftActivityModel(
        id = id,
        name = "赠品活动$id",
        giftType = type,
        thresholdCents = thresholdCents,
        thresholdQuantity = thresholdQuantity,
        scope = scope,
        productId = productId,
        customerGroupId = groupId,
        giftProductId = 2L,
        giftQuantity = 1,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
        status = PromotionActivityStatus.ACTIVE,
      )

    fun firstOrderActivity(
      id: Long,
      thresholdCents: Long? = 5000,
      discountAmountCents: Long? = 1000,
      discountRatePermille: Long? = null,
      scope: PromotionScope = PromotionScope.ALL,
      productId: Long? = null,
    ): FirstOrderActivityModel =
      FirstOrderActivityModel(
        id = id,
        name = "首单活动$id",
        activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
        thresholdCents = thresholdCents,
        discountAmountCents = discountAmountCents,
        discountRatePermille = discountRatePermille,
        scope = scope,
        productId = productId,
        newcomerPriceCents = null,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
        status = PromotionActivityStatus.ACTIVE,
      )

    fun stubCatalogPriceFor(
      productId: Long,
      unitPrice: Long,
    ) {
      every { productQueryApiService.findById(productId) } returns product(id = productId, price = unitPrice)
      every { marketingPricingQueryApiService.resolveUnitPrice(1L, productId) } returns
        PriceResolution(unitPrice, PriceSource.CATALOG_PRICE, false)
      every { marketingPricingQueryApiService.findGroupId(1L) } returns null
    }

    fun stubCatalogPrice() {
      every { productQueryApiService.findById(1L) } returns product()
      every { marketingPricingQueryApiService.resolveUnitPrice(1L, 1L) } returns
        PriceResolution(2100, PriceSource.CATALOG_PRICE, false)
      every { marketingPricingQueryApiService.findGroupId(1L) } returns null
    }

    fun stubFlashSalePrice() {
      every { productQueryApiService.findById(1L) } returns product()
      every { marketingPricingQueryApiService.resolveUnitPrice(1L, 1L) } returns
        PriceResolution(1500, PriceSource.FLASH_SALE_PRICE, false, hasFlashSale = true)
      every { marketingPricingQueryApiService.findGroupId(1L) } returns null
    }

    // 多商品请求(F-ORD-004):商品1×q1 + 商品2×q2

    // 商品独立计价 stub

    // ============ 限时特价(F-MKT-005,R-MKT-02③) ============

    // ============ 买赠/满赠(F-MKT-008,R-MKT-09) ============

    // ============ 首单立减/新人专享价(F-MKT-009,R-MKT-10) ============

    // ============ 多商品下单(F-ORD-004) ============

    beforeTest {
      productQueryApiService = mockk()
      marketingPricingQueryApiService = mockk()
      ticketQueryApiService = mockk()
      // 默认无待激活票(G97 复购激活门禁;具体用例覆盖)
      every { ticketQueryApiService.findPendingActivationIds(any(), any()) } returns emptySet()
      userTicketRepository = mockk()
      ticketTypeRepository = mockk()
      promotionActivityRepository = mockk()
      promotionActivityTierRepository = mockk()
      firstOrderActivityRepository = mockk()
      giftActivityRepository = mockk()
      orderQueryApiService = mockk()
      couponQueryApiService = mockk()
      userCouponRepository = mockk()

      engine =
        PromotionEngineImpl(
          productQueryApiService,
          listOf(
            UnitPriceRule(marketingPricingQueryApiService),
            TicketRule(ticketQueryApiService, userTicketRepository, ticketTypeRepository),
            GiftRule(giftActivityRepository, marketingPricingQueryApiService, productQueryApiService),
            PromotionActivityRule(
              promotionActivityRepository,
              promotionActivityTierRepository,
              firstOrderActivityRepository,
              orderQueryApiService,
              marketingPricingQueryApiService,
            ),
            CouponRule(couponQueryApiService, userCouponRepository),
          ),
        )

      // 默认:非新用户首单资格、无首单立减/买赠活动(具体用例各自覆盖)
      every { orderQueryApiService.hasCompletedOrders(any()) } returns false
      every { firstOrderActivityRepository.findActiveActivities(any(), any()) } returns emptyList()
      every { giftActivityRepository.findActiveActivities(any(), any()) } returns emptyList()
    }
    test("plain order uses catalog price") {
      stubCatalogPrice()
      stubNoTickets()
      stubNoPromotions()
      stubNoCoupons()

      val result = engine.preview(request(quantity = 5))

      result.unitPriceCents shouldBe 2100
      result.priceSource shouldBe PriceSource.CATALOG_PRICE
      result.subtotalCents shouldBe 10500
      result.cashSubtotalCents shouldBe 10500
      result.payableProductCents shouldBe 10500
      result.totalDiscountCents shouldBe 0
    }
    test("group price applies") {
      every { productQueryApiService.findById(1L) } returns product()
      every { marketingPricingQueryApiService.resolveUnitPrice(1L, 1L) } returns
        PriceResolution(1500, PriceSource.GROUP_PRICE, true)
      every { marketingPricingQueryApiService.findGroupId(1L) } returns 1L
      stubNoTickets()
      stubNoPromotions()
      stubNoCoupons()

      val result = engine.preview(request(quantity = 5))

      result.unitPriceCents shouldBe 1500
      result.priceSource shouldBe PriceSource.GROUP_PRICE
      result.payableProductCents shouldBe 7500
    }
    test("ticket redemption covers barrels at zero price") {
      every { productQueryApiService.findById(1L) } returns product()
      stubCatalogPrice()
      every { ticketQueryApiService.findUsableTickets(1L) } returns
        listOf(ticket(1, 1), ticket(2, 1), ticket(3, 1))
      every { ticketTypeRepository.findById(1L) } returns
        java.util.Optional.of(ticketType(1, TicketTypeKind.UNIVERSAL, specification = "19L"))
      stubNoPromotions()
      stubNoCoupons()

      val result = engine.preview(request(quantity = 5, ticketRedeemCount = 3))

      result.ticketRedeemCount shouldBe 3
      result.ticketCoveredQuantity shouldBe 3
      result.cashSubtotalCents shouldBe 2 * 2100
      result.payableProductCents shouldBe 2 * 2100
    }
    test("specific product ticket only covers matching product") {
      stubCatalogPrice()
      every { ticketQueryApiService.findUsableTickets(1L) } returns
        listOf(ticket(1, 1), ticket(2, 1))
      every { ticketTypeRepository.findById(1L) } returns
        java.util.Optional.of(ticketType(1, TicketTypeKind.SPECIFIC_PRODUCT, productId = 999L))
      stubNoPromotions()
      stubNoCoupons()

      val result = engine.preview(request(quantity = 2, ticketRedeemCount = 2))

      result.ticketRedeemCount shouldBe 0
      result.cashSubtotalCents shouldBe 2 * 2100
    }
    test("universal ticket spec mismatch not redeemable") {
      stubCatalogPrice()
      every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(ticket(1, 1))
      every { ticketTypeRepository.findById(1L) } returns
        java.util.Optional.of(ticketType(1, TicketTypeKind.UNIVERSAL, specification = "19L"))
      stubNoPromotions()
      stubNoCoupons()
      val fiveLiter = product(specification = "5L")
      every { productQueryApiService.findById(1L) } returns fiveLiter

      val result = engine.preview(request(quantity = 1, ticketRedeemCount = 1))

      result.ticketRedeemCount shouldBe 0
    }
    test("promotion picks highest tier and max across activities") {
      stubCatalogPrice()
      stubNoTickets()
      // 活动1:满100减10 / 满200减25(现金小计 21000=210元 → 命中 200 档减 25)
      // 活动2:满 150 打 9 折(21000 → 减 2100)
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, any()) } returns
        listOf(activity(1), activity(2, type = PromotionActivityType.FULL_DISCOUNT))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
        listOf(tier(10000, amount = 1000), tier(20000, amount = 2500))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(2L) } returns
        listOf(tier(15000, permille = 9000))
      stubNoCoupons()

      val result = engine.preview(request(quantity = 10))

      // 活动2 折扣 2100 > 活动1 折扣 2500? 2500 > 2100 → 取活动1的 2500
      result.promotionDiscountCents shouldBe 2500
      result.promotionActivityId shouldBe 1L
      result.promotionActivityName shouldBe "活动1"
      result.payableProductCents shouldBe 21000 - 2500
    }
    test("coupon best selection with override") {
      stubCatalogPrice()
      stubNoTickets()
      stubNoPromotions()
      // 现金小计 10500(满50可用):券1 减10(1000),券2 减 5(500) → 自动选券1
      every { couponQueryApiService.findUsableCoupons(1L) } returns
        listOf(coupon(1, amount = 1000, threshold = 5000), coupon(2, amount = 500, threshold = 5000))

      val auto = engine.preview(request(quantity = 5))
      auto.selectedCouponId shouldBe 1L
      auto.couponDiscountCents shouldBe 1000
      auto.payableProductCents shouldBe 10500 - 1000
      auto.couponOptions.first { it.couponId == 1L }.isBest shouldBe true

      val override = engine.preview(request(quantity = 5, preferredCouponId = 2L))
      override.selectedCouponId shouldBe 2L
      override.couponDiscountCents shouldBe 500

      val none = engine.preview(request(quantity = 5, preferredCouponId = 0L))
      none.selectedCouponId.shouldBeNull()
      none.couponDiscountCents shouldBe 0
    }
    test("coupon threshold gates usability") {
      stubCatalogPrice()
      stubNoTickets()
      stubNoPromotions()
      every { couponQueryApiService.findUsableCoupons(1L) } returns
        listOf(coupon(1, amount = 1000, threshold = 20000))

      val result = engine.preview(request(quantity = 5)) // 小计 10500 < 20000

      result.selectedCouponId.shouldBeNull()
      result.couponDiscountCents shouldBe 0
      result.couponOptions.first().usable shouldBe false
    }
    test("stacking and cap") {
      stubCatalogPrice()
      stubNoTickets()
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, any()) } returns
        listOf(activity(1))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
        listOf(tier(1000, amount = 10000)) // 满10减100(小计 2100 达标)
      every { couponQueryApiService.findUsableCoupons(1L) } returns
        listOf(coupon(1, amount = 10000, threshold = 5000)) // 减100

      val result = engine.preview(request(quantity = 1)) // 小计 2100

      // 满减 10000 封顶到 2100;券折扣基于余额 0 → 0;应付 0
      result.promotionDiscountCents shouldBe 2100
      result.couponDiscountCents shouldBe 0
      result.payableProductCents shouldBe 0
    }
    test("applyForOrder consumes and rolls back on shortage") {
      stubCatalogPrice()
      stubNoPromotions()
      stubNoCoupons()
      // 预检 3 张可用,但消费时被并发占用(原子更新受影响行数=0)
      every { ticketQueryApiService.findUsableTickets(1L) } returns
        listOf(ticket(1, 1), ticket(2, 1), ticket(3, 1))
      every { ticketTypeRepository.findById(1L) } returns
        java.util.Optional.of(ticketType(1, TicketTypeKind.UNIVERSAL, specification = "19L"))
      every { userTicketRepository.redeemAtomically(any(), any(), any(), any()) } returns 0

      shouldThrow<BadRequestException> {
        engine.applyForOrder(orderId = 99L, request(quantity = 5, ticketRedeemCount = 3))
      }
    }
    test("flash sale price skips order level discounts") {
      stubFlashSalePrice()
      stubNoTickets()
      stubNoCoupons()
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, any()) } returns
        listOf(activity(1))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
        listOf(tier(1000, amount = 10000)) // 满10减100(小计 1500 达标)
      every { firstOrderActivityRepository.findActiveActivities(any(), any()) } returns
        listOf(firstOrderActivity(1, thresholdCents = 1000, discountAmountCents = 500))

      val result = engine.preview(request(quantity = 1))

      result.unitPriceCents shouldBe 1500
      result.priceSource shouldBe PriceSource.FLASH_SALE_PRICE
      // 特价商品不参与叠加①(避免双重让利)
      result.promotionDiscountCents shouldBe 0
      result.firstOrderDiscountCents shouldBe 0
      result.payableProductCents shouldBe 1500
    }
    test("full gift adds zero price gift line") {
      stubCatalogPrice()
      stubNoTickets()
      stubNoPromotions()
      stubNoCoupons()
      every { productQueryApiService.findById(2L) } returns product(id = 2L, price = 1200)
      every { giftActivityRepository.findActiveActivities(any(), any()) } returns
        listOf(giftActivity(1, thresholdCents = 10000, scope = PromotionScope.PRODUCT, productId = 1L))

      // 小计 10500 ≥ 10000:命中赠品(0 价行,应付不变)
      val hit = engine.preview(request(quantity = 5))
      hit.giftItems.size shouldBe 1
      hit.giftItems[0].productId shouldBe 2L
      hit.giftItems[0].productName shouldBe "测试桶装水"
      hit.giftItems[0].quantity shouldBe 1
      hit.giftItems[0].giftActivityId shouldBe 1L
      hit.payableProductCents shouldBe 10500

      // 小计 4200 < 10000:未达门槛,无赠品
      val miss = engine.preview(request(quantity = 2))
      miss.giftItems.isEmpty() shouldBe true
    }
    test("buy gift triggers on cash quantity") {
      stubCatalogPrice()
      stubNoTickets()
      stubNoPromotions()
      stubNoCoupons()
      every { productQueryApiService.findById(2L) } returns product(id = 2L, price = 1200)
      every { giftActivityRepository.findActiveActivities(any(), any()) } returns
        listOf(giftActivity(1, type = GiftActivityType.BUY_GIFT, thresholdCents = null, thresholdQuantity = 5))

      val result = engine.preview(request(quantity = 6))

      result.giftItems.size shouldBe 1
      result.payableProductCents shouldBe 6 * 2100
    }
    test("buy gift counts cash quantity only") {
      stubCatalogPrice()
      stubNoPromotions()
      stubNoCoupons()
      every { productQueryApiService.findById(2L) } returns product(id = 2L, price = 1200)
      every { giftActivityRepository.findActiveActivities(any(), any()) } returns
        listOf(giftActivity(1, type = GiftActivityType.BUY_GIFT, thresholdCents = null, thresholdQuantity = 5))
      // 5 桶下单核销 2 桶 → 现金购买 3 桶 < 5,不达门槛
      every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(ticket(1, 1), ticket(2, 1))
      every { ticketTypeRepository.findById(1L) } returns
        java.util.Optional.of(ticketType(1, TicketTypeKind.UNIVERSAL, specification = "19L"))

      val result = engine.preview(request(quantity = 5, ticketRedeemCount = 2))

      result.ticketCoveredQuantity shouldBe 2
      result.giftItems.isEmpty() shouldBe true
    }
    test("first order reduce applies to new user only") {
      stubCatalogPrice()
      stubNoTickets()
      stubNoPromotions()
      stubNoCoupons()
      every { firstOrderActivityRepository.findActiveActivities(any(), any()) } returns
        listOf(firstOrderActivity(1, thresholdCents = 5000, discountAmountCents = 1000))

      // 新用户(无已完成订单):首单立减 1000
      val newUser = engine.preview(request(quantity = 5)) // 小计 10500
      newUser.firstOrderDiscountCents shouldBe 1000
      newUser.firstOrderActivityId shouldBe 1L
      newUser.payableProductCents shouldBe 9500

      // 老用户(已有完成订单):首单完成即失效,无优惠
      every { orderQueryApiService.hasCompletedOrders(1L) } returns true
      val oldUser = engine.preview(request(quantity = 5))
      oldUser.firstOrderDiscountCents shouldBe 0
      oldUser.firstOrderActivityId.shouldBeNull()
      oldUser.payableProductCents shouldBe 10500
    }
    test("first order reduce and promotion take max") {
      stubCatalogPrice()
      stubNoTickets()
      stubNoCoupons()
      every { firstOrderActivityRepository.findActiveActivities(any(), any()) } returns
        listOf(firstOrderActivity(1, thresholdCents = 5000, discountAmountCents = 1000))

      // 满减 500 < 首单 1000:首单胜出
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, any()) } returns
        listOf(activity(1))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
        listOf(tier(1000, amount = 500))
      val result = engine.preview(request(quantity = 5)) // 小计 10500
      result.promotionDiscountCents shouldBe 0
      result.firstOrderDiscountCents shouldBe 1000
      result.payableProductCents shouldBe 9500

      // 满减 2000 > 首单 1000:满减胜出,首单清零
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
        listOf(tier(1000, amount = 2000))
      val result2 = engine.preview(request(quantity = 5))
      result2.promotionDiscountCents shouldBe 2000
      result2.firstOrderDiscountCents shouldBe 0
      result2.firstOrderActivityId.shouldBeNull()
      result2.payableProductCents shouldBe 8500
    }
    test("first order reduce with rate") {
      stubCatalogPrice()
      stubNoTickets()
      stubNoPromotions()
      stubNoCoupons()
      every { firstOrderActivityRepository.findActiveActivities(any(), any()) } returns
        listOf(firstOrderActivity(1, thresholdCents = 5000, discountAmountCents = null, discountRatePermille = 9000))

      val result = engine.preview(request(quantity = 5)) // 小计 10500

      result.firstOrderDiscountCents shouldBe 10500 * (10000 - 9000) / 10000 // 1050
      result.payableProductCents shouldBe 9450
    }
    test("multi item resolves unit price per item") {
      stubCatalogPriceFor(1L, 2100)
      stubCatalogPriceFor(2L, 1500)
      stubNoTickets()
      stubNoPromotions()
      stubNoCoupons()

      val result = engine.preview(multiRequest(q1 = 2, q2 = 3))

      result.items.size shouldBe 2
      result.items[0].unitPriceCents shouldBe 2100
      result.items[1].unitPriceCents shouldBe 1500
      result.items[0].subtotalCents shouldBe 4200
      result.items[1].subtotalCents shouldBe 4500
      result.subtotalCents shouldBe 4200 + 4500
      result.cashSubtotalCents shouldBe 8700
      result.payableProductCents shouldBe 8700
      // 聚合展示:首件成交单价
      result.unitPriceCents shouldBe 2100
      result.priceSource shouldBe PriceSource.CATALOG_PRICE
    }
    test("multi item promotion uses aggregate subtotal") {
      stubCatalogPriceFor(1L, 2100)
      stubCatalogPriceFor(2L, 1500)
      stubNoTickets()
      stubNoCoupons()
      // 指定商品活动命中商品2(聚合小计 8700 ≥ 5000 → 减 1000)
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, any()) } returns
        listOf(activity(1, scope = PromotionScope.PRODUCT, productId = 2L))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
        listOf(tier(5000, amount = 1000))

      val result = engine.preview(multiRequest(q1 = 2, q2 = 3))

      result.promotionDiscountCents shouldBe 1000
      result.payableProductCents shouldBe 8700 - 1000
    }
    test("multi item tickets redeem across items") {
      stubCatalogPriceFor(1L, 2100)
      stubCatalogPriceFor(2L, 1500)
      stubNoPromotions()
      stubNoCoupons()
      // 通兑票 4 张,规格 19L 匹配商品1
      every { ticketQueryApiService.findUsableTickets(1L) } returns
        listOf(ticket(1, 1), ticket(2, 1), ticket(3, 1), ticket(4, 1))
      every { ticketTypeRepository.findById(1L) } returns
        java.util.Optional.of(ticketType(1, TicketTypeKind.UNIVERSAL, specification = "19L"))

      // 商品1×2 + 商品2×3,核销 4 张 → 商品1 覆盖 2 桶,商品2 覆盖 2 桶
      val result = engine.preview(multiRequest(q1 = 2, q2 = 3, ticketRedeemCount = 4))

      result.ticketRedeemCount shouldBe 4
      result.ticketCoveredQuantity shouldBe 4
      result.items[0].ticketCoveredQuantity shouldBe 2
      result.items[1].ticketCoveredQuantity shouldBe 2
      result.items[0].cashSubtotalCents shouldBe 0
      result.items[1].cashSubtotalCents shouldBe 1500
      result.cashSubtotalCents shouldBe 1500
      result.payableProductCents shouldBe 1500
    }
    test("multi item product coupon matches any item") {
      stubCatalogPriceFor(1L, 2100)
      stubCatalogPriceFor(2L, 1500)
      stubNoTickets()
      stubNoPromotions()
      // 指定商品2的券:聚合小计 8700 ≥ 5000 可用,减 1000
      every { couponQueryApiService.findUsableCoupons(1L) } returns
        listOf(coupon(1, amount = 1000, threshold = 5000, scope = CouponScope.PRODUCT, productId = 2L))

      val result = engine.preview(multiRequest(q1 = 2, q2 = 3))

      result.selectedCouponId shouldBe 1L
      result.couponDiscountCents shouldBe 1000
      result.payableProductCents shouldBe 7700
    }
    test("multi item flash sale skips order level discounts") {
      stubCatalogPriceFor(1L, 2100)
      stubNoTickets()
      stubNoCoupons()
      // 商品2 特价 1200
      every { productQueryApiService.findById(2L) } returns product(id = 2L, price = 1500)
      every { marketingPricingQueryApiService.resolveUnitPrice(1L, 2L) } returns
        PriceResolution(1200, PriceSource.FLASH_SALE_PRICE, false, hasFlashSale = true)
      every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, any()) } returns
        listOf(activity(1))
      every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
        listOf(tier(1000, amount = 10000))

      val result = engine.preview(multiRequest(q1 = 2, q2 = 3))

      result.promotionDiscountCents shouldBe 0
      result.firstOrderDiscountCents shouldBe 0
    }
    test("multi item rejects duplicate product") {
      stubCatalogPriceFor(1L, 2100)

      shouldThrow<BadRequestException> {
        engine.preview(
          OrderPricingRequest(
            userId = 1L,
            items =
              listOf(
                dev.yidafu.aqua.api.dto
                  .OrderPricingItemRequest(productId = 1L, quantity = 1),
                dev.yidafu.aqua.api.dto
                  .OrderPricingItemRequest(productId = 1L, quantity = 2),
              ),
          ),
        )
      }
    }
    test("multi item rejects missing product") {
      stubCatalogPriceFor(1L, 2100)
      every { productQueryApiService.findById(999L) } returns null

      shouldThrow<dev.yidafu.aqua.common.exception.NotFoundException> {
        engine.preview(
          OrderPricingRequest(
            userId = 1L,
            items =
              listOf(
                dev.yidafu.aqua.api.dto
                  .OrderPricingItemRequest(productId = 1L, quantity = 1),
                dev.yidafu.aqua.api.dto
                  .OrderPricingItemRequest(productId = 999L, quantity = 1),
              ),
          ),
        )
      }
    }
  })
