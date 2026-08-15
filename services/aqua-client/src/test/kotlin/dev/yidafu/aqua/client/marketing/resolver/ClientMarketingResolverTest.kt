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

package dev.yidafu.aqua.client.marketing.resolver

import dev.yidafu.aqua.api.dto.ClaimCouponRequest
import dev.yidafu.aqua.api.dto.OrderPricingItemResult
import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.api.dto.PurchaseTicketsRequest
import dev.yidafu.aqua.api.service.marketing.CouponMutationApiService
import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.api.service.marketing.MarketingPricingQueryApiService
import dev.yidafu.aqua.api.service.marketing.PriceResolution
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.api.service.marketing.TicketTypeQueryService
import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.CouponIssuanceType
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.common.graphql.generated.OrderItemInput
import dev.yidafu.aqua.common.graphql.generated.OrderPreviewInput
import dev.yidafu.aqua.common.graphql.generated.PurchaseTicketsInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import dev.yidafu.aqua.common.graphql.generated.UserCouponStatus as GraphQLUserCouponStatus
import dev.yidafu.aqua.common.graphql.generated.UserTicketStatus as GraphQLUserTicketStatus

/**
 * 用户端营销 Resolver 单元测试(F-MKT-003/007、R-MKT-01/02/07/08):
 * 领券中心/我的券包、水票商城/我的水票、购票(支付桩→requiresSimulation)、计价预览、商品价解析。
 */
class ClientMarketingResolverTest {
  private lateinit var couponQueryApiService: CouponQueryApiService
  private lateinit var couponMutationApiService: CouponMutationApiService
  private lateinit var ticketQueryApiService: TicketQueryApiService
  private lateinit var ticketMutationApiService: TicketMutationApiService
  private lateinit var ticketTypeQueryService: TicketTypeQueryService
  private lateinit var marketingPricingQueryApiService: MarketingPricingQueryApiService
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var orderMutationService: OrderMutationService
  private lateinit var paymentService: PaymentService
  private lateinit var userQueryApiService: UserQueryApiService
  private lateinit var addressQueryApiService: AddressQueryApiService
  private lateinit var deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService
  private lateinit var principal: UserPrincipal

  @BeforeEach
  fun setUp() {
    couponQueryApiService = mockk()
    couponMutationApiService = mockk()
    ticketQueryApiService = mockk()
    ticketMutationApiService = mockk()
    ticketTypeQueryService = mockk()
    marketingPricingQueryApiService = mockk()
    productQueryApiService = mockk()
    orderMutationService = mockk()
    paymentService = mockk()
    userQueryApiService = mockk()
    addressQueryApiService = mockk()
    deliveryFeeRuleQueryApiService = mockk()
    principal =
      UserPrincipal(
        id = 1L,
        _username = "testuser",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
      )
  }

  private fun couponTemplate(): CouponTemplateModel =
    CouponTemplateModel(
      id = 10L,
      name = "满50减10",
      couponType = CouponType.CASH,
      thresholdCents = 5000,
      discountAmountCents = 1000,
      discountRatePermille = null,
      scope = CouponScope.ALL,
      productId = null,
      validityType = ValidityType.FIXED_RANGE,
      validFrom = LocalDateTime.now().minusDays(1),
      validTo = LocalDateTime.now().plusDays(30),
      validDays = null,
      issuanceType = CouponIssuanceType.AUTO_CLAIM,
      perUserLimit = 1,
      status = CouponTemplateStatus.ACTIVE,
    )

  private fun userCoupon(): UserCouponModel =
    UserCouponModel(
      id = 1L,
      userId = 1L,
      couponTemplateId = 10L,
      status = UserCouponStatus.RECEIVED,
      name = "满50减10",
      couponType = CouponType.CASH,
      thresholdCents = 5000,
      discountAmountCents = 1000,
      discountRatePermille = null,
      scope = CouponScope.ALL,
      productId = null,
      expireTime = LocalDateTime.now().plusDays(30),
    )

