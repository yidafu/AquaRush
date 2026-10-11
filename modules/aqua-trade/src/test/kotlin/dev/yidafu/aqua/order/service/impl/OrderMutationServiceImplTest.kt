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
import dev.yidafu.aqua.api.dto.DepositCalculation
import dev.yidafu.aqua.api.dto.OrderItemRequest
import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.api.service.delivery.DeliveryAreaQueryService
import dev.yidafu.aqua.api.service.deposit.DepositMutationApiService
import dev.yidafu.aqua.api.service.marketing.CouponMutationApiService
import dev.yidafu.aqua.api.service.marketing.PromotionPricingService
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.api.service.order.OrderIdGeneratorService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.order.domain.repository.OrderGiftRepository
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

/**
 * 订单变更服务单元测试:
 * - R-MON-02 配送费(楼层口径):免费楼层内免运费、超出一次性加收、楼层为空免运费、自提免运费、落库快照;
 * - R-ORD-01 状态机(G09):支付成功→待派单、updateOrderStatus 流转校验、取消退款判定;
 * - G51 代客下单归属:订单 userId 取收货地址所属客户。
 */
class OrderMutationServiceImplTest :
  FunSpec({
    var orderRepository: OrderRepository = mockk()
    var orderItemRepository: OrderItemRepository = mockk()
    var orderGiftRepository: OrderGiftRepository = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var addressQueryApiService: AddressQueryApiService = mockk()
    var eventPublishService: SimplifiedEventPublishService = mockk()
    var productService: ProductService = mockk()
    var deliveryAreaQueryService: DeliveryAreaQueryService = mockk()
    var orderIdGenerator: OrderIdGeneratorService = mockk()
    var orderQueryService: OrderQueryService = mockk()
    var deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService = mockk()
    var promotionPricingService: PromotionPricingService = mockk()
    var couponMutationApiService: CouponMutationApiService = mockk()
    var ticketMutationApiService: TicketMutationApiService = mockk()
    var orderOperationService: OrderOperationService = mockk()
    var depositMutationApiService: DepositMutationApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: OrderMutationServiceImpl = mockk()
    val product = ProductModel(id = 1L, name = "桶装水", price = 1000, stock = 100)

    fun address(
      floor: Int? = null,
      userId: Long = 100L,
      longitude: Double? = null,
      latitude: Double? = null,
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
      longitude = longitude,
      latitude = latitude,
    )

    fun pricingFor(request: OrderPricingRequest): OrderPricingResult {
      val subtotal = product.price * request.items.sumOf { it.quantity }
      return OrderPricingResult(
        items =
          request.items.map {
            dev.yidafu.aqua.api.dto.OrderPricingItemResult(
              productId = it.productId,
              quantity = it.quantity,
              unitPriceCents = product.price,
              priceSource = PriceSource.CATALOG_PRICE,
              catalogPriceCents = product.price,
              subtotalCents = product.price * it.quantity,
              ticketCoveredQuantity = 0,
              cashSubtotalCents = product.price * it.quantity,
            )
          },
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

    fun createRequest(quantity: Int = 1) =
      CreateOrderRequest(
        userId = 100L,
        items = listOf(OrderItemRequest(productId = 1L, quantity = quantity)),
        addressId = 1L,
      )

    fun stubCreateOrderCommon(addr: AddressModel) {
      every { productQueryApiService.findById(1L) } returns product
      every { addressQueryApiService.findById(1L) } returns addr
      every { orderIdGenerator.generateOrderId() } returns "ORDER-TEST-001"
      every { productService.decreaseStock(any(), any()) } returns true
      // save 返回时回填主键(模拟 @SnowflakeIdGenerator,避免 publishOrderCreated(savedOrder.id!!) NPE)
      every { orderRepository.save(any()) } answers { (firstArg() as OrderModel).also { it.id = 999L } }
      every { promotionPricingService.preview(any()) } answers { pricingFor(firstArg()) }
      every { promotionPricingService.applyForOrder(any(), any()) } answers { pricingFor(secondArg()) }
    }

    // 组装 createOrder 公共 mock:产品/地址/订单号/库存扣减/落库返回/促销引擎计价
    // 计价链路(促销引擎):目录价成交单价,无营销优惠(R-MKT-02)

    // ============ 多商品下单(F-ORD-004) ============

    beforeTest {
      orderRepository = mockk(relaxed = true)
      orderItemRepository = mockk(relaxed = true)
      orderGiftRepository = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      addressQueryApiService = mockk(relaxed = true)
      eventPublishService = mockk(relaxed = true)
      productService = mockk(relaxed = true)
      deliveryAreaQueryService = mockk(relaxed = true)
      orderIdGenerator = mockk(relaxed = true)
      orderQueryService = mockk(relaxed = true)
      deliveryFeeRuleQueryApiService = mockk(relaxed = true)
      promotionPricingService = mockk(relaxed = true)
      couponMutationApiService = mockk(relaxed = true)
      ticketMutationApiService = mockk(relaxed = true)
      orderOperationService = mockk(relaxed = true)
      depositMutationApiService = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      // 单测无安全上下文,resolveOperator 返回空操作者(日志仅缺操作人,不阻断业务)
      every { bizLogger.resolveOperator() } returns (null to null)
      service =
        OrderMutationServiceImpl(
          orderRepository,
          orderItemRepository,
          orderGiftRepository,
          productQueryApiService,
          addressQueryApiService,
          eventPublishService,
          productService,
          deliveryAreaQueryService,
          orderIdGenerator,
          orderQueryService,
          deliveryFeeRuleQueryApiService,
          promotionPricingService,
          couponMutationApiService,
          ticketMutationApiService,
          orderOperationService,
          depositMutationApiService,
          bizLogger,
        )
      // 押金试算默认无押金(未配押金价目);建账 no-op
      every { depositMutationApiService.calcOrderDeposit(any(), any()) } returns DepositCalculation(0L, emptyList())
      every { depositMutationApiService.createPendingDeposits(any(), any(), any()) } returns emptyList()
    }
    test("default rule floor 4 is free") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order = service.createOrder(createRequest())

      order.deliveryFeeCents shouldBe 0
    }
    test("default rule floor 5 charges 200 cents") {
      stubCreateOrderCommon(address(floor = 5))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order = service.createOrder(createRequest())

      order.deliveryFeeCents shouldBe 200
      order.payAmountCents shouldBe product.price + 200
    }
    test("tiered fee by floor bands (R-MON-02 分档)") {
      // 默认三档:5-6 +200 / 7-9 +500 / 10+ +800
      stubCreateOrderCommon(address(floor = 8))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)
      val order = service.createOrder(createRequest())
      order.deliveryFeeCents shouldBe 500
    }
    test("tiered fee falls back to surcharge when tiers empty (存量行兼容)") {
      stubCreateOrderCommon(address(floor = 8))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 250, tiers = null)
      val order = service.createOrder(createRequest())
      order.deliveryFeeCents shouldBe 250
    }
    test("null floor is free") {
      stubCreateOrderCommon(address(floor = null))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order = service.createOrder(createRequest())

      order.deliveryFeeCents shouldBe 0
    }
    test("configured rule floor 7 charges 500 cents") {
      stubCreateOrderCommon(address(floor = 7))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 6, surchargeCents = 500)

      val order = service.createOrder(createRequest())

      order.deliveryFeeCents shouldBe 500
    }
    test("configured rule floor 6 is free") {
      stubCreateOrderCommon(address(floor = 6))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 6, surchargeCents = 500)

      val order = service.createOrder(createRequest())

      order.deliveryFeeCents shouldBe 0
    }
    test("self collect order is free") {
      stubCreateOrderCommon(address(floor = 10))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order =
        service.createDeliveryOrder(
          1L,
          CreateOrderRequest(
            userId = 0L,
            items =
              listOf(
                dev.yidafu.aqua.api.dto
                  .OrderItemRequest(1L, 1),
              ),
            addressId = 1L,
            isSelfCollect = true,
          ),
        )

      order.deliveryFeeCents shouldBe 0
    }
    test("fee is snapshotted at creation") {
      stubCreateOrderCommon(address(floor = 5))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order = service.createOrder(createRequest())

      // 规则读取发生在下单时(恰一次);落库后规则变更不影响已有行
      verify(exactly = 1) { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() }
      order.deliveryFeeCents shouldBe 200

      // 规则变更后已生成订单不变(行内快照)
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 10, surchargeCents = 0)
      order.deliveryFeeCents shouldBe 200
    }
    test("floor and distance tiers stack (R-MON-02 + R-OPS-03 叠加计费)") {
      // 7 楼命中默认 7-9 档 +500;地址距门店约 4.4km 命中 3-5km 档 +300
      stubCreateOrderCommon(address(floor = 7, longitude = 113.97, latitude = 22.53))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(
          freeFloorLimit = 4,
          surchargeCents = 200,
          distanceTiers =
            listOf(
              dev.yidafu.aqua.common.domain.model
                .DeliveryDistanceTier(upToKm = 3.0, feeCents = 0L),
              dev.yidafu.aqua.common.domain.model
                .DeliveryDistanceTier(upToKm = 5.0, feeCents = 300L),
              dev.yidafu.aqua.common.domain.model
                .DeliveryDistanceTier(upToKm = null, feeCents = 600L),
            ),
          storeLongitude = 113.93,
          storeLatitude = 22.53,
        )

      val order = service.createOrder(createRequest())

      // 快照口径(R-MON-02):楼层 + 距离合计落 delivery_fee_cents
      order.deliveryFeeCents shouldBe 800L
      order.payAmountCents shouldBe product.price + 800L
    }

    test("distance tier not charged when store or address coordinates missing") {
      stubCreateOrderCommon(address(floor = 7, longitude = null, latitude = null))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(
          freeFloorLimit = 4,
          surchargeCents = 200,
          distanceTiers =
            listOf(
              dev.yidafu.aqua.common.domain.model
                .DeliveryDistanceTier(upToKm = null, feeCents = 600L),
            ),
          storeLongitude = 113.93,
          storeLatitude = 22.53,
        )

      val order = service.createOrder(createRequest())

      order.deliveryFeeCents shouldBe 500L
    }

    test("createOrder rejected below minimum order amount (R-OPS-03 起送门槛)") {
      // 商品成交小计 1000 分(¥10),门槛 1500 分(¥15)→ 拦截并提示差额
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200, minimumOrderCents = 1500L)

      val exception = shouldThrow<BadRequestException> { service.createOrder(createRequest()) }
      exception.message shouldBe "商品金额未达起送金额 ¥15.00，还差 ¥5.00"
    }

    test("createOrder allowed when product amount equals minimum (等于门槛放行)") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200, minimumOrderCents = 1000L)

      val order = service.createOrder(createRequest())
      order.deliveryFeeCents shouldBe 0L
    }

    test("createOrder allowed when product amount above minimum (高于门槛放行)") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200, minimumOrderCents = 500L)

      val order = service.createOrder(createRequest())
      order.status shouldBe OrderModelStatus.PENDING_PAYMENT
    }

    test("self collect proxy order exempt from minimum order (自提豁免)") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200, minimumOrderCents = 1500L)

      val order =
        service.createDeliveryOrder(
          1L,
          CreateOrderRequest(
            userId = 0L,
            items = listOf(OrderItemRequest(productId = 1L, quantity = 1)),
            addressId = 1L,
            isSelfCollect = true,
          ),
        )

      order.deliveryFeeCents shouldBe 0L
    }

    test("createOrderEntersPendingPayment") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order = service.createOrder(createRequest())

      order.status shouldBe OrderModelStatus.PENDING_PAYMENT
    }
    test("deliveryOrderStaysPendingDispatch") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order =
        service.createDeliveryOrder(
          1L,
          CreateOrderRequest(
            userId = 0L,
            items =
              listOf(
                dev.yidafu.aqua.api.dto
                  .OrderItemRequest(1L, 1),
              ),
            addressId = 1L,
            isSelfCollect = false,
          ),
        )

      order.status shouldBe OrderModelStatus.PENDING_DISPATCH
    }
    test("create order writes business log") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      service.createOrder(createRequest())

      verify(exactly = 1) {
        bizLogger.logOrderOperation(
          orderId = 999L,
          operation = "ORDER_CREATED",
          result = "SUCCESS",
          userId = 100L,
          additionalData = any(),
        )
      }
    }
    test("cancel order writes business log") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderQueryService.getOrderById(1L) } returns order
      every { orderRepository.cancelAtomically(1L, any(), any()) } returns 1
      every { productService.increaseStock(any(), any()) } returns true

      service.cancelOrder(1L)

      verify(exactly = 1) {
        bizLogger.logOrderOperation(
          orderId = 1L,
          operation = "ORDER_CANCELLED",
          result = "SUCCESS",
          userId = any(),
          username = any(),
          additionalData = any(),
        )
      }
    }
    test("payment success transitions to pending dispatch") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderQueryService.getOrderById(1L) } returns order
      every { orderRepository.save(any()) } answers { firstArg() }

      service.handlePaymentSuccess(1L, "WX-TXN-001")

      order.status shouldBe OrderModelStatus.PENDING_DISPATCH
      order.paymentTransactionId shouldBe "WX-TXN-001"
      (order.paymentTime != null) shouldBe true
    }
    test("payment success rejects non pending payment") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_DISPATCH)
      every { orderQueryService.getOrderById(1L) } returns order

      shouldThrow<BadRequestException> { service.handlePaymentSuccess(1L, "WX-TXN-001") }
    }
    test("update order status allows cancel from pending payment") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderQueryService.getOrderById(1L) } returns order
      every { orderRepository.updateStatusAtomically(1L, any(), any(), any()) } returns 1

      val updated = service.updateOrderStatus(1L, OrderModelStatus.CANCELLED)

      updated.status shouldBe OrderModelStatus.CANCELLED
      verify(exactly = 1) {
        orderRepository.updateStatusAtomically(1L, OrderModelStatus.PENDING_PAYMENT, OrderModelStatus.CANCELLED, any())
      }
      verify(exactly = 0) { orderRepository.save(any()) }
    }
    test("update order status allows refunded from cancelled") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.CANCELLED)
      every { orderQueryService.getOrderById(1L) } returns order
      every { orderRepository.updateStatusAtomically(1L, any(), any(), any()) } returns 1

      val updated = service.updateOrderStatus(1L, OrderModelStatus.REFUNDED)

      updated.status shouldBe OrderModelStatus.REFUNDED
    }
    test("update order status rejects when CAS claim lost (状态谓词条件更新, 裁决结构 #9)") {
      // 读到 PENDING_PAYMENT,但写入前已被并发流转 → 条件 UPDATE 0 行 → 抛状态冲突
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderQueryService.getOrderById(1L) } returns order
      every { orderRepository.updateStatusAtomically(1L, any(), any(), any()) } returns 0

      val exception = shouldThrow<BadRequestException> { service.updateOrderStatus(1L, OrderModelStatus.CANCELLED) }

      exception.message!!.contains("状态冲突") shouldBe true
      // 冲突时不落库,不覆盖并发写入
      verify(exactly = 0) { orderRepository.save(any()) }
    }
    test("update order status rejects illegal transition") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderQueryService.getOrderById(1L) } returns order

      shouldThrow<BadRequestException> { service.updateOrderStatus(1L, OrderModelStatus.DELIVERING) }
      shouldThrow<BadRequestException> { service.updateOrderStatus(1L, OrderModelStatus.COMPLETED) }
      // 已完成订单不可再流转
      every { orderQueryService.getOrderById(2L) } returns
        OrderModel(id = 2L, userId = 100L, status = OrderModelStatus.COMPLETED)
      shouldThrow<BadRequestException> { service.updateOrderStatus(2L, OrderModelStatus.CANCELLED) }
    }
    test("cancel paid pending dispatch order triggers refund") {
      val order =
        OrderModel(
          id = 1L,
          userId = 100L,
          status = OrderModelStatus.PENDING_DISPATCH,
          paymentTransactionId = "WX-TXN-001",
        )
      // CAS 认领后重新读取返回最新状态(CANCELLED)
      every { orderQueryService.getOrderById(1L) } returnsMany
        listOf(order, order.copy(status = OrderModelStatus.CANCELLED))
      every { orderRepository.cancelAtomically(1L, any(), any()) } returns 1
      every { productService.increaseStock(any(), any()) } returns true

      val result = service.cancelOrder(1L)

      result.status shouldBe OrderModelStatus.CANCELLED
      verify(exactly = 1) {
        eventPublishService.publishOrderCancelled(
          orderId = 1L,
          userId = 100L,
          reason = any(),
          shouldRefund = true,
          paymentTransactionId = "WX-TXN-001",
        )
      }
    }
    test("cancel skips restore when claim lost (订单级互斥)") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      // 并发取消/支付超时已认领 → CAS 返回 0,重新读取为 CANCELLED
      every { orderQueryService.getOrderById(1L) } returnsMany
        listOf(order, order.copy(status = OrderModelStatus.CANCELLED))
      every { orderRepository.cancelAtomically(1L, any(), any()) } returns 0

      shouldThrow<BadRequestException> { service.cancelOrder(1L) }

      verify(exactly = 0) { productService.increaseStock(any(), any()) }
    }
    test("refunded order cannot be cancelled") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.REFUNDED)
      every { orderQueryService.getOrderById(1L) } returns order

      shouldThrow<BadRequestException> { service.cancelOrder(1L) }
    }
    test("urge delivering order publishes ORDER_URGING event and records operation") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.DELIVERING, deliveryWorkerId = 3L)
      every { orderRepository.findById(1L) } returns Optional.of(order)
      every { orderOperationService.getOrderOperations(1L) } returns emptyList()

      val result = service.urgeOrder(1L, 100L)

      result shouldBe order
      verify(exactly = 1) {
        orderOperationService.recordOperation(
          1L,
          OrderOperationType.ORDER_URGING,
          OperatorType.USER,
          100L,
          "用户催单",
        )
      }
      verify(exactly = 1) {
        eventPublishService.publishDomainEvent(
          match { it == "ORDER_URGING" },
          match { it == "1" },
          any(),
        )
      }
    }
    test("urge rejects non delivering order") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_DELIVERY)
      every { orderRepository.findById(1L) } returns Optional.of(order)

      shouldThrow<BadRequestException> { service.urgeOrder(1L, 100L) }
    }
    test("urge debounce: recent urge within 5 minutes rejected") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.DELIVERING)
      every { orderRepository.findById(1L) } returns Optional.of(order)
      val recent =
        OrderOperationModel(
          operationType = OrderOperationType.ORDER_URGING,
          createdAt = LocalDateTime.now().minusMinutes(1),
        )
      every { orderOperationService.getOrderOperations(1L) } returns listOf(recent)

      shouldThrow<BadRequestException> { service.urgeOrder(1L, 100L) }
    }
    test("urge returns null for other user's order") {
      val order = OrderModel(id = 1L, userId = 200L, status = OrderModelStatus.DELIVERING)
      every { orderRepository.findById(1L) } returns Optional.of(order)

      service.urgeOrder(1L, 100L).shouldBeNull()
    }
    test("delivery order belongs to address owner") {
      // 地址归属客户 200L(而非送水工本人)
      stubCreateOrderCommon(address(floor = 4, userId = 200L))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order =
        service.createDeliveryOrder(
          1L,
          CreateOrderRequest(
            userId = 0L,
            items =
              listOf(
                dev.yidafu.aqua.api.dto
                  .OrderItemRequest(1L, 1),
              ),
            addressId = 1L,
            isSelfCollect = false,
          ),
        )

      order.userId shouldBe 200L
      // 计价按客户解析(促销引擎入参 userId=客户)
      verify(exactly = 1) {
        promotionPricingService.preview(match { it.userId == 200L })
      }
    }
    test("multi item order persists items") {
      val addr = address(floor = 4)
      stubCreateOrderCommon(addr)
      every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "矿泉水", price = 500, stock = 100)
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)
      every { orderItemRepository.saveAll(any<List<OrderItemModel>>()) } answers { firstArg() }

      val order =
        service.createOrder(
          CreateOrderRequest(
            userId = 100L,
            items = listOf(OrderItemRequest(1L, 2), OrderItemRequest(2L, 3)),
            addressId = 1L,
          ),
        )

      // 明细逐项落库(商品1×2 + 商品2×3)
      verify(exactly = 1) { orderItemRepository.saveAll(any<List<OrderItemModel>>()) }
      order.status shouldBe OrderModelStatus.PENDING_PAYMENT
    }
    test("multi item partial stock shortage blocks order") {
      val addr = address(floor = 4)
      stubCreateOrderCommon(addr)
      val second = ProductModel(id = 2L, name = "矿泉水", price = 500, stock = 0)
      every { productQueryApiService.findById(2L) } returns second
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      shouldThrow<BadRequestException> {
        service.createOrder(
          CreateOrderRequest(
            userId = 100L,
            items = listOf(OrderItemRequest(1L, 1), OrderItemRequest(2L, 1)),
            addressId = 1L,
          ),
        )
      }

      // 未扣减任何库存
      verify(exactly = 0) { productService.decreaseStock(any(), any()) }
    }
    test("cancel multi item order restores each stock") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderQueryService.getOrderById(1L) } returns order
      every { orderRepository.cancelAtomically(1L, any(), any()) } returns 1
      every { productService.increaseStock(any(), any()) } returns true
      every { orderItemRepository.findByOrderId(1L) } returns
        listOf(
          OrderItemModel(orderId = 1L, productId = 1L, quantity = 2, unitPriceCents = 1000, totalPriceCents = 2000, productSnapshot = "{}"),
          OrderItemModel(orderId = 1L, productId = 2L, quantity = 3, unitPriceCents = 500, totalPriceCents = 1500, productSnapshot = "{}"),
        )

      service.cancelOrder(1L)

      verify(exactly = 1) { productService.increaseStock(1L, 2) }
      verify(exactly = 1) { productService.increaseStock(2L, 3) }
    }

    // ============ 支付超时订单级互斥(R-ORD-04,乐观锁 CAS) ============

    test("payment timeout claims and restores stock") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderQueryService.getOrderById(1L) } returns order
      every { orderRepository.cancelAtomically(1L, any(), any()) } returns 1
      every { productService.increaseStock(any(), any()) } returns true
      every { orderItemRepository.findByOrderId(1L) } returns
        listOf(
          OrderItemModel(
            orderId = 1L,
            productId = 1L,
            quantity = 2,
            unitPriceCents = 1000,
            totalPriceCents = 2000,
            productSnapshot = "{}",
          ),
        )

      service.handlePaymentTimeout(1L)

      verify(exactly = 1) { productService.increaseStock(1L, 2) }
      verify(exactly = 1) {
        eventPublishService.publishPaymentTimeout(orderId = 1L, userId = 100L)
      }
    }
    test("payment timeout skips when claim lost (与手动取消互斥)") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderQueryService.getOrderById(1L) } returns order
      every { orderRepository.cancelAtomically(1L, any(), any()) } returns 0

      service.handlePaymentTimeout(1L)

      verify(exactly = 0) { productService.increaseStock(any(), any()) }
      verify(exactly = 0) { eventPublishService.publishPaymentTimeout(any<Long>(), any<Long>()) }
    }
    test("payment timeout skips non pending order") {
      val order = OrderModel(id = 1L, userId = 100L, status = OrderModelStatus.PENDING_DELIVERY)
      every { orderQueryService.getOrderById(1L) } returns order

      service.handlePaymentTimeout(1L)

      verify(exactly = 0) { orderRepository.cancelAtomically(any(), any(), any()) }
      verify(exactly = 0) { productService.increaseStock(any(), any()) }
    }

    // ============ 服务端下单幂等(R-ORD-06) ============

    test("create order with same clientRequestId returns existing order without side effects") {
      // Given: 同一用户同一 clientRequestId 已存在订单(首次提交成功后的重复提交/网络重试)
      val existing = OrderModel(id = 555L, userId = 100L, status = OrderModelStatus.PENDING_PAYMENT)
      every { orderRepository.findByUserIdAndClientRequestId(100L, "req-001") } returns existing

      // When
      val result =
        service.createOrder(
          CreateOrderRequest(
            userId = 100L,
            items = listOf(OrderItemRequest(1L, 1)),
            addressId = 1L,
            clientRequestId = "req-001",
          ),
        )

      // Then: 返回已存在订单,不重复计价/扣库存/落库/核销权益
      result.id shouldBe 555L
      verify(exactly = 0) { promotionPricingService.preview(any()) }
      verify(exactly = 0) { productService.decreaseStock(any(), any()) }
      verify(exactly = 0) { orderRepository.save(any()) }
    }

    test("create order without clientRequestId keeps legacy behavior") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order = service.createOrder(createRequest())

      order.clientRequestId shouldBe null
      order.status shouldBe OrderModelStatus.PENDING_PAYMENT
    }

    // ============ 配送时段落库(AC-ORD-001-05) ============

    test("create order persists deliveryTimeSlot snapshot") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order =
        service.createOrder(
          CreateOrderRequest(
            userId = 100L,
            items = listOf(OrderItemRequest(1L, 1)),
            addressId = 1L,
            deliveryTimeSlot = DeliveryTimeSlot.AFTERNOON,
          ),
        )

      order.deliveryTimeSlot shouldBe DeliveryTimeSlot.AFTERNOON
    }

    test("create order defaults deliveryTimeSlot to immediate") {
      stubCreateOrderCommon(address(floor = 4))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)

      val order = service.createOrder(createRequest())

      order.deliveryTimeSlot shouldBe DeliveryTimeSlot.IMMEDIATE
    }

    // ============ 代客下单多商品(F-ORD-012) ============

    test("delivery order supports multi product items") {
      val addr = address(floor = 4, userId = 200L)
      stubCreateOrderCommon(addr)
      every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "矿泉水", price = 500, stock = 100)
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)
      every { orderItemRepository.saveAll(any<List<OrderItemModel>>()) } answers { firstArg() }

      val order =
        service.createDeliveryOrder(
          1L,
          CreateOrderRequest(
            userId = 0L,
            items = listOf(OrderItemRequest(1L, 2), OrderItemRequest(2L, 1)),
            addressId = 1L,
            isSelfCollect = false,
          ),
        )

      // 归属收货地址客户;逐项明细落库;现场收款直接入「待派单」
      order.userId shouldBe 200L
      order.status shouldBe OrderModelStatus.PENDING_DISPATCH
      order.deliveryTimeSlot shouldBe null
      verify(exactly = 1) { orderItemRepository.saveAll(any<List<OrderItemModel>>()) }
    }

    // ============ 周期订购代客下单幂等(G77/R-ORD-13) ============

    test("delivery order with same clientRequestId returns existing order (sub period idempotency)") {
      // 同一用户同一 clientRequestId 已存在 → 直接返回,不重复扣库存/落库
      // (代客下单 userId 取收货地址所属客户 200L,幂等键按该客户查)
      val existing = OrderModel(id = 777L, userId = 200L, status = OrderModelStatus.PENDING_DISPATCH)
      every { addressQueryApiService.findById(1L) } returns address(userId = 200L)
      every { orderRepository.findByUserIdAndClientRequestId(200L, "sub-9-3") } returns existing

      val result =
        service.createDeliveryOrder(
          1L,
          CreateOrderRequest(
            userId = 0L,
            items = listOf(OrderItemRequest(1L, 1)),
            addressId = 1L,
            clientRequestId = "sub-9-3",
          ),
        )

      result.id shouldBe 777L
      verify(exactly = 0) { productService.decreaseStock(any(), any()) }
      verify(exactly = 0) { orderRepository.save(any()) }
    }

    test("delivery order persists clientRequestId and deliveryTimeSlot snapshot") {
      stubCreateOrderCommon(address(floor = 4, userId = 200L))
      every { deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule() } returns
        DeliveryFeeRuleModel(freeFloorLimit = 4, surchargeCents = 200)
      // 幂等查询未命中(relaxed mock 会返回非空实例,显式置空走完整创建链路)
      every { orderRepository.findByUserIdAndClientRequestId(200L, "sub-9-1") } returns null

      val order =
        service.createDeliveryOrder(
          1L,
          CreateOrderRequest(
            userId = 0L,
            items = listOf(OrderItemRequest(1L, 1)),
            addressId = 1L,
            clientRequestId = "sub-9-1",
            deliveryTimeSlot = DeliveryTimeSlot.MORNING,
          ),
        )

      order.clientRequestId shouldBe "sub-9-1"
      order.deliveryTimeSlot shouldBe DeliveryTimeSlot.MORNING
    }
  })
