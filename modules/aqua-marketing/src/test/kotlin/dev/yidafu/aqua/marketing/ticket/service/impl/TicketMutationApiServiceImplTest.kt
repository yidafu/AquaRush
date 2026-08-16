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
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
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
import jakarta.persistence.EntityManager
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
    var entityManager: EntityManager = mockk()
    var bizLogger: BizLogger = mockk()
    var service: TicketMutationApiServiceImpl = mockk()

    fun ticketType(
      id: Long = 1L,
      kind: TicketTypeKind = TicketTypeKind.UNIVERSAL,
      specification: String? = "19L",
      perUserLimit: Int = 0,
      giftCount: Int = 0,
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
        status = TicketTypeStatus.ACTIVE,
      )

    beforeTest {
      ticketTypeRepository = mockk()
      ticketPurchaseRepository = mockk()
      userTicketRepository = mockk()
      entityManager = mockk(relaxed = true)
      bizLogger = mockk(relaxed = true)
      service = TicketMutationApiServiceImpl(ticketTypeRepository, ticketPurchaseRepository, userTicketRepository, entityManager, bizLogger)
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
      every { ticketPurchaseRepository.markPaidAtomically(1L, "tx-1", any()) } returns 1
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(giftCount = 1))
      // 真实实现中 EntityManager.refresh 将实体刷新为 DB 最新状态(PAID)
      every { entityManager.refresh(any()) } answers { (firstArg() as TicketPurchaseModel).paymentStatus = TicketPurchaseStatus.PAID }
      val ticketSlot = slot<UserTicketModel>()
      every { userTicketRepository.save(capture(ticketSlot)) } answers { firstArg() }

      val paid = service.handleTicketPurchasePaid(1L, "tx-1")

      paid.paymentStatus shouldBe TicketPurchaseStatus.PAID
      verify(exactly = 11) { userTicketRepository.save(any()) }
      ticketSlot.captured.isGift shouldBe true
      ticketSlot.captured.status shouldBe UserTicketStatus.UNUSED
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
    }
    test("returnTicketsByOrder returns unexpired tickets") {
      every { userTicketRepository.returnByOrder(99L, any()) } returns 3

      val returned = service.returnTicketsByOrder(99L)

      returned shouldBe 3
    }
  })
