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

import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.payment.domain.repository.PaymentRepository
import dev.yidafu.aqua.payment.service.WechatPayGateway
import dev.yidafu.aqua.payment.service.WechatPrepayParams
import dev.yidafu.aqua.payment.service.WechatTransaction
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 支付服务单元测试(PRD 04-order.md §F-ORD-005/R-PAY-01~03):
 * 覆盖假支付自动完成(订单/购票)、真实模式预支付与退款、回调验签/金额校验/幂等/取消不流转。
 */
class PaymentServiceImplTest {
  private lateinit var orderQueryService: OrderQueryService
  private lateinit var orderQueryApiService: OrderQueryApiService
  private lateinit var orderMutationService: OrderMutationService
  private lateinit var ticketQueryApiService: TicketQueryApiService
  private lateinit var ticketMutationApiService: TicketMutationApiService
  private lateinit var paymentRepository: PaymentRepository
  private lateinit var wechatPayGateway: WechatPayGateway
  private lateinit var bizLogger: BizLogger
  private lateinit var service: PaymentServiceImpl

  @BeforeEach
  fun setUp() {
    orderQueryService = mockk(relaxed = true)
    orderQueryApiService = mockk(relaxed = true)
    orderMutationService = mockk(relaxed = true)
    ticketQueryApiService = mockk(relaxed = true)
    ticketMutationApiService = mockk(relaxed = true)
    paymentRepository = mockk(relaxed = true)
    wechatPayGateway = mockk(relaxed = true)
    bizLogger = mockk(relaxed = true)
    service =
      PaymentServiceImpl(
        orderQueryService,
        orderQueryApiService,
        orderMutationService,
        ticketQueryApiService,
        ticketMutationApiService,
        paymentRepository,
        wechatPayGateway,
        bizLogger,
      )
    // JpaRepository.save 返回泛型 S,MockK relaxed 无法构造 PaymentModel → 显式回传入参
    every { paymentRepository.save(any()) } answers { firstArg() }
  }

  private fun order(
    id: Long = 1L,
    status: OrderModelStatus = OrderModelStatus.PENDING_PAYMENT,
    orderNo: String = "ORD-TEST-001",
    amountCents: Long = 1000,
    deliveryFeeCents: Long = 200,
  ) = OrderModel(
    id = id,
    orderNo = orderNo,
    userId = 100L,
    productId = 1L,
    quantity = 1,
    amountCents = amountCents,
    deliveryFeeCents = deliveryFeeCents,
    status = status,
  )

  private fun ticketPurchase(orderNo: String = "TP2025-001") =
    TicketPurchaseModel(
      id = 1L,
      userId = 100L,
      ticketTypeId = 1L,
      orderNo = orderNo,
      packageCount = 1,
      packagePriceCents = 10000,
      paymentStatus = TicketPurchaseStatus.PENDING,
    )

  private fun callbackTransaction(
    orderNo: String = "ORD-TEST-001",
    transactionId: String = "WX-TXN-001",
    amountCents: Long = 1200,
  ) = WechatTransaction(outTradeNo = orderNo, transactionId = transactionId, tradeState = "SUCCESS", amountCents = amountCents)

  // ================================================================
  // 假支付模式(测试环境,默认开启):发起支付即自动完成
  // ================================================================

  @Test
  @DisplayName("假支付:订单发起支付后自动完成(状态流转+返回 mock 参数)")
  fun mockModeAutoCompletesOrderPayment() {
    every { orderQueryService.getOrderById(1L) } returns order()
    every { paymentRepository.findByOrderId(1L) } returns Optional.empty()

    val result = service.createWechatJsapiPay(1L, 1200, "桶装水订单", "openid-1")

    assertTrue(result["mock"] == true, "应返回 mock 标记")
    assertTrue(result["paid"] == true, "应返回已支付标记")
    assertEquals("MOCK-ORD-TEST-001", result["transactionId"])
    verify(exactly = 1) { orderMutationService.handlePaymentSuccess(1L, "MOCK-ORD-TEST-001") }
  }

  @Test
  @DisplayName("假支付:购票单发起支付后自动入账")
  fun mockModeAutoCompletesTicketPurchasePayment() {
    every { ticketQueryApiService.findPurchaseById(1L) } returns ticketPurchase()

    val result = service.createWechatJsapiPayForTicket(1L, "TP2025-001", 10000, "桶水票", "openid-1")

    assertTrue(result["mock"] == true)
    verify(exactly = 1) { ticketMutationApiService.handleTicketPurchasePaid(1L, "MOCK-TP2025-001") }
  }

