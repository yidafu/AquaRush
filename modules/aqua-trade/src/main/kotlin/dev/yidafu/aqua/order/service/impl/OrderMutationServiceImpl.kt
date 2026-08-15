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
import dev.yidafu.aqua.api.dto.OrderItemRequest
import dev.yidafu.aqua.api.dto.OrderPricingItemRequest
import dev.yidafu.aqua.api.dto.OrderPricingRequest
import dev.yidafu.aqua.api.dto.OrderPricingResult
import dev.yidafu.aqua.api.service.delivery.DeliveryAreaQueryService
import dev.yidafu.aqua.api.service.marketing.CouponMutationApiService
import dev.yidafu.aqua.api.service.marketing.PromotionPricingService
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.ops.DeliveryFeeRuleQueryApiService
import dev.yidafu.aqua.api.service.order.OrderIdGeneratorService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.product.ProductService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.OrderGiftModel
import dev.yidafu.aqua.common.domain.model.OrderItemModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.common.utils.SearchContentBuilder
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.order.domain.repository.OrderGiftRepository
import dev.yidafu.aqua.order.domain.repository.OrderItemRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

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

    // 4. 计算配送费(楼层口径 R-MON-02):免费楼层内免运费,超过免费楼层一次性加收;自提免运费
    val deliveryFeeCents = computeDeliveryFee(address.floor, isSelfCollect)

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
    )
  }

  /**
   * 配送费计算(R-MON-02 楼层口径,PRD 12-operations-config.md §F-OPS-005):
   * - 自提订单免运费(无配送行为,楼层不参与判定);
   * - 收货楼层为空(平房/无楼层信息)免运费;
   * - 楼层 > 免费楼层(默认4)时一次性加收 surchargeCents(默认2元),不随层数累加。
   * 金额以分存储与服务端计算(R-MON-01);规则配置变更即时生效,已生成订单不受影响(运费按下单时快照)。
   */
  private fun computeDeliveryFee(
    floor: Int?,
    isSelfCollect: Boolean,
  ): Long {
    if (isSelfCollect) {
      return 0
    }
    val rule = deliveryFeeRuleQueryApiService.getEffectiveDeliveryFeeRule()
    if (floor == null || floor <= rule.freeFloorLimit) {
      return 0
    }
    return rule.surchargeCents
  }

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
    paymentMethod: PaymentMethod?,
    remark: String?,
    isSelfCollect: Boolean,
    paymentTime: LocalDateTime?,
    pricing: OrderPricingResult,
    initialStatus: OrderModelStatus,
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
        amountCents = amountCents,
        deliveryFeeCents = deliveryFeeCents,
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

    // 4. 保存订单(按预检结果快照,明细落 order_items)
    val savedOrder =
      saveOrder(
        userId = validation.userId,
        items = validation.items,
        addressId = validation.address.id!!,
        orderNo = validation.orderNo,
        amountCents = preview.payableProductCents,
        deliveryFeeCents = validation.deliveryFeeCents,
        paymentMethod = null,
        remark = input.remark,
        isSelfCollect = false,
        paymentTime = null,
        pricing = preview,
        // 用户在线下单进入「待支付」(G09/R-ORD-01:支付成功后才进入「待派单」)
        initialStatus = OrderModelStatus.PENDING_PAYMENT,
      )

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

    val savedOrder =
      saveOrder(
        userId = validation.userId,
        items = validation.items,
        addressId = validation.address.id!!,
        orderNo = validation.orderNo,
        amountCents = preview.payableProductCents,
        deliveryFeeCents = validation.deliveryFeeCents,
        paymentMethod = PaymentMethod.CASH,
        remark = input.remark,
        isSelfCollect = isSelfCollect,
        paymentTime = if (isSelfCollect) LocalDateTime.now() else null,
        pricing = preview,
        // 代客下单现场收款(现金/水票):直接进入「待分配」(R-PAY-03)
        initialStatus = OrderModelStatus.PENDING_DISPATCH,
      )

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

    // 1. 验证订单状态是否可以取消
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

    // 3. 恢复库存(逐商品明细 R-ORD-02;未配送且已支付或待支付时恢复)
    if (order.status != OrderModelStatus.DELIVERING && order.status != OrderModelStatus.COMPLETED) {
      restoreStock(order)
    }

    // 4. 更新订单状态
    order.status = OrderModelStatus.CANCELLED
    val cancelledOrder = orderRepository.save(order)

    // 5. 退回营销权益(同事务,幂等):已核销券/水票未过期→可复用,过期→作废(R-MKT-03/08)
    returnCouponAndTickets(cancelledOrder.id!!)

    // 6. 发布订单取消事件
    val cancelDescription = if (shouldRefund) "用户取消订单（需退款）" else "用户取消订单"
    eventPublishService.publishOrderCancelled(
      orderId = cancelledOrder.id!!,
      userId = cancelledOrder.userId,
      reason = cancelDescription,
      shouldRefund = shouldRefund,
      paymentTransactionId = if (shouldRefund) cancelledOrder.paymentTransactionId else null,
    )

    // 业务日志(F-LOG-003 ORDER 域):记录取消
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logOrderOperation(
      orderId = cancelledOrder.id!!,
      operation = "ORDER_CANCELLED",
      result = "SUCCESS",
      userId = operatorId ?: cancelledOrder.userId,
      username = operatorName,
      additionalData = mapOf("reason" to cancelDescription, "refund" to shouldRefund),
    )

    return cancelledOrder
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

  @Transactional
  override fun updateOrderStatus(
    orderId: Long,
    status: OrderModelStatus,
  ): OrderModel {
    val order = orderQueryService.getOrderById(orderId)
    val beforeStatus = order.status
    validateTransition(beforeStatus, status)
    order.status = status
    val saved = orderRepository.save(order)

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

    // 恢复库存(逐商品明细)
    restoreStock(order)

    // 取消订单
    order.status = OrderModelStatus.CANCELLED
    orderRepository.save(order)

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
}
