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

package dev.yidafu.aqua.payment.service.impl

import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseMutationService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseQueryService
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.payment.domain.repository.PaymentRefundRepository
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import dev.yidafu.aqua.payment.service.WechatPayGateway
import dev.yidafu.aqua.payment.service.WechatPrepayParams
import dev.yidafu.aqua.payment.service.WechatTransaction
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.beans.factory.ObjectProvider
import java.time.LocalDateTime
import java.util.Optional
import java.util.function.Consumer

/**
 * 支付服务单元测试(PRD 04-order.md §F-ORD-005/R-PAY-01~03):
 * 覆盖假支付自动完成(订单/购票)、真实模式预支付与退款、回调验签/金额校验/幂等/取消不流转。
 */
class PaymentServiceImplTest :
  FunSpec({
    var orderQueryService: OrderQueryService = mockk()
    var orderQueryApiService: OrderQueryApiService = mockk()
    var orderMutationService: OrderMutationService = mockk()
    var ticketQueryApiService: TicketQueryApiService = mockk()
    var ticketMutationApiService: TicketMutationApiService = mockk()
    var barrelPurchaseQueryService: BarrelPurchaseQueryService = mockk()
    var barrelPurchaseMutationService: BarrelPurchaseMutationService = mockk()
    var paymentRepository: PaymentRepository = mockk()
    var paymentRefundRepository: PaymentRefundRepository = mockk()
    var wechatPayGateway: WechatPayGateway = mockk()
    var bizLogger: BizLogger = mockk()
    var eventPublishService: SimplifiedEventPublishService = mockk()
    var eventPublishServiceProvider: ObjectProvider<SimplifiedEventPublishService> = mockk()
    var service: PaymentServiceImpl = mockk()

    fun order(
      id: Long = 1L,
      status: OrderModelStatus = OrderModelStatus.PENDING_PAYMENT,
      orderNo: String = "ORD-TEST-001",
      amountCents: Long = 1000,
      deliveryFeeCents: Long = 200,
      transactionId: String? = null,
      paymentTime: LocalDateTime? = null,
    ) = OrderModel(
      id = id,
      orderNo = orderNo,
      userId = 100L,
      amountCents = amountCents,
      deliveryFeeCents = deliveryFeeCents,
      status = status,
      paymentTransactionId = transactionId,
      paymentTime = paymentTime,
    )

    fun ticketPurchase(orderNo: String = "TP2025-001") =
      TicketPurchaseModel(
        id = 1L,
        userId = 100L,
        ticketTypeId = 1L,
        orderNo = orderNo,
        packageCount = 1,
        packagePriceCents = 10000,
        paymentStatus = TicketPurchaseStatus.PENDING,
      )

    fun barrelPurchase(orderNo: String = "BK2025-001") =
      BarrelPurchaseModel(
        id = 1L,
        userId = 100L,
        orderNo = orderNo,
        totalAmountCents = 4000,
        paymentStatus = BarrelPaymentStatus.PENDING,
      )

    fun callbackTransaction(
      orderNo: String = "ORD-TEST-001",
      transactionId: String = "WX-TXN-001",
      amountCents: Long = 1200,
    ) = WechatTransaction(outTradeNo = orderNo, transactionId = transactionId, tradeState = "SUCCESS", amountCents = amountCents)

    // ================================================================
    // 假支付模式(测试环境,默认开启):发起支付即自动完成
    // ================================================================

    // ================================================================
    // 真实模式:微信预支付/查询/退款
    // ================================================================

    // ================================================================
    // 回调处理(R-PAY-01:验签/金额校验/幂等)
    // ================================================================

    // ================================================================
    // 支付超时
    // ================================================================

    beforeTest {
      orderQueryService = mockk(relaxed = true)
      orderQueryApiService = mockk(relaxed = true)
      orderMutationService = mockk(relaxed = true)
      ticketQueryApiService = mockk(relaxed = true)
      ticketMutationApiService = mockk(relaxed = true)
      barrelPurchaseQueryService = mockk(relaxed = true)
      barrelPurchaseMutationService = mockk(relaxed = true)
      paymentRepository = mockk(relaxed = true)
      paymentRefundRepository = mockk(relaxed = true)
      wechatPayGateway = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      eventPublishService = mockk(relaxed = true)
      eventPublishServiceProvider = mockk(relaxed = true)
      // ObjectProvider.ifAvailable:存在 bean 时调用消费者(upsertPendingPayment 发布延迟消息)
      every { eventPublishServiceProvider.ifAvailable(any()) } answers {
        firstArg<Consumer<SimplifiedEventPublishService>>().accept(eventPublishService)
      }
      service =
        PaymentServiceImpl(
          orderQueryService,
          orderQueryApiService,
          orderMutationService,
          ticketQueryApiService,
          ticketMutationApiService,
          barrelPurchaseQueryService,
          barrelPurchaseMutationService,
          paymentRepository,
          paymentRefundRepository,
          wechatPayGateway,
          bizLogger,
          eventPublishServiceProvider,
        )
      // JpaRepository.save 返回泛型 S,MockK relaxed 无法构造实体 → 显式回传入参
      every { paymentRepository.save(any()) } answers { firstArg() }
      every { paymentRefundRepository.save(any()) } answers { firstArg() }
    }
    test("mockModeAutoCompletesOrderPayment") {
      every { orderQueryService.getOrderById(1L) } returns order()
      every { paymentRepository.findByOrderId(1L) } returns Optional.empty()

      val result = service.createWechatJsapiPay(1L, 1200, "桶装水订单", "openid-1")

      withClue("应返回 mock 标记") { (result["mock"] == true) shouldBe true }
      withClue("应返回已支付标记") { (result["paid"] == true) shouldBe true }
      result["transactionId"] shouldBe "MOCK-ORD-TEST-001"
      verify(exactly = 1) { orderMutationService.handlePaymentSuccess(1L, "MOCK-ORD-TEST-001") }
    }
    test("mockModeAutoCompletesTicketPurchasePayment") {
      every { ticketQueryApiService.findPurchaseById(1L) } returns ticketPurchase()

      val result = service.createWechatJsapiPayForTicket(1L, "TP2025-001", 10000, "桶水票", "openid-1")

      (result["mock"] == true) shouldBe true
      verify(exactly = 1) { ticketMutationApiService.handleTicketPurchasePaid(1L, "MOCK-TP2025-001") }
    }
    test("mockModeAutoCompletesBarrelPurchasePayment (G06)") {
      every { barrelPurchaseQueryService.getById(1L) } returns barrelPurchase()

      val result = service.createWechatJsapiPayForBarrelPurchase(1L, "BK2025-001", 4000, "水桶费", "openid-1")

      (result["mock"] == true) shouldBe true
      verify(exactly = 1) { barrelPurchaseMutationService.handleBarrelPurchasePaid(1L, "MOCK-BK2025-001") }
    }
    test("mockModeRejectsAmountMismatch") {
      every { orderQueryService.getOrderById(1L) } returns order()

      shouldThrow<BadRequestException> { service.createWechatJsapiPay(1L, 999, "桶装水订单", "openid-1") }
      verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
    }
    test("mockModeRejectsNonPendingOrder") {
      every { orderQueryService.getOrderById(1L) } returns order(status = OrderModelStatus.DELIVERING)

      shouldThrow<BadRequestException> { service.createWechatJsapiPay(1L, 1200, "桶装水订单", "openid-1") }
    }
    test("realModePrepayReturnsPaymentParams") {
      service.mockEnabled = false
      every { orderQueryService.getOrderById(1L) } returns order()
      // 首次查询无支付记录(新建),prepay 成功后二次查询命中(回填 prepayId)
      val savedPayment = PaymentModel(orderId = 1L, userId = 100L, amount = 1200)
      every { paymentRepository.findByOrderId(1L) } returnsMany listOf(Optional.empty(), Optional.of(savedPayment))
      every { wechatPayGateway.isConfigured } returns true
      every { wechatPayGateway.prepayWithRequestPayment(any()) } returns
        WechatPrepayParams(
          appId = "wx-app",
          timeStamp = "1750000000",
          nonceStr = "nonce",
          packageVal = "prepay_id=wx123",
          signType = "RSA",
          paySign = "sign",
        )
      every { paymentRepository.save(any()) } answers { firstArg() }

      val result = service.createWechatJsapiPay(1L, 1200, "桶装水订单", "openid-1")

      result["appId"] shouldBe "wx-app"
      result["package"] shouldBe "prepay_id=wx123"
      result["signType"] shouldBe "RSA"
      verify { paymentRepository.save(match { it.prepayId == "wx123" }) }
      verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
    }
    test("realModeWithoutConfigThrowsNotImplementedError") {
      service.mockEnabled = false
      every { orderQueryService.getOrderById(1L) } returns order()
      every { paymentRepository.findByOrderId(1L) } returns Optional.empty()
      every { wechatPayGateway.isConfigured } returns false

      shouldThrow<NotImplementedError> { service.createWechatJsapiPay(1L, 1200, "桶装水订单", "openid-1") }
    }
    test("realModeRefundCallsGateway") {
      service.mockEnabled = false
      val payment = PaymentModel(id = 9L, orderId = 1L, userId = 100L, transactionId = "WX-TXN-001", amount = 1200)
      every { paymentRepository.findByTransactionId("WX-TXN-001") } returns Optional.of(payment)
      every { orderQueryService.getOrderById(1L) } returns order()
      every { wechatPayGateway.isConfigured } returns true
      every { wechatPayGateway.createRefund(any(), "WX-TXN-001", 1000, 1200, any()) } returns "REFUND-001"
      every { paymentRepository.save(any()) } answers { firstArg() }

      val result = service.refund("WX-TXN-001", 1000, 1200, "订单取消退款")

      result["refundId"] shouldBe "REFUND-001"
      verify { paymentRepository.save(match { it.status.name == "REFUNDED" }) }
      // R-PAY-02:退款记录落库(status=COMPLETED),供退款/净营收统计聚合
      verify {
        paymentRefundRepository.save(
          match {
            it.status == RefundStatus.COMPLETED &&
              it.refundAmount == 1000L &&
              it.orderId == 1L &&
              it.userId == 100L &&
              it.refundId == "REFUND-001"
          },
        )
      }
    }
    test("mockModeRefundSucceeds") {
      every { paymentRepository.findByTransactionId(any()) } returns Optional.empty()

      val result = service.refund("MOCK-ORD-TEST-001", 1000, 1200, "订单取消退款")

      (result["mock"] == true) shouldBe true
      (result["refundId"] as String).startsWith("MOCK-REFUND-") shouldBe true
      // 未命中支付记录:退款照常执行,但跳过退款记录落库(统计不计该笔)
      verify(exactly = 0) { paymentRefundRepository.save(any()) }
    }
    test("mockModeRefundPersistsRefundRecord") {
      val payment = PaymentModel(id = 9L, orderId = 1L, userId = 100L, transactionId = "MOCK-ORD-TEST-001", amount = 1200)
      every { paymentRepository.findByTransactionId("MOCK-ORD-TEST-001") } returns Optional.of(payment)
      every { orderQueryService.getOrderById(1L) } returns order()

      val result = service.refund("MOCK-ORD-TEST-001", 1000, 1200, "订单取消退款")

      (result["mock"] == true) shouldBe true
      verify { paymentRepository.save(match { it.status.name == "REFUNDED" }) }
      verify {
        paymentRefundRepository.save(
          match {
            it.status == RefundStatus.COMPLETED &&
              it.refundAmount == 1000L &&
              it.orderId == 1L &&
              it.userId == 100L &&
              (it.refundId as String).startsWith("MOCK-REFUND-")
          },
        )
      }
    }
    test("queryPaymentStatusDerivesFromOrder") {
      every { orderQueryService.getOrderById(1L) } returns order(status = OrderModelStatus.PENDING_DELIVERY)
      every { orderQueryService.getOrderById(2L) } returns order(id = 2L, status = OrderModelStatus.CANCELLED)
      every { orderQueryService.getOrderById(3L) } returns order(id = 3L, status = OrderModelStatus.PENDING_PAYMENT)

      service.queryPaymentStatus(1L) shouldBe "SUCCESS"
      service.queryPaymentStatus(2L) shouldBe "CLOSED"
      service.queryPaymentStatus(3L) shouldBe "UNKNOWN"
    }
    test("callbackWithValidSignatureAndAmountProcessesOrder") {
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction()
      every { orderQueryApiService.findByOrderNumber("ORD-TEST-001") } returns order()
      every { orderQueryService.getOrderById(1L) } returns order()

      val ok = service.handleWechatPayCallback("{}", emptyMap())

      ok shouldBe true
      verify(exactly = 1) { orderMutationService.handlePaymentSuccess(1L, "WX-TXN-001") }
    }
    test("callbackWithAmountMismatchRejects") {
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction(amountCents = 999)
      every { orderQueryApiService.findByOrderNumber("ORD-TEST-001") } returns order()

      val ok = service.handleWechatPayCallback("{}", emptyMap())

      ok shouldBe false
      verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
    }
    test("callbackWithInvalidSignatureRejects") {
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } throws IllegalStateException("签名验证失败")

      val ok = service.handleWechatPayCallback("{}", emptyMap())

      ok shouldBe false
    }
    test("callbackRepeatIsIdempotent") {
      val paidOrder = order(status = OrderModelStatus.PENDING_DELIVERY).also { it.paymentTransactionId = "WX-TXN-001" }
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction()
      every { orderQueryApiService.findByOrderNumber("ORD-TEST-001") } returns paidOrder
      every { orderQueryService.getOrderById(1L) } returns paidOrder

      val ok = service.handleWechatPayCallback("{}", emptyMap())

      ok shouldBe true
      verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
    }
    test("callbackAfterOrderCancelledDoesNotTransition") {
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction()
      every { orderQueryApiService.findByOrderNumber("ORD-TEST-001") } returns order(status = OrderModelStatus.CANCELLED)
      every { orderQueryService.getOrderById(1L) } returns order(status = OrderModelStatus.CANCELLED)

      val ok = service.handleWechatPayCallback("{}", emptyMap())

      ok shouldBe true
      verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
    }
    test("callbackProcessesTicketPurchaseWithIdempotency") {
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns
        callbackTransaction(orderNo = "TP2025-001", amountCents = 10000)
      every { orderQueryApiService.findByOrderNumber("TP2025-001") } returns null
      // 购桶单先行匹配(回调派发顺序:订单 → 购桶 → 购票,G06):无匹配购桶单
      every { barrelPurchaseQueryService.findByOrderNo("TP2025-001") } returns null
      every { ticketQueryApiService.findPurchaseByOrderNo("TP2025-001") } returns ticketPurchase()

      val ok = service.handleWechatPayCallback("{}", emptyMap())
      ok shouldBe true
      verify(exactly = 1) { ticketMutationApiService.handleTicketPurchasePaid(1L, "WX-TXN-001") }

      // 重复回调:已支付 → 幂等跳过
      val paid = ticketPurchase().also { it.paymentStatus = TicketPurchaseStatus.PAID }
      every { ticketQueryApiService.findPurchaseByOrderNo("TP2025-001") } returns paid
      val ok2 = service.handleWechatPayCallback("{}", emptyMap())
      ok2 shouldBe true
      verify(exactly = 1) { ticketMutationApiService.handleTicketPurchasePaid(any(), any()) }
    }
    test("callbackProcessesBarrelPurchaseWithIdempotency (G06)") {
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns
        callbackTransaction(orderNo = "BK2025-001", amountCents = 4000)
      every { orderQueryApiService.findByOrderNumber("BK2025-001") } returns null
      every { barrelPurchaseQueryService.findByOrderNo("BK2025-001") } returns barrelPurchase()

      val ok = service.handleWechatPayCallback("{}", emptyMap())
      ok shouldBe true
      verify(exactly = 1) { barrelPurchaseMutationService.handleBarrelPurchasePaid(1L, "WX-TXN-001") }

      // 重复回调:已支付 → 幂等跳过
      val paid = barrelPurchase().also { it.paymentStatus = BarrelPaymentStatus.PAID }
      every { barrelPurchaseQueryService.findByOrderNo("BK2025-001") } returns paid
      val ok2 = service.handleWechatPayCallback("{}", emptyMap())
      ok2 shouldBe true
      verify(exactly = 1) { barrelPurchaseMutationService.handleBarrelPurchasePaid(any(), any()) }
    }
    test("callbackWithBarrelAmountMismatchRejects (G06)") {
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns
        callbackTransaction(orderNo = "BK2025-001", amountCents = 999)
      every { orderQueryApiService.findByOrderNumber("BK2025-001") } returns null
      every { barrelPurchaseQueryService.findByOrderNo("BK2025-001") } returns barrelPurchase()

      val ok = service.handleWechatPayCallback("{}", emptyMap())

      ok shouldBe false
      verify(exactly = 0) { barrelPurchaseMutationService.handleBarrelPurchasePaid(any(), any()) }
    }
    test("callbackWithoutMatchingOrderRejects") {
      every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction(orderNo = "UNKNOWN-001")
      every { orderQueryApiService.findByOrderNumber("UNKNOWN-001") } returns null

      val ok = service.handleWechatPayCallback("{}", emptyMap())

      ok shouldBe false
    }
    test("paymentTimeoutClaimsAndDelegatesToOrderMutation (R-ORD-04)") {
      every { paymentRepository.claimExpired(1L, any()) } returns 1
      every { orderQueryService.getOrderById(1L) } returns order()

      service.handlePaymentTimeout(1L)

      verify(exactly = 1) { orderMutationService.handlePaymentTimeout(1L) }
    }
    test("paymentTimeoutSkipsWhenClaimFails (幂等/互斥)") {
      every { paymentRepository.claimExpired(1L, any()) } returns 0

      service.handlePaymentTimeout(1L)

      verify(exactly = 0) { orderMutationService.handlePaymentTimeout(any()) }
    }
    test("paymentTimeoutReconcilesSuccessWhenCallbackWonRace") {
      val payment = PaymentModel(id = 9L, orderId = 1L, userId = 100L, amount = 1200, status = PaymentStatus.EXPIRED)
      every { paymentRepository.claimExpired(1L, any()) } returns 1
      every { orderQueryService.getOrderById(1L) } returns
        order(status = OrderModelStatus.PENDING_DELIVERY, transactionId = "WX-1", paymentTime = LocalDateTime.now())
      every { paymentRepository.findByOrderId(1L) } returns Optional.of(payment)

      service.handlePaymentTimeout(1L)

      verify(exactly = 0) { orderMutationService.handlePaymentTimeout(any()) }
      verify { paymentRepository.save(match { it.status == PaymentStatus.SUCCESS }) }
      payment.transactionId shouldBe "WX-1"
    }
    test("upsertPublishesPaymentExpiryCheckDelayedMessage") {
      every { orderQueryService.getOrderById(1L) } returns order()
      every { paymentRepository.findByOrderId(1L) } returns Optional.empty()

      service.createWechatJsapiPay(1L, 1200, "桶装水订单", "openid-1")

      verify(exactly = 1) { eventPublishService.publishPaymentExpiryCheck(1L, 100L, any()) }
    }
  })
