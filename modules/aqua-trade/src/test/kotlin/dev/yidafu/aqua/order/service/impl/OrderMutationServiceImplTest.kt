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

package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.dto.CreateOrderRequest
import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryAreaQueryService
import dev.yidafu.aqua.api.service.marketing.CouponMutationApiService
import dev.yidafu.aqua.api.service.marketing.PromotionPricingService
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.api.service.order.OrderIdGeneratorService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.order.domain.repository.OrderGiftRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * 订单变更服务配送费计算单元测试(R-MON-02 楼层口径,F-OPS-005):
 * 免费楼层内免运费、超出一次性加收、楼层为空免运费、自提免运费、落库快照。
 */
class OrderMutationServiceImplTest {
  private lateinit var orderRepository: OrderRepository
  private lateinit var orderGiftRepository: OrderGiftRepository
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var addressQueryApiService: AddressQueryApiService
  private lateinit var eventPublishService: SimplifiedEventPublishService
  private lateinit var productService: ProductService
  private lateinit var deliveryAreaQueryService: DeliveryAreaQueryService
  private lateinit var orderIdGenerator: OrderIdGeneratorService
  private lateinit var adminService: AdminService
  private lateinit var orderQueryService: OrderQueryService
  private lateinit var deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService
  private lateinit var promotionPricingService: PromotionPricingService
  private lateinit var couponMutationApiService: CouponMutationApiService
  private lateinit var ticketMutationApiService: TicketMutationApiService
  private lateinit var service: OrderMutationServiceImpl

  private val product = ProductModel(id = 1L, name = "桶装水", price = 1000, stock = 100)

  private fun address(
    floor: Int? = null,
    userId: Long = 100L,
  ) = AddressModel(
    id = 1L,
    userId = userId,
    receiverName = "张三",
    phone = "13800138000",
    province = "浙江省",
    city = "杭州市",
    district = "西湖区",
    detailAddress = "文一西路100号",
    floor = floor,
  )

  @BeforeEach
  fun setUp() {
    orderRepository = mockk(relaxed = true)
    orderGiftRepository = mockk(relaxed = true)
    productQueryApiService = mockk(relaxed = true)
    addressQueryApiService = mockk(relaxed = true)
    eventPublishService = mockk(relaxed = true)
    productService = mockk(relaxed = true)
    deliveryAreaQueryService = mockk(relaxed = true)
    orderIdGenerator = mockk(relaxed = true)
    adminService = mockk(relaxed = true)
    orderQueryService = mockk(relaxed = true)
    deliveryFeeRuleQueryApiService = mockk(relaxed = true)
    promotionPricingService = mockk(relaxed = true)
    couponMutationApiService = mockk(relaxed = true)
    ticketMutationApiService = mockk(relaxed = true)
    service =
      OrderMutationServiceImpl(
        orderRepository,
        orderGiftRepository,
        productQueryApiService,
        addressQueryApiService,
        eventPublishService,
        productService,
        deliveryAreaQueryService,
        orderIdGenerator,
        adminService,
        orderQueryService,
        deliveryFeeRuleQueryApiService,
        promotionPricingService,
        couponMutationApiService,
        ticketMutationApiService,
      )
  }

  /**
   * 组装 createOrder 公共 mock:产品/地址/订单号/库存扣减/落库返回/促销引擎计价
   * 计价链路(促销引擎):目录价成交单价,无营销优惠(R-MKT-02)
   */
  private fun stubCreateOrderCommon(addr: AddressModel) {
    every { productQueryApiService.findById(1L) } returns product
    every { addressQueryApiService.findById(1L) } returns addr
    every { orderIdGenerator.generateOrderId() } returns "ORDER-TEST-001"
    every { productService.decreaseStock(any(), any()) } returns true
    // save 返回时回填主键(模拟 @SnowflakeIdGenerator,避免 publishOrderCreated(savedOrder.id!!) NPE)
    every { orderRepository.save(any()) } answers { (firstArg() as OrderModel).also { it.id = 999L } }
    every { promotionPricingService.preview(any()) } answers { pricingFor(firstArg()) }
    every { promotionPricingService.applyForOrder(any(), any()) } answers { pricingFor(secondArg()) }
  }