  private fun ticketType(): TicketTypeModel =
    TicketTypeModel(
      id = 20L,
      name = "通兑19L",
      ticketType = TicketTypeKind.UNIVERSAL,
      productId = null,
      specification = "19L",
      packageCount = 10,
      packagePriceCents = 10000,
      giftCount = 0,
      validityType = ValidityType.FIXED_RANGE,
      validFrom = LocalDateTime.now().minusDays(1),
      validTo = LocalDateTime.now().plusDays(365),
      perUserLimit = 0,
      status = TicketTypeStatus.ACTIVE,
    )

  private fun userTicket(): UserTicketModel =
    UserTicketModel(
      id = 1L,
      userId = 1L,
      ticketTypeId = 20L,
      purchaseId = 1L,
      status = UserTicketStatus.UNUSED,
      expireTime = LocalDateTime.now().plusDays(30),
      isGift = false,
    )

  private fun product(): ProductModel =
    ProductModel(
      id = 1L,
      name = "桶装水19L",
      price = 2100,
      originalPrice = null,
      depositPrice = null,
      coverImageUrl = "http://x/cover.png",
      imageGallery = null,
      specification = "19L",
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

  private fun address(): AddressModel =
    AddressModel(
      id = 1L,
      userId = 1L,
      receiverName = "张三",
      phone = "13800138000",
      province = "广东省",
      city = "深圳市",
      district = "南山区",
      detailAddress = "测试地址1号",
      floor = 5,
    )

  @Test
  @DisplayName("领券中心:仅自动领取模板,含已领数量(AC-MKT-003-01/02)")
  fun `couponCenter returns claimable templates`() {
    every { couponQueryApiService.findClaimableTemplates(1L) } returns listOf(couponTemplate())
    every { couponQueryApiService.countUserCouponsByTemplate(1L, 10L) } returns 1L
    val resolver = ClientCouponQueryResolver(couponQueryApiService)

    val result = resolver.couponCenter(principal)

    assertEquals(1, result.size)
    assertEquals("满50减10", result[0].name)
    assertEquals(1, result[0].claimedCount)
  }

  @Test
  @DisplayName("我的券包:按状态过滤返回(AC-MKT-003-06)")
  fun `myCoupons filters by status`() {
    every { couponQueryApiService.findUserCoupons(1L, UserCouponStatus.RECEIVED) } returns listOf(userCoupon())
    val resolver = ClientCouponQueryResolver(couponQueryApiService)

    val result = resolver.myCoupons(GraphQLUserCouponStatus.RECEIVED, principal)

    assertEquals(1, result.size)
    assertEquals(UserCouponStatus.RECEIVED, UserCouponStatus.valueOf(result[0].status.name))
    assertFalse(result[0].usable)
  }

  @Test
  @DisplayName("领取优惠券(AC-MKT-003-02)")
  fun `claimCoupon claims`() {
    every { couponMutationApiService.claimCoupon(1L, ClaimCouponRequest(10L)) } returns userCoupon()
    val resolver = ClientCouponMutationResolver(couponMutationApiService)

    val result = resolver.claimCoupon(10L, principal)

    assertEquals(1L, result.id)
    assertEquals("满50减10", result.name)
  }

  @Test
  @DisplayName("水票商城:含已购张数(AC-MKT-007-01)")
  fun `ticketShop returns active types`() {
    every { ticketQueryApiService.findActiveTicketTypes() } returns listOf(ticketType())
    every { ticketQueryApiService.countPurchasedByTicketType(1L, 20L) } returns 3
    val resolver = ClientTicketQueryResolver(ticketQueryApiService, ticketTypeQueryService)

    val result = resolver.ticketShop(principal)

    assertEquals(1, result.size)
    assertEquals(10, result[0].packageCount)
    assertEquals(3, result[0].myPurchasedCount)
  }

  @Test
  @DisplayName("我的水票:票种名称/规格联查(AC-MKT-007-05)")
  fun `myTickets resolves ticket type info`() {
    every { ticketQueryApiService.findUserTickets(1L, UserTicketStatus.UNUSED) } returns listOf(userTicket())
    every { ticketTypeQueryService.getTicketType(20L) } returns ticketType()
    val resolver = ClientTicketQueryResolver(ticketQueryApiService, ticketTypeQueryService)

    val result = resolver.myTickets(GraphQLUserTicketStatus.UNUSED, principal)

    assertEquals(1, result.size)
    assertEquals("通兑19L", result[0].ticketTypeName)
    assertEquals("19L", result[0].specification)
  }

  @Test
  @DisplayName("购票:支付 SDK 桩 → requiresSimulation=true 前端提示待接入")
  fun `purchaseTickets marks simulation when payment stubbed`() {
    val purchase =
      TicketPurchaseModel(
        id = 1L,
        userId = 1L,
        ticketTypeId = 20L,
        orderNo = "TP201",
        packageCount = 10,
        giftCount = 0,
        totalCount = 10,
        packagePriceCents = 10000,
        paymentStatus = TicketPurchaseStatus.PENDING,
      )
    every { ticketMutationApiService.purchaseTickets(1L, PurchaseTicketsRequest(20L, 1)) } returns purchase
    every { ticketTypeQueryService.getTicketType(20L) } returns ticketType()
    every { userQueryApiService.findById(1L) } returns
      UserModel(id = 1L, wechatOpenId = "openid-1", email = "", lastLoginAt = LocalDateTime.now())
    every { paymentService.createWechatJsapiPayForTicket(1L, "TP201", 10000, any(), "openid-1") } throws NotImplementedError("stub")
    val resolver =
      ClientTicketMutationResolver(ticketMutationApiService, ticketTypeQueryService, userQueryApiService, paymentService, ObjectMapper())

    val result = resolver.purchaseTickets(PurchaseTicketsInput(packageCount = 1, ticketTypeId = 20L), principal)

    assertTrue(result.requiresSimulation)
    assertEquals(TicketPurchaseStatus.PENDING, TicketPurchaseStatus.valueOf(result.paymentStatus.name))
    assertEquals("通兑19L", result.ticketTypeName)
  }

  @Test
  @DisplayName("购票:假支付模式返回 mock 参数且已自动完成入账")
  fun purchaseTicketsAutoCompletedInMockMode() {
    val purchase =
      TicketPurchaseModel(
        id = 2L,
        userId = 1L,
        ticketTypeId = 20L,
        orderNo = "TP202",
        packageCount = 10,
        giftCount = 0,
        totalCount = 10,
        packagePriceCents = 10000,
        paymentStatus = TicketPurchaseStatus.PENDING,
      )
    every { ticketMutationApiService.purchaseTickets(1L, PurchaseTicketsRequest(20L, 1)) } returns purchase
    every { ticketTypeQueryService.getTicketType(20L) } returns ticketType()
    every { userQueryApiService.findById(1L) } returns
      UserModel(id = 1L, wechatOpenId = "openid-1", email = "", lastLoginAt = LocalDateTime.now())
    every { paymentService.createWechatJsapiPayForTicket(2L, "TP202", 10000, any(), "openid-1") } returns
      mapOf("mock" to true, "paid" to true, "transactionId" to "MOCK-TP202")
    val resolver =
      ClientTicketMutationResolver(ticketMutationApiService, ticketTypeQueryService, userQueryApiService, paymentService, ObjectMapper())

    val result = resolver.purchaseTickets(PurchaseTicketsInput(packageCount = 1, ticketTypeId = 20L), principal)

    assertFalse(result.requiresSimulation)
    assertNotNull(result.paymentParams)
    assertTrue(result.paymentParams!!.get("mock").asBoolean())
    assertEquals(TicketPurchaseStatus.PENDING, TicketPurchaseStatus.valueOf(result.paymentStatus.name))
  }

  @Test
  @DisplayName("商品价解析:无分组回退目录价,有分组价返回熟客价(R-MKT-01)")
  fun `productPrice resolves per user`() {
    every { marketingPricingQueryApiService.resolveUnitPrice(1L, 1L) } returns
      PriceResolution(1500, PriceSource.GROUP_PRICE, true)
    every { productQueryApiService.findById(1L) } returns product()
    val resolver = ClientProductPriceResolver(marketingPricingQueryApiService, productQueryApiService)

    val result = resolver.productPrice(1L, principal)

    assertNotNull(result)
    assertEquals(1500L, result.effectivePriceCents)
    assertTrue(result.hasGroupPrice)
    assertEquals(2100L, result.catalogPriceCents)
  }

  @Test
  @DisplayName("计价预览:委托订单域引擎,含配送费与实付合计(R-MKT-02/07)")
  fun `orderPreview computes breakdown`() {
    val pricing =
      OrderPricingResult(
        items =
          listOf(
            OrderPricingItemResult(
              productId = 1L,
              quantity = 2,
              unitPriceCents = 2100,
              priceSource = PriceSource.CATALOG_PRICE,
              catalogPriceCents = 2100,
              subtotalCents = 4200,
              ticketCoveredQuantity = 0,
              cashSubtotalCents = 4200,
            ),
          ),
        unitPriceCents = 2100,
        priceSource = PriceSource.CATALOG_PRICE,
        subtotalCents = 4200,
        ticketRedeemCount = 0,
        ticketCoveredQuantity = 0,
        cashSubtotalCents = 4200,
        promotionDiscountCents = 0,
        promotionActivityId = null,
        promotionActivityName = null,
        firstOrderDiscountCents = 0,
        firstOrderActivityId = null,
        firstOrderActivityName = null,
        couponDiscountCents = 0,
        selectedCouponId = null,
        selectedCouponName = null,
        totalDiscountCents = 0,
        payableProductCents = 4200,
        availableTicketCount = 0,
        couponOptions = emptyList(),
        giftItems = emptyList(),
      )
    every { orderMutationService.confirmOrderPreview(any()) } returns pricing
    every { addressQueryApiService.findById(1L) } returns address()
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)
    val resolver =
      ClientOrderPreviewResolver(orderMutationService, deliveryFeeRuleQueryApiService, addressQueryApiService, couponQueryApiService)

    val result =
      resolver.orderPreview(
        OrderPreviewInput(
          items = listOf(OrderItemInput(productId = 1L, quantity = 2)),
          addressId = 1L,
          preferredCouponId = null,
          ticketRedeemCount = 0,
        ),
        principal,
      )

    assertEquals(4200L, result.payableProductCents)
    assertEquals(200L, result.deliveryFeeCents)
    assertEquals(4400L, result.payAmountCents)
    verify { orderMutationService.confirmOrderPreview(any<OrderPricingRequest>()) }
  }

  @Test
  @DisplayName("计价预览:非本人地址拦截")
  fun `orderPreview rejects foreign address`() {
    val otherAddress = address().apply { userId = 999L }
    every { addressQueryApiService.findById(1L) } returns otherAddress
    val resolver =
      ClientOrderPreviewResolver(orderMutationService, deliveryFeeRuleQueryApiService, addressQueryApiService, couponQueryApiService)

    val result =
      kotlin.runCatching {
        resolver.orderPreview(
          OrderPreviewInput(
            items = listOf(OrderItemInput(productId = 1L, quantity = 2)),
            addressId = 1L,
            preferredCouponId = null,
            ticketRedeemCount = 0,
          ),
          principal,
        )
      }

    assertTrue(result.isFailure)
    assertNull(result.exceptionOrNull()?.message?.let { null })
  }
}
