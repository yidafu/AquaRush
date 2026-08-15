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
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 促销引擎单元测试(PRD 10-marketing.md §R-MKT-02/04/07/08):
 * 单价解析 → 水票核销(现金小计剔除)→ 满减/折扣(多活动取最大)→ 优惠券(最优组合,用户可改)→ 封顶。
 */
class PromotionEngineImplTest {
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var marketingPricingQueryApiService: MarketingPricingQueryApiService
  private lateinit var ticketQueryApiService: TicketQueryApiService
  private lateinit var userTicketRepository: UserTicketRepository
  private lateinit var ticketTypeRepository: TicketTypeRepository
  private lateinit var promotionActivityRepository: PromotionActivityRepository
  private lateinit var promotionActivityTierRepository: PromotionActivityTierRepository
  private lateinit var firstOrderActivityRepository: FirstOrderActivityRepository
  private lateinit var giftActivityRepository: GiftActivityRepository
  private lateinit var orderQueryApiService: OrderQueryApiService
  private lateinit var couponQueryApiService: CouponQueryApiService
  private lateinit var userCouponRepository: UserCouponRepository
  private lateinit var engine: PromotionEngineImpl

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
    productQueryApiService = mockk()
    marketingPricingQueryApiService = mockk()
    ticketQueryApiService = mockk()
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

  private fun product(
    id: Long = 1L,
    price: Long = 2100,
    specification: String = "19L",
  ): ProductModel =
    ProductModel(
      id = id,
      name = "测试桶装水",
      price = price,
      originalPrice = null,
      depositPrice = null,
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
      deliverySettings = null,
    )

