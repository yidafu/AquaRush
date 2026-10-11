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
import dev.yidafu.aqua.api.dto.OrderDepositItem
import dev.yidafu.aqua.api.dto.OrderItemRequest
import dev.yidafu.aqua.api.dto.OrderPricingItemRequest
import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.api.service.delivery.DeliveryAreaQueryService
import dev.yidafu.aqua.api.service.deposit.DepositMutationApiService
import dev.yidafu.aqua.api.service.marketing.CouponMutationApiService
import dev.yidafu.aqua.api.service.marketing.PromotionPricingService
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.api.service.order.OrderIdGeneratorService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.common.domain.model.OrderGiftModel
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.messaging.event.DomainEventType
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.common.util.SearchContentBuilder
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.order.domain.repository.OrderGiftRepository
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.Locale

/**
 * 订单变更服务实现(F-ORD-004 多商品下单):
 * 商品明细(items)为唯一下单口径,订单商品权威数据落 order_items 表(orders 仅存聚合金额/营销快照);
 * 计价链路(PRD 10-marketing.md §R-MKT-02/07/08,由促销引擎统一计算):
 * 成交单价逐商品解析(分组价→目录价)→ 水票核销(被覆盖桶单价 0)→ 满减/折扣 → 优惠券(自动最优,用户可改)→ 封顶;
 * 下单时营销结果快照到订单,营销配置变更不影响已生成订单(R-MKT-01)。
 */