  @Test
  @DisplayName("假支付:金额与订单应付不一致时拒绝创建支付(R-MON-01)")
  fun mockModeRejectsAmountMismatch() {
    every { orderQueryService.getOrderById(1L) } returns order()

    assertFailsWith<BadRequestException> { service.createWechatJsapiPay(1L, 999, "桶装水订单", "openid-1") }
    verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
  }

  @Test
  @DisplayName("假支付:非待支付订单拒绝创建支付")
  fun mockModeRejectsNonPendingOrder() {
    every { orderQueryService.getOrderById(1L) } returns order(status = OrderModelStatus.DELIVERING)

    assertFailsWith<BadRequestException> { service.createWechatJsapiPay(1L, 1200, "桶装水订单", "openid-1") }
  }

  // ================================================================
  // 真实模式:微信预支付/查询/退款
  // ================================================================

  @Test
  @DisplayName("真实模式:预支付返回 requestPayment 参数并回填 prepayId")
  fun realModePrepayReturnsPaymentParams() {
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

    assertEquals("wx-app", result["appId"])
    assertEquals("prepay_id=wx123", result["package"])
    assertEquals("RSA", result["signType"])
    verify { paymentRepository.save(match { it.prepayId == "wx123" }) }
    verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
  }

  @Test
  @DisplayName("真实模式:未配置商户参数时抛 NotImplementedError(前端提示支付待接入)")
  fun realModeWithoutConfigThrowsNotImplementedError() {
    service.mockEnabled = false
    every { orderQueryService.getOrderById(1L) } returns order()
    every { paymentRepository.findByOrderId(1L) } returns Optional.empty()
    every { wechatPayGateway.isConfigured } returns false

    assertFailsWith<NotImplementedError> { service.createWechatJsapiPay(1L, 1200, "桶装水订单", "openid-1") }
  }

  @Test
  @DisplayName("真实模式:退款调用微信退款接口并标记支付记录 REFUNDED")
  fun realModeRefundCallsGateway() {
    service.mockEnabled = false
    val payment = PaymentModel(orderId = 1L, userId = 100L, transactionId = "WX-TXN-001", amount = 1200)
    every { paymentRepository.findByTransactionId("WX-TXN-001") } returns Optional.of(payment)
    every { orderQueryService.getOrderById(1L) } returns order()
    every { wechatPayGateway.isConfigured } returns true
    every { wechatPayGateway.createRefund(any(), "WX-TXN-001", 1000, 1200, any()) } returns "REFUND-001"
    every { paymentRepository.save(any()) } answers { firstArg() }

    val result = service.refund("WX-TXN-001", 1000, 1200, "订单取消退款")

    assertEquals("REFUND-001", result["refundId"])
    verify { paymentRepository.save(match { it.status.name == "REFUNDED" }) }
  }

  @Test
  @DisplayName("假支付模式:退款直接成功(mock)")
  fun mockModeRefundSucceeds() {
    every { paymentRepository.findByTransactionId(any()) } returns Optional.empty()

    val result = service.refund("MOCK-ORD-TEST-001", 1000, 1200, "订单取消退款")

    assertTrue(result["mock"] == true)
    assertTrue((result["refundId"] as String).startsWith("MOCK-REFUND-"))
  }

  @Test
  @DisplayName("查询支付状态:按订单状态推导 SUCCESS/CLOSED/UNKNOWN")
  fun queryPaymentStatusDerivesFromOrder() {
    every { orderQueryService.getOrderById(1L) } returns order(status = OrderModelStatus.PENDING_DELIVERY)
    every { orderQueryService.getOrderById(2L) } returns order(id = 2L, status = OrderModelStatus.CANCELLED)
    every { orderQueryService.getOrderById(3L) } returns order(id = 3L, status = OrderModelStatus.PENDING_PAYMENT)

    assertEquals("SUCCESS", service.queryPaymentStatus(1L))
    assertEquals("CLOSED", service.queryPaymentStatus(2L))
    assertEquals("UNKNOWN", service.queryPaymentStatus(3L))
  }

  // ================================================================
  // 回调处理(R-PAY-01:验签/金额校验/幂等)
  // ================================================================