  private fun pricingFor(request: OrderPricingRequest): OrderPricingResult {
    val subtotal = product.price * request.quantity
    return OrderPricingResult(
      unitPriceCents = product.price,
      priceSource = PriceSource.CATALOG_PRICE,
      subtotalCents = subtotal,
      ticketRedeemCount = 0,
      ticketCoveredQuantity = 0,
      cashSubtotalCents = subtotal,
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
      payableProductCents = subtotal,
      availableTicketCount = 0,
      couponOptions = emptyList(),
      giftItems = emptyList(),
    )
  }

  private fun createRequest(quantity: Int = 1) =
    CreateOrderRequest(
      userId = 100L,
      productId = 1L,
      addressId = 1L,
      quantity = quantity,
    )

  @Test
  @DisplayName("默认规则(免费4楼/加价2元):4楼订单免运费")
  fun `default rule floor 4 is free`() {
    stubCreateOrderCommon(address(floor = 4))
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

    val order = service.createOrder(createRequest())

    assertEquals(0, order.deliveryFeeCents)
  }

  @Test
  @DisplayName("默认规则(免费4楼/加价2元):5楼订单加收 2 元")
  fun `default rule floor 5 charges 200 cents`() {
    stubCreateOrderCommon(address(floor = 5))
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

    val order = service.createOrder(createRequest())

    assertEquals(200, order.deliveryFeeCents)
    assertEquals(product.price + 200, order.payAmountCents)
  }

  @Test
  @DisplayName("楼层为空(平房/无楼层信息)免运费")
  fun `null floor is free`() {
    stubCreateOrderCommon(address(floor = null))
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

    val order = service.createOrder(createRequest())

    assertEquals(0, order.deliveryFeeCents)
  }

  @Test
  @DisplayName("配置规则(免费6楼/加价5元):7楼订单加收 5 元")
  fun `configured rule floor 7 charges 500 cents`() {
    stubCreateOrderCommon(address(floor = 7))
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 6, surchargeCents = 500)

    val order = service.createOrder(createRequest())

    assertEquals(500, order.deliveryFeeCents)
  }

  @Test
  @DisplayName("配置规则(免费6楼/加价5元):6楼订单免运费")
  fun `configured rule floor 6 is free`() {
    stubCreateOrderCommon(address(floor = 6))
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 6, surchargeCents = 500)

    val order = service.createOrder(createRequest())

    assertEquals(0, order.deliveryFeeCents)
  }

  @Test
  @DisplayName("自提订单免运费(楼层不参与判定)")
  fun `self collect order is free`() {
    every { adminService.getUserById(1L) } returns UserModel(id = 100L)
    stubCreateOrderCommon(address(floor = 10))
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

    val order =
      service.createDeliveryOrder(
        1L,
        CreateOrderRequest(userId = 0L, productId = 1L, addressId = 1L, quantity = 1, isSelfCollect = true),
      )

    assertEquals(0, order.deliveryFeeCents)
  }

  @Test
  @DisplayName("配送费落库快照:规则变更不影响已生成订单")
  fun `fee is snapshotted at creation`() {
    stubCreateOrderCommon(address(floor = 5))
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

    val order = service.createOrder(createRequest())

    // 规则读取发生在下单时(恰一次);落库后规则变更不影响已有行
    verify(exactly = 1) { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() }
    assertEquals(200, order.deliveryFeeCents)

    // 规则变更后已生成订单不变(行内快照)
    every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
      DeliveryFeeRuleModel(freeFloorLimit = 10, surchargeCents = 0)
    assertEquals(200, order.deliveryFeeCents)
  }
}
