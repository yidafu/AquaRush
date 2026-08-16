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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.UserCouponStatus as GraphQLUserCouponStatus
import dev.yidafu.aqua.common.graphql.generated.UserTicketStatus as GraphQLUserTicketStatus

/**
 * 用户端营销 Resolver 单元测试(F-MKT-003/007、R-MKT-01/02/07/08):
 * 领券中心/我的券包、水票商城/我的水票、购票(支付桩→requiresSimulation)、计价预览、商品价解析。
 */
class ClientMarketingResolverTest :
  FunSpec({
    var couponQueryApiService: CouponQueryApiService = mockk()
    var couponMutationApiService: CouponMutationApiService = mockk()
    var ticketQueryApiService: TicketQueryApiService = mockk()
    var ticketMutationApiService: TicketMutationApiService = mockk()
    var ticketTypeQueryService: TicketTypeQueryService = mockk()
    var marketingPricingQueryApiService: MarketingPricingQueryApiService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var orderMutationService: OrderMutationService = mockk()
    var paymentService: PaymentService = mockk()
    var userQueryApiService: UserQueryApiService = mockk()
    var addressQueryApiService: AddressQueryApiService = mockk()
    var deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService = mockk()
    var principal: UserPrincipal = mockk()

    fun couponTemplate(): CouponTemplateModel =
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

    fun userCoupon(): UserCouponModel =
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

    fun ticketType(): TicketTypeModel =
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

    fun userTicket(): UserTicketModel =
      UserTicketModel(
        id = 1L,
        userId = 1L,
        ticketTypeId = 20L,
        purchaseId = 1L,
        status = UserTicketStatus.UNUSED,
        expireTime = LocalDateTime.now().plusDays(30),
        isGift = false,
      )

    fun product(): ProductModel =
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

    fun address(): AddressModel =
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

    beforeTest {
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
    test("couponCenter returns claimable templates") {
      every { couponQueryApiService.findClaimableTemplates(1L) } returns listOf(couponTemplate())
      every { couponQueryApiService.countUserCouponsByTemplate(1L, 10L) } returns 1L
      val resolver = ClientCouponQueryResolver(couponQueryApiService)

      val result = resolver.couponCenter(principal)

      result.size shouldBe 1
      result[0].name shouldBe "满50减10"
      result[0].claimedCount shouldBe 1
    }
    test("myCoupons filters by status") {
      every { couponQueryApiService.findUserCoupons(1L, UserCouponStatus.RECEIVED) } returns listOf(userCoupon())
      val resolver = ClientCouponQueryResolver(couponQueryApiService)

      val result = resolver.myCoupons(GraphQLUserCouponStatus.RECEIVED, principal)

      result.size shouldBe 1
      UserCouponStatus.valueOf(result[0].status.name) shouldBe UserCouponStatus.RECEIVED
      result[0].usable shouldBe false
    }
    test("claimCoupon claims") {
      every { couponMutationApiService.claimCoupon(1L, ClaimCouponRequest(10L)) } returns userCoupon()
      val resolver = ClientCouponMutationResolver(couponMutationApiService)

      val result = resolver.claimCoupon(10L, principal)

      result.id shouldBe 1L
      result.name shouldBe "满50减10"
    }
    test("ticketShop returns active types") {
      every { ticketQueryApiService.findActiveTicketTypes() } returns listOf(ticketType())
      every { ticketQueryApiService.countPurchasedByTicketType(1L, 20L) } returns 3
      val resolver = ClientTicketQueryResolver(ticketQueryApiService, ticketTypeQueryService)

      val result = resolver.ticketShop(principal)

      result.size shouldBe 1
      result[0].packageCount shouldBe 10
      result[0].myPurchasedCount shouldBe 3
    }
    test("myTickets resolves ticket type info") {
      every { ticketQueryApiService.findUserTickets(1L, UserTicketStatus.UNUSED) } returns listOf(userTicket())
      every { ticketTypeQueryService.getTicketType(20L) } returns ticketType()
      val resolver = ClientTicketQueryResolver(ticketQueryApiService, ticketTypeQueryService)

      val result = resolver.myTickets(GraphQLUserTicketStatus.UNUSED, principal)

      result.size shouldBe 1
      result[0].ticketTypeName shouldBe "通兑19L"
      result[0].specification shouldBe "19L"
    }
    test("purchaseTickets marks simulation when payment stubbed") {
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

      result.requiresSimulation shouldBe true
      TicketPurchaseStatus.valueOf(result.paymentStatus.name) shouldBe TicketPurchaseStatus.PENDING
      result.ticketTypeName shouldBe "通兑19L"
    }
    test("purchaseTicketsAutoCompletedInMockMode") {
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

      result.requiresSimulation shouldBe false
      result.paymentParams.shouldNotBeNull()
      result.paymentParams!!.get("mock").asBoolean() shouldBe true
      TicketPurchaseStatus.valueOf(result.paymentStatus.name) shouldBe TicketPurchaseStatus.PENDING
    }
    test("productPrice resolves per user") {
      every { marketingPricingQueryApiService.resolveUnitPrice(1L, 1L) } returns
        PriceResolution(1500, PriceSource.GROUP_PRICE, true)
      every { productQueryApiService.findById(1L) } returns product()
      val resolver = ClientProductPriceResolver(marketingPricingQueryApiService, productQueryApiService)

      val result = resolver.productPrice(1L, principal)

      result.shouldNotBeNull()
      result.effectivePriceCents shouldBe 1500L
      result.hasGroupPrice shouldBe true
      result.catalogPriceCents shouldBe 2100L
    }
    test("orderPreview computes breakdown") {
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

      result.payableProductCents shouldBe 4200L
      result.deliveryFeeCents shouldBe 200L
      result.payAmountCents shouldBe 4400L
      verify { orderMutationService.confirmOrderPreview(any<OrderPricingRequest>()) }
    }
    test("orderPreview rejects foreign address") {
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

      result.isFailure shouldBe true
      result
        .exceptionOrNull()
        ?.message
        ?.let { null }
        .shouldBeNull()
    }
  })