@Service
class OrderMutationServiceImpl(
  private val orderRepository: OrderRepository,
  private val orderItemRepository: OrderItemRepository,
  private val orderGiftRepository: OrderGiftRepository,
  private val productQueryApiService: ProductQueryApiService,
  private val addressQueryApiService: AddressQueryApiService,
  private val eventPublishService: SimplifiedEventPublishService,
  private val productService: ProductService,
  private val deliveryAreaQueryService: DeliveryAreaQueryService,
  private val orderIdGenerator: OrderIdGeneratorService,
  private val orderQueryService: OrderQueryService,
  private val deliveryFeeRuleQueryApiService: DeliveryFeeRuleQueryApiService,
  private val promotionPricingService: PromotionPricingService,
  private val couponMutationApiService: CouponMutationApiService,
  private val ticketMutationApiService: TicketMutationApiService,
  private val orderOperationService: OrderOperationService,
  private val depositMutationApiService: DepositMutationApiService,
  private val bizLogger: BizLogger,
) : OrderMutationService {
  private val logger = LoggerFactory.getLogger(OrderMutationServiceImpl::class.java)
  private val objectMapper = jacksonObjectMapper()

  /** 校验通过的商品明细项 */
  private data class ValidatedOrderItem(
    val product: ProductModel,
    val quantity: Int,
  )

  /**
   * 订单验证结果，包含验证通过后所需的全部数据
   */
  private data class OrderValidationResult(
    val items: List<ValidatedOrderItem>,
    val address: AddressModel,
    val userId: Long,
    val deliveryFeeCents: Long,
    val orderNo: String,
    /** 本次计价采用的配送费规则(门槛判定与运费同源,避免重复读配置) */
    val deliveryFeeRule: DeliveryFeeRuleModel,
  )

  /**
   * 统一的订单验证逻辑，提取 createOrder 和 createDeliveryOrder 的公共验证步骤:
   * 多商品(F-ORD-004)先对全部商品做存在/库存校验(任一不足整体拦截、不扣任何库存),
   * 全部通过后逐商品原子扣减(事务回滚保证原子性)。
   */
  private fun validateAndPrepareOrder(
    userId: Long,
    items: List<OrderItemRequest>,
    addressId: Long,
    isSelfCollect: Boolean,
  ): OrderValidationResult {
    // 1. 验证全部商品存在且有足够库存(整体校验,列出全部缺货项)
    val validatedItems =
      items.map { item ->
        val product =
          productQueryApiService.findById(item.productId)
            ?: throw NotFoundException("产品不存在: ${item.productId}")
        if (product.stock < item.quantity) {
          throw BadRequestException(
            "以下商品库存不足:${product.name}(当前库存 ${product.stock},需求 ${item.quantity} 件)",
          )
        }
        ValidatedOrderItem(product = product, quantity = item.quantity)
      }

    // 2. 验证地址存在
    val address =
      addressQueryApiService
        .findById(addressId)
        ?: throw NotFoundException("收货地址不存在: $addressId")

    // 3. 验证地址是否在配送范围内
    deliveryAreaQueryService.validateDeliveryAddress(address.province, address.city, address.district)

    // 4. 计算配送费(R-MON-02 楼层分档 + R-OPS-03 距离阶梯,叠加):自提免运费
    val rule = deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule()
    val deliveryFeeCents = computeDeliveryFee(rule, address, isSelfCollect)

    // 5. 生成唯一订单号
    val orderNo = orderIdGenerator.generateOrderId()

    // 6. 全部校验通过后逐商品扣减库存(原子操作;任一失败抛异常整体回滚)
    validatedItems.forEach { item ->
      val stockDecreased = productService.decreaseStock(item.product.id!!, item.quantity)
      if (!stockDecreased) {
        throw BadRequestException("库存扣减失败:${item.product.name},请重试")
      }
    }

    return OrderValidationResult(
      items = validatedItems,
      address = address,
      userId = userId,
      deliveryFeeCents = deliveryFeeCents,
      orderNo = orderNo,
      deliveryFeeRule = rule,
    )
  }

  /**
   * 起送门槛拦截(R-OPS-03/G96,服务端最终防线;客户端提交前预检为第一道):
   * 订单商品金额(成交小计,优惠前)低于启用中的门槛 → 拦截下单并提示差额;自提订单豁免。
   * 商品金额口径与确认订单页「商品金额」行、orderPreview.belowMinimum 一致。
   */
  private fun validateMinimumOrderAmount(
    rule: DeliveryFeeRuleModel,
    productAmountCents: Long,
    isSelfCollect: Boolean,
  ) {
    if (isSelfCollect) return
    if (!rule.belowMinimumOrder(productAmountCents)) return
    val minimum = rule.minimumOrderCents
    val gap = minimum - productAmountCents
    throw BadRequestException(
      String.format(
        Locale.ROOT,
        "商品金额未达起送金额 ¥%.2f，还差 ¥%.2f",
        minimum / 100.0,
        gap / 100.0,
      ),
    )
  }

  /**
   * 配送费计算(R-MON-02 楼层分档 + R-OPS-03 距离阶梯,叠加;PRD 12-operations-config.md §F-OPS-005):
   * 委托 [DeliveryFeeRuleModel.feeCentsForDelivery] 共享实现——自提免运费、免费楼层内免运费、
   * 超免费楼层按 tiers 档位命中、距离档按门店与地址直线距离命中(坐标缺失不加收),
   * 与订单预览同源,保证预览=实付;费用总额落 orders.delivery_fee_cents 快照(R-MON-02)。
   * 金额以分存储与服务端计算(R-MON-01);规则配置变更即时生效,已生成订单不受影响。
   */
  private fun computeDeliveryFee(
    rule: DeliveryFeeRuleModel,
    address: AddressModel,
    isSelfCollect: Boolean,
  ): Long = rule.feeCentsForDelivery(address.floor, address.longitude, address.latitude, isSelfCollect)

  /**
   * 保存订单的公共逻辑(含营销计价快照 R-MKT-02/07/08 与多商品明细落库 F-ORD-004)
   */
  private fun saveOrder(
    userId: Long,
    items: List<ValidatedOrderItem>,
    addressId: Long,
    orderNo: String,
    amountCents: Long,
    deliveryFeeCents: Long,
    depositCents: Long,
    paymentMethod: PaymentMethod?,
    remark: String?,
    isSelfCollect: Boolean,
    paymentTime: LocalDateTime?,
    pricing: OrderPricingResult,
    initialStatus: OrderModelStatus,
    clientRequestId: String? = null,
    deliveryTimeSlot: DeliveryTimeSlot? = null,
  ): OrderModel {
    // 构建搜索内容(多商品:全部商品名称拼接,便于后台按商品名检索)
    val searchContent =
      SearchContentBuilder.buildSearchContent(
        address = null, // address 会在后续通过 repository 查询，这里传 null
        products = items.map { it.product },
      )

    val order =
      OrderModel(
        orderNo = orderNo,
        userId = userId,
        clientRequestId = clientRequestId,
        deliveryTimeSlot = deliveryTimeSlot,
        amountCents = amountCents,
        deliveryFeeCents = deliveryFeeCents,
        // 随单押金(R-MON-03:首购规格收取,计入 payAmountCents,可退)
        depositCents = depositCents,
        unitPriceCents = pricing.unitPriceCents,
        priceSource = pricing.priceSource,
        promotionActivityId = pricing.promotionActivityId,
        promotionActivityName = pricing.promotionActivityName,
        promotionDiscountCents = pricing.promotionDiscountCents,
        couponId = pricing.selectedCouponId,
        couponName = pricing.selectedCouponName,
        couponDiscountCents = pricing.couponDiscountCents,
        ticketRedeemCount = pricing.ticketRedeemCount,
        addressId = addressId,
        status = initialStatus,
        paymentMethod = paymentMethod,
        paymentTransactionId = null,
        paymentTime = paymentTime,
        deliveryWorkerId = null,
        deliveryPhotos = null,
        completedAt = null,
        remark = remark,
        isSelfCollect = isSelfCollect,
        searchContent = searchContent,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    val savedOrder = orderRepository.save(order)

    // 多商品明细落库(F-ORD-004):商品名/规格/封面/目录价快照,配置变更不影响已生成订单
    val pricingById = pricing.items.associateBy { it.productId }
    orderItemRepository.saveAll(
      items.map { item ->
        val priced = pricingById[item.product.id]
        OrderItemModel.create(
          orderId = savedOrder.id!!,
          productId = item.product.id!!,
          quantity = item.quantity,
          unitPriceCents = priced?.unitPriceCents ?: item.product.price,
          productSnapshot =
            objectMapper.writeValueAsString(
              mapOf(
                "id" to item.product.id,
                "name" to item.product.name,
                "specification" to item.product.specification,
                "coverImageUrl" to item.product.coverImageUrl,
                "priceCents" to item.product.price,
              ),
            ),
        )
      },
    )

    // 发布订单创建事件(商品明细以 order_items 为准,事件载荷仅订单级信息)
    eventPublishService.publishOrderCreated(
      orderId = savedOrder.id!!,
      userId = savedOrder.userId,
      amountCents = savedOrder.amountCents,
    )

    return savedOrder
  }

  /**
   * 引擎消费后的权威计价结果回写订单快照列(与预检一致;异常时整体回滚)
   */
  private fun applyPricingSnapshot(
    order: OrderModel,
    pricing: OrderPricingResult,
  ) {
    order.amountCents = pricing.payableProductCents
    order.unitPriceCents = pricing.unitPriceCents
    order.priceSource = pricing.priceSource
    order.promotionActivityId = pricing.promotionActivityId
    order.promotionActivityName = pricing.promotionActivityName
    order.promotionDiscountCents = pricing.promotionDiscountCents
    order.firstOrderActivityId = pricing.firstOrderActivityId
    order.firstOrderActivityName = pricing.firstOrderActivityName
    order.firstOrderDiscountCents = pricing.firstOrderDiscountCents
    order.couponId = pricing.selectedCouponId
    order.couponName = pricing.selectedCouponName
    order.couponDiscountCents = pricing.couponDiscountCents
    order.ticketRedeemCount = pricing.ticketRedeemCount
  }

  /**
   * 赠品随单落库(R-MKT-09:附加 0 价行,随订单配送;商品名随单快照,活动配置变更不影响已生成订单)
   */
  private fun saveOrderGifts(
    orderId: Long,
    pricing: OrderPricingResult,
  ) {
    if (pricing.giftItems.isEmpty()) return
    orderGiftRepository.saveAll(
      pricing.giftItems.map { gift ->
        OrderGiftModel(
          orderId = orderId,
          giftActivityId = gift.giftActivityId,
          giftActivityName = gift.giftActivityName,
          productId = gift.productId,
          productName = gift.productName,
          quantity = gift.quantity,
        )
      },
    )
  }

  @Transactional
  override fun createOrder(input: CreateOrderRequest): OrderModel {
    // 服务端下单幂等(R-ORD-06):同一用户同一 clientRequestId 已下单则直接返回已存在订单,
    // 不重复扣库存/核销权益;(user_id, client_request_id) 唯一索引为并发重复提交的最终防线
    // (Snowflake 预分配 ID 下唯一冲突在提交时抛出,请求失败但不产生重复订单)。
    input.clientRequestId?.let { requestId ->
      orderRepository.findByUserIdAndClientRequestId(input.userId, requestId)?.let { return it }
    }

    val pricingRequest =
      OrderPricingRequest(
        userId = input.userId,
        items = input.items.map { OrderPricingItemRequest(productId = it.productId, quantity = it.quantity) },
        isSelfCollect = false,
        preferredCouponId = input.preferredCouponId,
        ticketRedeemCount = input.ticketRedeemCount,
      )
    // 1. 促销引擎计价预检(纯计算;金额一律服务端计算 R-MON-01,前端不传价格)
    val preview = promotionPricingService.preview(pricingRequest)

    // 2. 校验库存/地址/配送范围/配送费
    val validation =
      validateAndPrepareOrder(
        userId = input.userId,
        items = input.items,
        addressId = input.addressId,
        isSelfCollect = false,
      )

    // 3. 验证地址属于当前用户
    if (validation.address.userId != validation.userId) {
      throw BadRequestException("无权使用此收货地址")
    }

    // 3.2 起送门槛拦截(R-OPS-03 服务端最终防线;在线下单恒为配送单)
    validateMinimumOrderAmount(validation.deliveryFeeRule, preview.subtotalCents, isSelfCollect = false)

    // 3.5 押金试算(R-MON-03 按规格收取;与订单预览同源,金额服务端计算 R-MON-01)
    val deposit =
      depositMutationApiService.calcOrderDeposit(
        userId = validation.userId,
        items = validation.items.map { OrderDepositItem(productId = it.product.id!!, quantity = it.quantity) },
      )

    // 4. 保存订单(按预检结果快照,明细落 order_items;幂等键与配送时段随单落库)
    val savedOrder =
      saveOrder(
        userId = validation.userId,
        items = validation.items,
        addressId = validation.address.id!!,
        orderNo = validation.orderNo,
        amountCents = preview.payableProductCents,
        deliveryFeeCents = validation.deliveryFeeCents,
        depositCents = deposit.totalCents,
        paymentMethod = null,
        remark = input.remark,
        isSelfCollect = false,
        paymentTime = null,
        pricing = preview,
        // 用户在线下单进入「待支付」(G09/R-ORD-01:支付成功后才进入「待派单」)
        initialStatus = OrderModelStatus.PENDING_PAYMENT,
        clientRequestId = input.clientRequestId,
        // 配送时段(AC-ORD-001-05):缺省=立即配送
        deliveryTimeSlot = input.deliveryTimeSlot ?: DeliveryTimeSlot.IMMEDIATE,
      )

    // 4.5 随单建押金账(PENDING;支付成功回调置 HELD,取消/超时清账)
    depositMutationApiService.createPendingDeposits(savedOrder.id!!, validation.userId, deposit)

    // 5. 引擎消费(同事务):原子核销水票/优惠券;实际核销数 < 预检 → 抛异常整体回滚
    val finalPricing = promotionPricingService.applyForOrder(savedOrder.id!!, pricingRequest)
    applyPricingSnapshot(savedOrder, finalPricing)
    saveOrderGifts(savedOrder.id!!, finalPricing)
    val result = orderRepository.save(savedOrder)

    // 业务日志(F-LOG-003 ORDER 域):记录下单
    bizLogger.logOrderOperation(
      orderId = result.id!!,
      operation = "ORDER_CREATED",
      result = "SUCCESS",
      userId = result.userId,
      additionalData =
        mapOf(
          "itemCount" to result.orderItemCount(),
          "productIds" to result.orderItemIds().joinToString(","),
          "amountCents" to result.amountCents,
        ),
    )
    return result
  }

  @Transactional
  override fun createDeliveryOrder(
    adminId: Long,
    input: CreateOrderRequest,
  ): OrderModel {
    // 代客下单归属(G51,PRD 04-order.md §F-ORD-012):订单 userId 取收货地址所属客户,
    // 而非送水工本人 —— 熟客分组价按客户解析(R-MKT-01①)且订单归属/统计口径正确
    val address =
      addressQueryApiService.findById(input.addressId)
        ?: throw NotFoundException("收货地址不存在: ${input.addressId}")
    val userId = address.userId

    val isSelfCollect = input.isSelfCollect ?: false
    // 代客下单:默认禁用自动营销优惠(满减/券),水票核销不适用(R-MKT-07 边界场景)
    val pricingRequest =
      OrderPricingRequest(
        userId = userId,
        items = input.items.map { OrderPricingItemRequest(productId = it.productId, quantity = it.quantity) },
        isSelfCollect = isSelfCollect,
        preferredCouponId = 0L,
        ticketRedeemCount = 0,
      )
    val preview = promotionPricingService.preview(pricingRequest)

    val validation =
      validateAndPrepareOrder(
        userId = userId,
        items = input.items,
        addressId = input.addressId,
        isSelfCollect = isSelfCollect,
      )

    // 起送门槛拦截(R-OPS-03;代客配送单同口径,自提单豁免)
    validateMinimumOrderAmount(validation.deliveryFeeRule, preview.subtotalCents, isSelfCollect)

    // 押金试算(R-MON-03:代客下单同口径按客户 userId 判定在租只收一次)
    val deposit =
      depositMutationApiService.calcOrderDeposit(
        userId = userId,
        items = validation.items.map { OrderDepositItem(productId = it.product.id!!, quantity = it.quantity) },
      )

    val savedOrder =
      saveOrder(
        userId = validation.userId,
        items = validation.items,
        addressId = validation.address.id!!,
        orderNo = validation.orderNo,
        amountCents = preview.payableProductCents,
        deliveryFeeCents = validation.deliveryFeeCents,
        depositCents = deposit.totalCents,
        paymentMethod = PaymentMethod.CASH,
        remark = input.remark,
        isSelfCollect = isSelfCollect,
        paymentTime = if (isSelfCollect) LocalDateTime.now() else null,
        pricing = preview,
        // 代客下单现场收款(现金/水票):直接进入「待分配」(R-PAY-03)
        initialStatus = OrderModelStatus.PENDING_DISPATCH,
      )

    // 随单建押金账并即时确认实收(代客下单确认即置 HELD,design03 §9)
    depositMutationApiService.createPendingDeposits(savedOrder.id!!, validation.userId, deposit)
    depositMutationApiService.markHeldByOrder(savedOrder.id!!)

    val finalPricing = promotionPricingService.applyForOrder(savedOrder.id!!, pricingRequest)
    applyPricingSnapshot(savedOrder, finalPricing)
    saveOrderGifts(savedOrder.id!!, finalPricing)
    val result = orderRepository.save(savedOrder)

    // 业务日志(F-LOG-003 ORDER 域):代客下单(管理员/配送员)
    bizLogger.logOrderOperation(
      orderId = result.id!!,
      operation = "ORDER_CREATED",
      result = "SUCCESS",
      userId = result.userId,
      additionalData =
        mapOf(
          "itemCount" to result.orderItemCount(),
          "productIds" to result.orderItemIds().joinToString(","),
          "amountCents" to result.amountCents,
          "isSelfCollect" to result.isSelfCollect,
          "operatorAdminId" to adminId,
        ),
    )
    return result
  }

  /**
   * 逐商品恢复库存(F-ORD-004 多商品:按 order_items 明细逐项恢复)
   */
  private fun restoreStock(order: OrderModel) {
    orderItemRepository.findByOrderId(order.id!!).forEach { item ->
      productService.increaseStock(item.productId, item.quantity)
    }
  }

  @Transactional
  override fun cancelOrder(orderId: Long): OrderModel {
    val order = orderQueryService.getOrderById(orderId)

    // 1. 验证订单状态是否可以取消(快照校验,快速失败给出准确提示;原子认领兜底并发)
    if (order.status == OrderModelStatus.CANCELLED) {
      throw BadRequestException("订单已取消")
    }

    if (order.status == OrderModelStatus.REFUNDED) {
      throw BadRequestException("订单已退款，无法取消")
    }

    if (order.status == OrderModelStatus.COMPLETED) {
      throw BadRequestException("订单已完成，无法取消")
    }

    if (order.status == OrderModelStatus.DELIVERING) {
      throw BadRequestException("订单配送中，无法取消")
    }

    // 2. 如果订单已支付，需要退款（先处理库存恢复，退款在支付服务中处理）
    // 已支付订单位于「待派单」(支付成功回调 R-ORD-01 状态机);兼容历史「待配送」已支付数据
    var shouldRefund = false
    if (
      (order.status == OrderModelStatus.PENDING_DISPATCH || order.status == OrderModelStatus.PENDING_DELIVERY) &&
      order.paymentTransactionId != null
    ) {
      shouldRefund = true
    }

    // 3. 原子认领取消(订单级乐观锁 CAS,R-ORD-04 互斥):
    //    与支付超时/并发取消竞争时仅一方认领成功,保证库存只恢复一次;
    //    0 行 = 已被并发取消/支付超时处理或状态已流转(如支付回调)
    val claimed =
      orderRepository.cancelAtomically(
        orderId,
        CANCELABLE_STATUSES,
        LocalDateTime.now(),
      )
    if (claimed == 0) {
      val latest = orderQueryService.getOrderById(orderId)
      throw BadRequestException(
        if (latest.status == OrderModelStatus.CANCELLED) "订单已取消" else "订单状态已变更,请刷新后重试",
      )
    }

    // 4. 恢复库存(逐商品明细 R-ORD-02;仅认领成功方执行,并发下不重复恢复)
    restoreStock(order)

    // 4.5 押金清账(R-MON-03):
    //   未实收(待支付)→ 软删 PENDING 清账;
    //   现场收款(无微信交易号,代客单实收确认 HELD)→ 取消即线下退款,HELD→REFUNDED;
    //   微信已付 → 退款审核经 RefundReviewService 联动 HELD→REFUNDED(资金实际退回时)
    if (order.status == OrderModelStatus.PENDING_PAYMENT) {
      depositMutationApiService.cancelPendingByOrder(orderId)
    } else if (order.paymentTransactionId == null) {
      depositMutationApiService.markRefundedByOrder(orderId, bizLogger.resolveOperator().first, "订单取消(现场收款)退还")
    }

    // 5. 退回营销权益(同事务,幂等):已核销券/水票未过期→可复用,过期→作废(R-MKT-03/08)
    returnCouponAndTickets(orderId)

    // 6. 发布订单取消事件
    val cancelDescription = if (shouldRefund) "用户取消订单（需退款）" else "用户取消订单"
    eventPublishService.publishOrderCancelled(
      orderId = orderId,
      userId = order.userId,
      reason = cancelDescription,
      shouldRefund = shouldRefund,
      paymentTransactionId = if (shouldRefund) order.paymentTransactionId else null,
    )

    // 业务日志(F-LOG-003 ORDER 域):记录取消
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logOrderOperation(
      orderId = orderId,
      operation = "ORDER_CANCELLED",
      result = "SUCCESS",
      userId = operatorId ?: order.userId,
      username = operatorName,
      additionalData = mapOf("reason" to cancelDescription, "refund" to shouldRefund),
    )

    // 返回最新状态(CAS 已置 CANCELLED;clearAutomatically 后重新读取,避免返回过期快照)
    return orderQueryService.getOrderById(orderId)
  }

  @Transactional
  override fun cancelOrder(
    orderId: Long,
    userId: Long,
  ): OrderModel? {
    val order = orderRepository.findById(orderId).orElse(null) ?: return null

    // Verify order belongs to user
    if (order.userId != userId) {
      return null
    }

    return cancelOrder(orderId)
  }

  @Transactional
  override fun cancelOrderForAdmin(orderId: Long): OrderModel? =
    try {
      cancelOrder(orderId)
    } catch (e: Exception) {
      null
    }

  /**
   * 用户催单(F-ORD-008,AC-ORD-008-01):仅本人「配送中」订单可催;
   * 防抖 —— 5 分钟内重复催单仅一次生效(操作时间线 ORDER_URGING 为准);
   * 记录操作时间线并发布 ORDER_URGING 事件(通知域据此推送用户「催单已受理」+ 送水工「用户催单」)。
   */
  @Transactional
  override fun urgeOrder(
    orderId: Long,
    userId: Long,
  ): OrderModel? {
    val order = orderRepository.findById(orderId).orElse(null) ?: return null
    if (order.userId != userId) {
      return null
    }
    if (order.status != OrderModelStatus.DELIVERING) {
      throw BadRequestException("仅配送中的订单可催单")
    }
    val latestUrge =
      orderOperationService
        .getOrderOperations(orderId)
        .firstOrNull { it.operationType == OrderOperationType.ORDER_URGING }
    if (latestUrge != null && latestUrge.createdAt.isAfter(LocalDateTime.now().minusMinutes(URGE_DEBOUNCE_MINUTES))) {
      throw BadRequestException("催单过于频繁,请稍后再试")
    }

    orderOperationService.recordOperation(
      orderId = orderId,
      operationType = OrderOperationType.ORDER_URGING,
      operatorType = OperatorType.USER,
      operatorId = userId,
      description = "用户催单",
    )
    eventPublishService.publishDomainEvent(
      eventType = DomainEventType.ORDER_URGING.value,
      aggregateId = orderId.toString(),
      eventData =
        buildMap<String, Any> {
          put("orderId", orderId)
          put("userId", userId)
          order.deliveryWorkerId?.let { put("deliveryWorkerId", it) }
          put("timestamp", System.currentTimeMillis())
        },
    )
    return order
  }

  @Transactional
  override fun updateOrderStatus(
    orderId: Long,
    status: OrderModelStatus,
  ): OrderModel {
    val order = orderQueryService.getOrderById(orderId)
    val beforeStatus = order.status
    validateTransition(beforeStatus, status)
    // 状态谓词条件更新(裁决结构 #9 ①):WHERE id + 读到的当前状态,0 行 = 并发已流转,
    // 抛状态冲突而非覆盖他人写入(原读-改-存无守卫,存在丢失更新窗口)
    val updated =
      orderRepository.updateStatusAtomically(
        id = orderId,
        from = beforeStatus,
        to = status,
        now = LocalDateTime.now(),
      )
    if (updated == 0) {
      throw BadRequestException(
        "订单状态冲突:当前状态已被其他操作变更,无法从 ${beforeStatus.name} 流转为 ${status.name},请刷新后重试",
      )
    }
    order.status = status
    val saved = order

    // 业务日志(F-LOG-003 ORDER 域):记录状态流转
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logOrderOperation(
      orderId = saved.id!!,
      operation = "ORDER_STATUS_CHANGED",
      result = "SUCCESS",
      userId = operatorId ?: saved.userId,
      username = operatorName,
      additionalData = mapOf("from" to beforeStatus.name, "to" to status.name),
    )
    return saved
  }

  override fun updateOrder(
    orderId: Long,
    orderDTO: OrderModel,
  ): OrderModel {
    orderDTO.id = orderId
    return orderRepository.save(orderDTO)
  }

  /**
   * 状态流转表校验(R-ORD-01,PRD 02-business-flows.md §订单状态机):
   * updateOrderStatus 为后台兜底接口,仅允许流转表中的合法流转;
   * 其余触发动作(支付回调/派单/抢单/开始配送/完成配送/取消)走各自专用接口,不在此放开。
   * 非法流转直接拒绝,防止任意改状态破坏履约链路。
   */
  private fun validateTransition(
    from: OrderModelStatus,
    to: OrderModelStatus,
  ) {
    val allowed =
      when (from) {
        // 用户取消(未支付)/商家取消/支付超时
        OrderModelStatus.PENDING_PAYMENT -> setOf(OrderModelStatus.CANCELLED)
        // 用户/管理员取消(已支付) → 已取消(触发退款流程);退款审核通过 → 已退款
        OrderModelStatus.PENDING_DISPATCH -> setOf(OrderModelStatus.CANCELLED)
        OrderModelStatus.CANCELLED -> setOf(OrderModelStatus.REFUNDED)
        else -> emptySet()
      }
    if (to !in allowed) {
      throw BadRequestException("非法订单状态流转: ${from.name} → ${to.name}")
    }
  }

  @Transactional
  override fun handlePaymentSuccess(
    orderId: Long,
    paymentTransactionId: String,
  ) {
    val order = orderQueryService.getOrderById(orderId)

    if (order.status != OrderModelStatus.PENDING_PAYMENT) {
      throw BadRequestException("订单状态不正确，无法处理支付")
    }

    // 支付成功回调:待支付 → 待派单(PRD 02-business-flows.md §订单状态机,
    // R-ORD-01 差异①/G09:在线支付订单进入「待派单」,可抢单也可后台派单)
    order.status = OrderModelStatus.PENDING_DISPATCH
    order.paymentTransactionId = paymentTransactionId
    order.paymentTime = LocalDateTime.now()
    order.paymentMethod = PaymentMethod.WECHAT_PAY

    val updatedOrder = orderRepository.save(order)

    // 押金实收确认(R-MON-03:随单 PENDING → HELD,在租生效)
    depositMutationApiService.markHeldByOrder(orderId)

    // 发布支付成功事件(商品明细以 order_items 为准,事件载荷仅订单级信息)
    eventPublishService.publishOrderPaid(
      orderId = updatedOrder.id!!,
      userId = updatedOrder.userId,
      amountCents = updatedOrder.amountCents,
    )

    // 业务日志(F-LOG-003 ORDER 域):订单已支付
    bizLogger.logOrderOperation(
      orderId = updatedOrder.id!!,
      operation = "ORDER_PAID",
      result = "SUCCESS",
      userId = updatedOrder.userId,
      additionalData = mapOf("paymentTransactionId" to paymentTransactionId),
    )
  }

  @Transactional
  override fun handlePaymentTimeout(orderId: Long) {
    val order = orderQueryService.getOrderById(orderId)

    if (order.status != OrderModelStatus.PENDING_PAYMENT) {
      return // 已处理过，跳过
    }

    // 原子认领超时取消(订单级乐观锁 CAS,R-ORD-04 互斥):
    // 与用户/管理员手动取消并发时仅一方认领成功,保证库存只恢复一次;
    // 0 行 = 已被并发取消或回调已流转,幂等跳过
    val claimed =
      orderRepository.cancelAtomically(
        orderId,
        listOf(OrderModelStatus.PENDING_PAYMENT),
        LocalDateTime.now(),
      )
    if (claimed == 0) {
      return // 已处理过(并发取消/回调),跳过
    }

    // 恢复库存(逐商品明细;仅认领成功方执行,并发下不重复恢复)
    restoreStock(order)

    // 押金清账(未实收:软删 PENDING,R-MON-03)
    depositMutationApiService.cancelPendingByOrder(orderId)

    // 退回营销权益(幂等)
    returnCouponAndTickets(orderId)

    // 发布支付超时事件
    eventPublishService.publishPaymentTimeout(
      orderId = order.id!!,
      userId = order.userId,
    )

    // 业务日志(F-LOG-003 ORDER 域):支付超时自动取消
    bizLogger.logOrderOperation(
      orderId = order.id!!,
      operation = "ORDER_CANCELLED",
      result = "SUCCESS",
      userId = order.userId,
      additionalData = mapOf("reason" to "PAYMENT_TIMEOUT"),
    )
  }

  override fun confirmOrderPreview(input: OrderPricingRequest): OrderPricingResult = promotionPricingService.preview(input)

  /**
   * 退回营销权益(取消/支付超时,幂等):
   * 已核销券/水票未过期→RETURNED 可复用,过期→EXPIRED(R-MKT-03/08)。
   */
  private fun returnCouponAndTickets(orderId: Long) {
    val returnedCoupons = couponMutationApiService.returnCouponsByOrder(orderId)
    val returnedTickets = ticketMutationApiService.returnTicketsByOrder(orderId)
    if (returnedCoupons > 0 || returnedTickets > 0) {
      logger.info("Order {} cancelled: returned {} coupons, {} tickets", orderId, returnedCoupons, returnedTickets)
    }
  }

  /** 订单商品明细数(日志用) */
  private fun OrderModel.orderItemCount(): Int = orderItemRepository.findByOrderId(id!!).size

  /** 订单商品 ID 列表(日志用) */
  private fun OrderModel.orderItemIds(): List<Long> = orderItemRepository.findByOrderId(id!!).map { it.productId }

  companion object {
    /** 催单防抖窗口(分钟):短时间重复催单仅一次生效(AC-ORD-008-01) */
    private const val URGE_DEBOUNCE_MINUTES = 5L

    /** 可取消状态(用户/管理员取消,乐观锁 CAS 认领范围,R-ORD-04 互斥) */
    private val CANCELABLE_STATUSES =
      listOf(
        OrderModelStatus.PENDING_PAYMENT,
        OrderModelStatus.PENDING_DISPATCH,
        OrderModelStatus.PENDING_DELIVERY,
      )
  }
}
