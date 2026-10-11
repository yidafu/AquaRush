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

package dev.yidafu.aqua.marketing.ticket.service.impl

import dev.yidafu.aqua.api.dto.PurchaseTicketsRequest
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

/**
 * 桶水票用户侧变更服务单元测试(F-MKT-007):
 * 购票限购、支付入账(买N送M、幂等)、取消退票。
 */
class TicketMutationApiServiceImplTest :
  FunSpec({
    var ticketTypeRepository: TicketTypeRepository = mockk()
    var ticketPurchaseRepository: TicketPurchaseRepository = mockk()
    var userTicketRepository: UserTicketRepository = mockk()
    var orderQueryApiService: OrderQueryApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: TicketMutationApiServiceImpl = mockk()

    fun ticketType(
      id: Long = 1L,
      kind: TicketTypeKind = TicketTypeKind.UNIVERSAL,
      specification: String? = "19L",
      perUserLimit: Int = 0,
      giftCount: Int = 0,
      isLeadGeneration: Boolean = false,
      activationOrderCount: Int? = null,
    ): TicketTypeModel =
      TicketTypeModel(
        id = id,
        name = "通兑票",
        ticketType = kind,
        productId = if (kind == TicketTypeKind.SPECIFIC_PRODUCT) 1L else null,
        specification = specification,
        packageCount = 10,
        packagePriceCents = 10000,
        giftCount = giftCount,
        validityType = ValidityType.FIXED_RANGE,
        validFrom = LocalDateTime.now().minusDays(1),
        validTo = LocalDateTime.now().plusDays(365),
        perUserLimit = perUserLimit,
        isLeadGeneration = isLeadGeneration,
        activationOrderCount = activationOrderCount,
        status = TicketTypeStatus.ACTIVE,
      )

    beforeTest {
      ticketTypeRepository = mockk()
      ticketPurchaseRepository = mockk()
      userTicketRepository = mockk()
      orderQueryApiService = mockk()
      bizLogger = mockk(relaxed = true)
      // 默认:非新用户判定按用例覆盖;未启用复购激活时不触发订单数查询
      every { orderQueryApiService.hasCompletedOrders(any()) } returns false
      every { orderQueryApiService.countByUserIdGroupByStatus(any()) } returns emptyMap()
      service =
        TicketMutationApiServiceImpl(
          ticketTypeRepository,
          ticketPurchaseRepository,
          userTicketRepository,
          orderQueryApiService,
          bizLogger,
        )
    }
    test("purchaseTickets creates pending purchase") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(perUserLimit = 20))
      every { ticketPurchaseRepository.countByUserIdAndTicketTypeIdAndPaymentStatus(1L, 1L, TicketPurchaseStatus.PAID) } returns 5
      val purchaseSlot = slot<TicketPurchaseModel>()
      every { ticketPurchaseRepository.save(capture(purchaseSlot)) } answers { firstArg() }

      val purchase = service.purchaseTickets(1L, PurchaseTicketsRequest(ticketTypeId = 1L, packageCount = 2))

      purchase.paymentStatus shouldBe TicketPurchaseStatus.PENDING
      purchase.packageCount shouldBe 2
      purchase.packagePriceCents shouldBe 20000
      purchase.orderNo.startsWith("TP") shouldBe true
    }
    test("purchaseTickets rejects over limit") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(perUserLimit = 10))
      every { ticketPurchaseRepository.countByUserIdAndTicketTypeIdAndPaymentStatus(1L, 1L, TicketPurchaseStatus.PAID) } returns 9

      shouldThrow<IllegalArgumentException> {
        service.purchaseTickets(1L, PurchaseTicketsRequest(ticketTypeId = 1L, packageCount = 2))
      }
    }
    test("purchaseTickets rejects packageCount out of 1..10") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(perUserLimit = 0))

      shouldThrow<IllegalArgumentException> {
        service.purchaseTickets(1L, PurchaseTicketsRequest(ticketTypeId = 1L, packageCount = 0))
      }
      shouldThrow<IllegalArgumentException> {
        service.purchaseTickets(1L, PurchaseTicketsRequest(ticketTypeId = 1L, packageCount = 11))
      }
    }
    test("handleTicketPurchasePaid credits N plus M gift") {
      val purchase =
        TicketPurchaseModel(
          id = 1L,
          userId = 1L,
          ticketTypeId = 1L,
          orderNo = "TP1",
          packageCount = 10,
          giftCount = 1,
          totalCount = 11,
          packagePriceCents = 10000,
          paymentStatus = TicketPurchaseStatus.PENDING,
        )
      every { ticketPurchaseRepository.findById(1L) } returns Optional.of(purchase)
      every { ticketPurchaseRepository.markPaidAtomically(1L, "tx-1", any()) } answers
        {
          purchase.paymentStatus = TicketPurchaseStatus.PAID
          purchase.transactionId = "tx-1"
          1
        }
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(giftCount = 1))
      every { userTicketRepository.nextTicketSeq(any()) } returnsMany (1L..11L).toList()
      val ticketSlots = mutableListOf<UserTicketModel>()
      every { userTicketRepository.save(capture(ticketSlots)) } answers { firstArg() }

      val paid = service.handleTicketPurchasePaid(1L, "tx-1")

      paid.paymentStatus shouldBe TicketPurchaseStatus.PAID
      verify(exactly = 11) { userTicketRepository.save(any()) }
      // 每张票唯一递增票号:SP + 入账年月 + 8位序号(按月重置 v2.2.0)
      val monthKey =
        java.time.LocalDate
          .now()
          .format(
            java.time.format.DateTimeFormatter
              .ofPattern("yyyyMM"),
          )
      val ticketNoPattern = Regex("^SP\\d{6}\\d{8}$")
      ticketSlots.map { it.ticketNo }.forEach { no -> ticketNoPattern.matches(no) shouldBe true }
      ticketSlots.map { it.ticketNo }.distinct().size shouldBe 11
      ticketSlots.forEachIndexed { index, ticket ->
        ticket.ticketNo shouldBe "SP$monthKey${(index + 1).toString().padStart(8, '0')}"
      }
      // 最后一票为赠送票(总 11 张 = 10 正常 + 1 赠送)
      ticketSlots.last().isGift shouldBe true
      ticketSlots.first().isGift shouldBe false
      ticketSlots.all { it.status == UserTicketStatus.UNUSED } shouldBe true
      ticketSlots.all { it.purchaseId == 1L } shouldBe true
    }
    test("handleTicketPurchasePaid is idempotent") {
      val paidPurchase =
        TicketPurchaseModel(
          id = 1L,
          userId = 1L,
          ticketTypeId = 1L,
          orderNo = "TP1",
          packageCount = 10,
          giftCount = 0,
          totalCount = 10,
          packagePriceCents = 10000,
          paymentStatus = TicketPurchaseStatus.PAID,
          transactionId = "tx-1",
        )
      every { ticketPurchaseRepository.findById(1L) } returns Optional.of(paidPurchase)

      val result = service.handleTicketPurchasePaid(1L, "tx-1")

      result.paymentStatus shouldBe TicketPurchaseStatus.PAID
      verify(exactly = 0) { userTicketRepository.save(any()) }
      verify(exactly = 0) { ticketPurchaseRepository.markPaidAtomically(any(), any(), any()) }
      verify(exactly = 0) { userTicketRepository.nextTicketSeq(any()) }
    }
    test("returnTicketsByOrder returns unexpired tickets") {
      every { userTicketRepository.returnByOrder(99L, any()) } returns 3

      val returned = service.returnTicketsByOrder(99L)

      returned shouldBe 3
    }
    test("lead generation ticket rejected for user with completed orders") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(isLeadGeneration = true))
      every { orderQueryApiService.hasCompletedOrders(1L) } returns true

      val error =
        shouldThrow<IllegalArgumentException> {
          service.purchaseTickets(1L, PurchaseTicketsRequest(ticketTypeId = 1L, packageCount = 1))
        }
      error.message shouldBe "引流票仅限新用户(无已完成订单)购买"
      verify(exactly = 0) { ticketPurchaseRepository.save(any()) }
    }
    test("lead generation ticket purchasable by new user") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(isLeadGeneration = true))
      every { orderQueryApiService.hasCompletedOrders(1L) } returns false
      every { ticketPurchaseRepository.save(any()) } answers { firstArg() }

      val purchase = service.purchaseTickets(1L, PurchaseTicketsRequest(ticketTypeId = 1L, packageCount = 1))

      purchase.paymentStatus shouldBe TicketPurchaseStatus.PENDING
      purchase.completedOrdersAtPurchase shouldBe null
    }
    test("activation enabled purchase records completed orders baseline") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(activationOrderCount = 2))
      every { orderQueryApiService.countByUserIdGroupByStatus(1L) } returns mapOf(OrderModelStatus.COMPLETED to 3L)
      val purchaseSlot = slot<TicketPurchaseModel>()
      every { ticketPurchaseRepository.save(capture(purchaseSlot)) } answers { firstArg() }

      service.purchaseTickets(1L, PurchaseTicketsRequest(ticketTypeId = 1L, packageCount = 1))

      purchaseSlot.captured.completedOrdersAtPurchase shouldBe 3
    }
    test("activation disabled purchase keeps baseline null") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType())
      val purchaseSlot = slot<TicketPurchaseModel>()
      every { ticketPurchaseRepository.save(capture(purchaseSlot)) } answers { firstArg() }

      service.purchaseTickets(1L, PurchaseTicketsRequest(ticketTypeId = 1L, packageCount = 1))

      purchaseSlot.captured.completedOrdersAtPurchase shouldBe null
      verify(exactly = 0) { orderQueryApiService.countByUserIdGroupByStatus(any()) }
    }
  })