  @Test
  @DisplayName("回调:验签通过且金额一致 → 订单流转支付成功")
  fun callbackWithValidSignatureAndAmountProcessesOrder() {
    every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction()
    every { orderQueryApiService.findByOrderNumber("ORD-TEST-001") } returns order()
    every { orderQueryService.getOrderById(1L) } returns order()

    val ok = service.handleWechatPayCallback("{}", emptyMap())

    assertTrue(ok)
    verify(exactly = 1) { orderMutationService.handlePaymentSuccess(1L, "WX-TXN-001") }
  }

  @Test
  @DisplayName("回调:金额与订单应付不一致 → 拒绝放行(AC-ORD-005-06)")
  fun callbackWithAmountMismatchRejects() {
    every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction(amountCents = 999)
    every { orderQueryApiService.findByOrderNumber("ORD-TEST-001") } returns order()

    val ok = service.handleWechatPayCallback("{}", emptyMap())

    assertFalse(ok)
    verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
  }

  @Test
  @DisplayName("回调:验签失败 → 返回 false 由微信重试")
  fun callbackWithInvalidSignatureRejects() {
    every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } throws IllegalStateException("签名验证失败")

    val ok = service.handleWechatPayCallback("{}", emptyMap())

    assertFalse(ok)
  }

  @Test
  @DisplayName("回调:重复回调幂等,不重复流转状态(AC-ORD-005-05)")
  fun callbackRepeatIsIdempotent() {
    val paidOrder = order(status = OrderModelStatus.PENDING_DELIVERY).also { it.paymentTransactionId = "WX-TXN-001" }
    every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction()
    every { orderQueryApiService.findByOrderNumber("ORD-TEST-001") } returns paidOrder
    every { orderQueryService.getOrderById(1L) } returns paidOrder

    val ok = service.handleWechatPayCallback("{}", emptyMap())

    assertTrue(ok)
    verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
  }

  @Test
  @DisplayName("回调:回调到达前订单已取消 → 保持已取消不流转(AC-ORD-005-08)")
  fun callbackAfterOrderCancelledDoesNotTransition() {
    every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction()
    every { orderQueryApiService.findByOrderNumber("ORD-TEST-001") } returns order(status = OrderModelStatus.CANCELLED)
    every { orderQueryService.getOrderById(1L) } returns order(status = OrderModelStatus.CANCELLED)

    val ok = service.handleWechatPayCallback("{}", emptyMap())

    assertTrue(ok)
    verify(exactly = 0) { orderMutationService.handlePaymentSuccess(any(), any()) }
  }

  @Test
  @DisplayName("回调:购票单支付成功入账;重复回调幂等")
  fun callbackProcessesTicketPurchaseWithIdempotency() {
    every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns
      callbackTransaction(orderNo = "TP2025-001", amountCents = 10000)
    every { orderQueryApiService.findByOrderNumber("TP2025-001") } returns null
    every { ticketQueryApiService.findPurchaseByOrderNo("TP2025-001") } returns ticketPurchase()

    val ok = service.handleWechatPayCallback("{}", emptyMap())
    assertTrue(ok)
    verify(exactly = 1) { ticketMutationApiService.handleTicketPurchasePaid(1L, "WX-TXN-001") }

    // 重复回调:已支付 → 幂等跳过
    val paid = ticketPurchase().also { it.paymentStatus = TicketPurchaseStatus.PAID }
    every { ticketQueryApiService.findPurchaseByOrderNo("TP2025-001") } returns paid
    val ok2 = service.handleWechatPayCallback("{}", emptyMap())
    assertTrue(ok2)
    verify(exactly = 1) { ticketMutationApiService.handleTicketPurchasePaid(any(), any()) }
  }

  @Test
  @DisplayName("回调:未匹配业务单 → 返回 false")
  fun callbackWithoutMatchingOrderRejects() {
    every { wechatPayGateway.parseNotification(any(), any(), any(), any(), any()) } returns callbackTransaction(orderNo = "UNKNOWN-001")
    every { orderQueryApiService.findByOrderNumber("UNKNOWN-001") } returns null

    val ok = service.handleWechatPayCallback("{}", emptyMap())

    assertFalse(ok)
  }

  // ================================================================
  // 支付超时
  // ================================================================

  @Test
  @DisplayName("支付超时:委托订单服务处理")
  fun paymentTimeoutDelegatesToOrderMutation() {
    service.handlePaymentTimeout(1L)

    verify(exactly = 1) { orderMutationService.handlePaymentTimeout(1L) }
  }
}