  private fun ticket(
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

  private fun ticketType(
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

  private fun coupon(
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

  private fun activity(
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

  private fun request(
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

  /** 多商品请求(F-ORD-004):商品1×q1 + 商品2×q2 */
  private fun multiRequest(
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

  /** 商品独立计价 stub */
  private fun stubCatalogPriceFor(
    productId: Long,
    unitPrice: Long,
  ) {
    every { productQueryApiService.findById(productId) } returns product(id = productId, price = unitPrice)
    every { marketingPricingQueryApiService.resolveUnitPrice(1L, productId) } returns
      PriceResolution(unitPrice, PriceSource.CATALOG_PRICE, false)
    every { marketingPricingQueryApiService.findGroupId(1L) } returns null
  }

  private fun stubCatalogPrice() {
    every { productQueryApiService.findById(1L) } returns product()
    every { marketingPricingQueryApiService.resolveUnitPrice(1L, 1L) } returns
      PriceResolution(2100, PriceSource.CATALOG_PRICE, false)
    every { marketingPricingQueryApiService.findGroupId(1L) } returns null
  }

  private fun stubNoTickets() {
    every { ticketQueryApiService.findUsableTickets(1L) } returns emptyList()
  }

  private fun stubNoPromotions() {
    every { promotionActivityRepository.findActiveActivities(PromotionActivityStatus.ACTIVE, any()) } returns emptyList()
  }

  private fun stubNoCoupons() {
    every { couponQueryApiService.findUsableCoupons(1L) } returns emptyList()
  }

  @Test
  @DisplayName("无任何营销配置:单价=目录价,无优惠(R-MKT-02/07,AC-ORD-023-10)")
  fun `plain order uses catalog price`() {
    stubCatalogPrice()
    stubNoTickets()
    stubNoPromotions()
    stubNoCoupons()

    val result = engine.preview(request(quantity = 5))

    assertEquals(2100, result.unitPriceCents)
    assertEquals(PriceSource.CATALOG_PRICE, result.priceSource)
    assertEquals(10500, result.subtotalCents)
    assertEquals(10500, result.cashSubtotalCents)
    assertEquals(10500, result.payableProductCents)
    assertEquals(0, result.totalDiscountCents)
  }

  @Test
  @DisplayName("熟客分组价自动应用(AC-ORD-023-01):分组价 1500 优先于目录价 2100")
  fun `group price applies`() {
    every { productQueryApiService.findById(1L) } returns product()
    every { marketingPricingQueryApiService.resolveUnitPrice(1L, 1L) } returns
      PriceResolution(1500, PriceSource.GROUP_PRICE, true)
    every { marketingPricingQueryApiService.findGroupId(1L) } returns 1L
    stubNoTickets()
    stubNoPromotions()
    stubNoCoupons()

    val result = engine.preview(request(quantity = 5))

    assertEquals(1500, result.unitPriceCents)
    assertEquals(PriceSource.GROUP_PRICE, result.priceSource)
    assertEquals(7500, result.payableProductCents)
  }

  @Test
  @DisplayName("水票核销:5 桶核销 3 张,现金小计按 2 桶计(R-MKT-08,AC-MKT-007-03)")
  fun `ticket redemption covers barrels at zero price`() {
    every { productQueryApiService.findById(1L) } returns product()
    stubCatalogPrice()
    every { ticketQueryApiService.findUsableTickets(1L) } returns
      listOf(ticket(1, 1), ticket(2, 1), ticket(3, 1))
    every { ticketTypeRepository.findById(1L) } returns
      java.util.Optional.of(ticketType(1, TicketTypeKind.UNIVERSAL, specification = "19L"))
    stubNoPromotions()
    stubNoCoupons()

    val result = engine.preview(request(quantity = 5, ticketRedeemCount = 3))

    assertEquals(3, result.ticketRedeemCount)
    assertEquals(3, result.ticketCoveredQuantity)
    assertEquals(2 * 2100, result.cashSubtotalCents)
    assertEquals(2 * 2100, result.payableProductCents)
  }

  @Test
  @DisplayName("指定商品票仅可核销指定商品(AC-MKT-007-07)")
  fun `specific product ticket only covers matching product`() {
    stubCatalogPrice()
    every { ticketQueryApiService.findUsableTickets(1L) } returns
      listOf(ticket(1, 1), ticket(2, 1))
    every { ticketTypeRepository.findById(1L) } returns
      java.util.Optional.of(ticketType(1, TicketTypeKind.SPECIFIC_PRODUCT, productId = 999L))
    stubNoPromotions()
    stubNoCoupons()

    val result = engine.preview(request(quantity = 2, ticketRedeemCount = 2))

    assertEquals(0, result.ticketRedeemCount)
    assertEquals(2 * 2100, result.cashSubtotalCents)
  }

  @Test
  @DisplayName("通兑票跨规格不通用(AC-MKT-007-09):票限定 19L,商品 5L 不可核销")
  fun `universal ticket spec mismatch not redeemable`() {
    stubCatalogPrice()
    every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(ticket(1, 1))
    every { ticketTypeRepository.findById(1L) } returns
      java.util.Optional.of(ticketType(1, TicketTypeKind.UNIVERSAL, specification = "19L"))
    stubNoPromotions()
    stubNoCoupons()
    val fiveLiter = product(specification = "5L")
    every { productQueryApiService.findById(1L) } returns fiveLiter

    val result = engine.preview(request(quantity = 1, ticketRedeemCount = 1))

    assertEquals(0, result.ticketRedeemCount)
  }

  @Test
  @DisplayName("满减多档取最高已达标档,跨活动取优惠最大者不叠加(AC-MKT-004-04)")
  fun `promotion picks highest tier and max across activities`() {
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
    assertEquals(2500, result.promotionDiscountCents)
    assertEquals(1L, result.promotionActivityId)
    assertEquals("活动1", result.promotionActivityName)
    assertEquals(21000 - 2500, result.payableProductCents)
  }

  @Test
  @DisplayName("优惠券自动选最优,用户可改选/取消(R-MKT-03/07,AC-MKT-003-03)")
  fun `coupon best selection with override`() {
    stubCatalogPrice()
    stubNoTickets()
    stubNoPromotions()
    // 现金小计 10500(满50可用):券1 减10(1000),券2 减 5(500) → 自动选券1
    every { couponQueryApiService.findUsableCoupons(1L) } returns
      listOf(coupon(1, amount = 1000, threshold = 5000), coupon(2, amount = 500, threshold = 5000))

    val auto = engine.preview(request(quantity = 5))
    assertEquals(1L, auto.selectedCouponId)
    assertEquals(1000, auto.couponDiscountCents)
    assertEquals(10500 - 1000, auto.payableProductCents)
    assertTrue(auto.couponOptions.first { it.couponId == 1L }.isBest)

    val override = engine.preview(request(quantity = 5, preferredCouponId = 2L))
    assertEquals(2L, override.selectedCouponId)
    assertEquals(500, override.couponDiscountCents)

    val none = engine.preview(request(quantity = 5, preferredCouponId = 0L))
    assertNull(none.selectedCouponId)
    assertEquals(0, none.couponDiscountCents)
  }

  @Test
  @DisplayName("券门槛按现金小计判定,门槛不足不可用(AC-MKT-003-04)")
  fun `coupon threshold gates usability`() {
    stubCatalogPrice()
    stubNoTickets()
    stubNoPromotions()
    every { couponQueryApiService.findUsableCoupons(1L) } returns
      listOf(coupon(1, amount = 1000, threshold = 20000))

    val result = engine.preview(request(quantity = 5)) // 小计 10500 < 20000

    assertNull(result.selectedCouponId)
    assertEquals(0, result.couponDiscountCents)
    assertFalse(result.couponOptions.first().usable)
  }

  @Test
  @DisplayName("叠加与封顶:满减+券顺序应用,优惠不超应付(R-MKT-07)")
  fun `stacking and cap`() {
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
    assertEquals(2100, result.promotionDiscountCents)
    assertEquals(0, result.couponDiscountCents)
    assertEquals(0, result.payableProductCents)
  }

  @Test
  @DisplayName("下单消费:水票/优惠券同事务原子核销,核销不足回滚(BadRequestException)")
  fun `applyForOrder consumes and rolls back on shortage`() {
    stubCatalogPrice()
    stubNoPromotions()
    stubNoCoupons()
    // 预检 3 张可用,但消费时被并发占用(原子更新受影响行数=0)
    every { ticketQueryApiService.findUsableTickets(1L) } returns
      listOf(ticket(1, 1), ticket(2, 1), ticket(3, 1))
    every { ticketTypeRepository.findById(1L) } returns
      java.util.Optional.of(ticketType(1, TicketTypeKind.UNIVERSAL, specification = "19L"))
    every { userTicketRepository.redeemAtomically(any(), any(), any(), any()) } returns 0

    assertFailsWith<BadRequestException> {
      engine.applyForOrder(orderId = 99L, request(quantity = 5, ticketRedeemCount = 3))
    }
  }

  // ============ 限时特价(F-MKT-005,R-MKT-02③) ============

  private fun stubFlashSalePrice() {
    every { productQueryApiService.findById(1L) } returns product()
    every { marketingPricingQueryApiService.resolveUnitPrice(1L, 1L) } returns
      PriceResolution(1500, PriceSource.FLASH_SALE_PRICE, false, hasFlashSale = true)
    every { marketingPricingQueryApiService.findGroupId(1L) } returns null
  }

  @Test
  @DisplayName("限时特价:特价商品按特价计价,不参与满减/首单立减(R-MKT-07 边界,AC-MKT-005-03)")
  fun `flash sale price skips order level discounts`() {
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

    assertEquals(1500, result.unitPriceCents)
    assertEquals(PriceSource.FLASH_SALE_PRICE, result.priceSource)
    // 特价商品不参与叠加①(避免双重让利)
    assertEquals(0, result.promotionDiscountCents)
    assertEquals(0, result.firstOrderDiscountCents)
    assertEquals(1500, result.payableProductCents)
  }

  // ============ 买赠/满赠(F-MKT-008,R-MKT-09) ============

  private fun giftActivity(
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

  @Test
  @DisplayName("满赠:现金小计达标自动出赠品(0 价,不影响应付)(AC-MKT-008-02/03)")
  fun `full gift adds zero price gift line`() {
    stubCatalogPrice()
    stubNoTickets()
    stubNoPromotions()
    stubNoCoupons()
    every { productQueryApiService.findById(2L) } returns product(id = 2L, price = 1200)
    every { giftActivityRepository.findActiveActivities(any(), any()) } returns
      listOf(giftActivity(1, thresholdCents = 10000, scope = PromotionScope.PRODUCT, productId = 1L))

    // 小计 10500 ≥ 10000:命中赠品(0 价行,应付不变)
    val hit = engine.preview(request(quantity = 5))
    assertEquals(1, hit.giftItems.size)
    assertEquals(2L, hit.giftItems[0].productId)
    assertEquals("测试桶装水", hit.giftItems[0].productName)
    assertEquals(1, hit.giftItems[0].quantity)
    assertEquals(1L, hit.giftItems[0].giftActivityId)
    assertEquals(10500, hit.payableProductCents)

    // 小计 4200 < 10000:未达门槛,无赠品
    val miss = engine.preview(request(quantity = 2))
    assertTrue(miss.giftItems.isEmpty())
  }

  @Test
  @DisplayName("买赠:指定商品现金购买 6 桶赠 1 桶(AC-MKT-008-04)")
  fun `buy gift triggers on cash quantity`() {
    stubCatalogPrice()
    stubNoTickets()
    stubNoPromotions()
    stubNoCoupons()
    every { productQueryApiService.findById(2L) } returns product(id = 2L, price = 1200)
    every { giftActivityRepository.findActiveActivities(any(), any()) } returns
      listOf(giftActivity(1, type = GiftActivityType.BUY_GIFT, thresholdCents = null, thresholdQuantity = 5))

    val result = engine.preview(request(quantity = 6))

    assertEquals(1, result.giftItems.size)
    assertEquals(6 * 2100, result.payableProductCents)
  }

  @Test
  @DisplayName("买赠:按现金购买桶数判定(水票覆盖桶不计)(AC-MKT-008-04 边界)")
  fun `buy gift counts cash quantity only`() {
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

    assertEquals(2, result.ticketCoveredQuantity)
    assertTrue(result.giftItems.isEmpty())
  }

  // ============ 首单立减/新人专享价(F-MKT-009,R-MKT-10) ============

  private fun firstOrderActivity(
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

  @Test
  @DisplayName("首单立减:新用户首单自动减,老用户无优惠(AC-MKT-009-02/03/05)")
  fun `first order reduce applies to new user only`() {
    stubCatalogPrice()
    stubNoTickets()
    stubNoPromotions()
    stubNoCoupons()
    every { firstOrderActivityRepository.findActiveActivities(any(), any()) } returns
      listOf(firstOrderActivity(1, thresholdCents = 5000, discountAmountCents = 1000))

    // 新用户(无已完成订单):首单立减 1000
    val newUser = engine.preview(request(quantity = 5)) // 小计 10500
    assertEquals(1000, newUser.firstOrderDiscountCents)
    assertEquals(1L, newUser.firstOrderActivityId)
    assertEquals(9500, newUser.payableProductCents)

    // 老用户(已有完成订单):首单完成即失效,无优惠
    every { orderQueryApiService.hasCompletedOrders(1L) } returns true
    val oldUser = engine.preview(request(quantity = 5))
    assertEquals(0, oldUser.firstOrderDiscountCents)
    assertNull(oldUser.firstOrderActivityId)
    assertEquals(10500, oldUser.payableProductCents)
  }

  @Test
  @DisplayName("首单立减与满减互斥取最大(AC-MKT-009-06)")
  fun `first order reduce and promotion take max`() {
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
    assertEquals(0, result.promotionDiscountCents)
    assertEquals(1000, result.firstOrderDiscountCents)
    assertEquals(9500, result.payableProductCents)

    // 满减 2000 > 首单 1000:满减胜出,首单清零
    every { promotionActivityTierRepository.findByActivityIdOrderBySortAsc(1L) } returns
      listOf(tier(1000, amount = 2000))
    val result2 = engine.preview(request(quantity = 5))
    assertEquals(2000, result2.promotionDiscountCents)
    assertEquals(0, result2.firstOrderDiscountCents)
    assertNull(result2.firstOrderActivityId)
    assertEquals(8500, result2.payableProductCents)
  }

  @Test
  @DisplayName("首单立减:满折型按现金小计折算(9000=9折)")
  fun `first order reduce with rate`() {
    stubCatalogPrice()
    stubNoTickets()
    stubNoPromotions()
    stubNoCoupons()
    every { firstOrderActivityRepository.findActiveActivities(any(), any()) } returns
      listOf(firstOrderActivity(1, thresholdCents = 5000, discountAmountCents = null, discountRatePermille = 9000))

    val result = engine.preview(request(quantity = 5)) // 小计 10500

    assertEquals(10500 * (10000 - 9000) / 10000, result.firstOrderDiscountCents) // 1050
    assertEquals(9450, result.payableProductCents)
  }

  // ============ 多商品下单(F-ORD-004) ============

  @Test
  @DisplayName("多商品:单价逐商品解析,聚合小计=Σ金额(AC-ORD-004-02)")
  fun `multi item resolves unit price per item`() {
    stubCatalogPriceFor(1L, 2100)
    stubCatalogPriceFor(2L, 1500)
    stubNoTickets()
    stubNoPromotions()
    stubNoCoupons()

    val result = engine.preview(multiRequest(q1 = 2, q2 = 3))

    assertEquals(2, result.items.size)
    assertEquals(2100, result.items[0].unitPriceCents)
    assertEquals(1500, result.items[1].unitPriceCents)
    assertEquals(4200, result.items[0].subtotalCents)
    assertEquals(4500, result.items[1].subtotalCents)
    assertEquals(4200 + 4500, result.subtotalCents)
    assertEquals(8700, result.cashSubtotalCents)
    assertEquals(8700, result.payableProductCents)
    // 聚合展示:首件成交单价
    assertEquals(2100, result.unitPriceCents)
    assertEquals(PriceSource.CATALOG_PRICE, result.priceSource)
  }

  @Test
  @DisplayName("多商品:满减按聚合现金小计判定,PRODUCT 范围命中任一商品")
  fun `multi item promotion uses aggregate subtotal`() {
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

    assertEquals(1000, result.promotionDiscountCents)
    assertEquals(8700 - 1000, result.payableProductCents)
  }

  @Test
  @DisplayName("多商品:水票跨商品核销,上限=Σ数量,按 items 顺序覆盖")
  fun `multi item tickets redeem across items`() {
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

    assertEquals(4, result.ticketRedeemCount)
    assertEquals(4, result.ticketCoveredQuantity)
    assertEquals(2, result.items[0].ticketCoveredQuantity)
    assertEquals(2, result.items[1].ticketCoveredQuantity)
    assertEquals(0, result.items[0].cashSubtotalCents)
    assertEquals(1500, result.items[1].cashSubtotalCents)
    assertEquals(1500, result.cashSubtotalCents)
    assertEquals(1500, result.payableProductCents)
  }

  @Test
  @DisplayName("多商品:PRODUCT 范围券命中任一商品,门槛按聚合现金小计")
  fun `multi item product coupon matches any item`() {
    stubCatalogPriceFor(1L, 2100)
    stubCatalogPriceFor(2L, 1500)
    stubNoTickets()
    stubNoPromotions()
    // 指定商品2的券:聚合小计 8700 ≥ 5000 可用,减 1000
    every { couponQueryApiService.findUsableCoupons(1L) } returns
      listOf(coupon(1, amount = 1000, threshold = 5000, scope = CouponScope.PRODUCT, productId = 2L))

    val result = engine.preview(multiRequest(q1 = 2, q2 = 3))

    assertEquals(1L, result.selectedCouponId)
    assertEquals(1000, result.couponDiscountCents)
    assertEquals(7700, result.payableProductCents)
  }

  @Test
  @DisplayName("多商品:任一商品为限时特价 → 跳过订单层叠加①")
  fun `multi item flash sale skips order level discounts`() {
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

    assertEquals(0, result.promotionDiscountCents)
    assertEquals(0, result.firstOrderDiscountCents)
  }

  @Test
  @DisplayName("多商品:重复商品拒绝(购物车按 productId 合并,后端兜底)")
  fun `multi item rejects duplicate product`() {
    stubCatalogPriceFor(1L, 2100)

    assertFailsWith<BadRequestException> {
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

  @Test
  @DisplayName("多商品:商品不存在 → NotFoundException")
  fun `multi item rejects missing product`() {
    stubCatalogPriceFor(1L, 2100)
    every { productQueryApiService.findById(999L) } returns null

    assertFailsWith<dev.yidafu.aqua.common.exception.NotFoundException> {
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
}
