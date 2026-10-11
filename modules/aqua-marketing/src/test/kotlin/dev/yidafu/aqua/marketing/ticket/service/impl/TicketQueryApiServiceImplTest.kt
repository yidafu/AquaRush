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

import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

/** 桶水票用户侧查询服务单元测试(水票商城/我的水票/可用张数预检)。 */
class TicketQueryApiServiceImplTest :
  FunSpec({
    var ticketTypeRepository: TicketTypeRepository = mockk()
    var userTicketRepository: UserTicketRepository = mockk()
    var ticketPurchaseRepository: TicketPurchaseRepository = mockk()
    var orderQueryApiService: OrderQueryApiService = mockk()
    var service: TicketQueryApiServiceImpl = mockk()
    val now: LocalDateTime = LocalDateTime.now()

    fun ticketType(
      id: Long,
      kind: TicketTypeKind = TicketTypeKind.UNIVERSAL,
      productId: Long? = null,
      specification: String? = "19L",
    ): TicketTypeModel =
      TicketTypeModel(
        id = id,
        name = "票$id",
        ticketType = kind,
        productId = productId,
        specification = specification,
        packageCount = 10,
        packagePriceCents = 10000,
        giftCount = 0,
        validityType = dev.yidafu.aqua.common.domain.model.enums.ValidityType.FIXED_RANGE,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(365),
        perUserLimit = 0,
        status = TicketTypeStatus.ACTIVE,
      )

    fun ticket(
      id: Long,
      typeId: Long,
      expireTime: LocalDateTime = now.plusDays(30),
      status: UserTicketStatus = UserTicketStatus.UNUSED,
      purchaseId: Long = 1L,
    ): UserTicketModel =
      UserTicketModel(
        id = id,
        userId = 1L,
        ticketTypeId = typeId,
        purchaseId = purchaseId,
        status = status,
        expireTime = expireTime,
        isGift = false,
      )

    beforeTest {
      ticketTypeRepository = mockk()
      userTicketRepository = mockk()
      ticketPurchaseRepository = mockk()
      orderQueryApiService = mockk()
      service = TicketQueryApiServiceImpl(ticketTypeRepository, userTicketRepository, ticketPurchaseRepository, orderQueryApiService)
    }
    test("findActiveTicketTypes") {
      every { ticketTypeRepository.findByStatus(TicketTypeStatus.ACTIVE) } returns listOf(ticketType(1L))

      service.findActiveTicketTypes().size shouldBe 1
    }
    test("findUserTickets by status") {
      every { userTicketRepository.findByUserId(1L) } returns listOf(ticket(1L, 1L))
      every { userTicketRepository.findByUserIdAndStatus(1L, UserTicketStatus.USED) } returns
        listOf(ticket(2L, 1L, status = UserTicketStatus.USED))

      service.findUserTickets(1L, null).size shouldBe 1
      service.findUserTickets(1L, UserTicketStatus.USED).size shouldBe 1
    }
    test("findUsableTickets filters expired") {
      every {
        userTicketRepository.findByUserIdAndStatusIn(1L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED))
      } returns
        listOf(
          ticket(1L, 1L, now.plusDays(1)),
          ticket(2L, 1L, now.minusDays(1)),
          ticket(3L, 1L, now.plusDays(1), UserTicketStatus.RETURNED),
        )

      val result = service.findUsableTickets(1L)

      result.map { it.id } shouldBe listOf(1L, 3L)
    }
    test("countUsableFor specific product") {
      every { userTicketRepository.findByUserIdAndStatusIn(1L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED)) } returns
        listOf(ticket(1L, 1L), ticket(2L, 2L))
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(1L, TicketTypeKind.SPECIFIC_PRODUCT, productId = 10L))
      every { ticketTypeRepository.findById(2L) } returns Optional.of(ticketType(2L, TicketTypeKind.SPECIFIC_PRODUCT, productId = 20L))

      service.countUsableFor(1L, productId = 10L, specification = "19L") shouldBe 1
      service.countUsableFor(1L, productId = 99L, specification = "19L") shouldBe 0
    }
    test("countUsableFor universal spec match") {
      every { userTicketRepository.findByUserIdAndStatusIn(1L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED)) } returns
        listOf(ticket(1L, 1L), ticket(2L, 1L))
      every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(1L, TicketTypeKind.UNIVERSAL, specification = "19L"))

      service.countUsableFor(1L, productId = 1L, specification = "19L") shouldBe 2
      service.countUsableFor(1L, productId = 1L, specification = "5L") shouldBe 0
    }
    test("countUsableFor missing ticket type") {
      every { userTicketRepository.findByUserIdAndStatusIn(1L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED)) } returns
        listOf(ticket(1L, 99L))
      every { ticketTypeRepository.findById(99L) } returns Optional.empty()

      service.countUsableFor(1L, productId = 1L, specification = "19L") shouldBe 0
    }
    test("countPurchasedByTicketType") {
      every { ticketPurchaseRepository.countByUserIdAndTicketTypeIdAndPaymentStatus(1L, 5L, TicketPurchaseStatus.PAID) } returns 3

      service.countPurchasedByTicketType(1L, 5L) shouldBe 3
    }
    test("findPurchaseById and byOrderNo") {
      val purchase = TicketPurchaseModel(id = 1L, userId = 1L, ticketTypeId = 5L, orderNo = "TP5")
      every { ticketPurchaseRepository.findById(1L) } returns Optional.of(purchase)
      every { ticketPurchaseRepository.findById(99L) } returns Optional.empty()
      every { ticketPurchaseRepository.findByOrderNo("TP5") } returns purchase
      every { ticketPurchaseRepository.findByOrderNo("NOPE") } returns null

      service.findPurchaseById(1L)?.id shouldBe 1L
      service.findPurchaseById(99L).shouldBeNull()
      service.findPurchaseByOrderNo("TP5")?.orderNo shouldBe "TP5"
      service.findPurchaseByOrderNo("NOPE").shouldBeNull()
    }
    test("findPendingActivationIds empty tickets short-circuits without order query") {
      service.findPendingActivationIds(1L, emptyList()) shouldBe emptySet()
      verify(exactly = 0) { orderQueryApiService.countByUserIdGroupByStatus(any()) }
    }
    test("findPendingActivationIds pending when completed below baseline plus required") {
      val t = ticket(1L, 1L)
      every { ticketPurchaseRepository.findByIds(listOf(1L)) } returns
        listOf(
          TicketPurchaseModel(id = 1L, userId = 1L, ticketTypeId = 1L, orderNo = "TP1", completedOrdersAtPurchase = 0),
        )
      every { ticketTypeRepository.findAllById(listOf(1L)) } returns
        listOf(ticketType(1L).apply { activationOrderCount = 1 })
      every { orderQueryApiService.countByUserIdGroupByStatus(1L) } returns emptyMap()

      service.findPendingActivationIds(1L, listOf(t)) shouldBe setOf(1L)
    }
    test("findPendingActivationIds activated when completed reaches baseline plus required") {
      val t = ticket(1L, 1L)
      every { ticketPurchaseRepository.findByIds(listOf(1L)) } returns
        listOf(
          TicketPurchaseModel(id = 1L, userId = 1L, ticketTypeId = 1L, orderNo = "TP1", completedOrdersAtPurchase = 1),
        )
      every { ticketTypeRepository.findAllById(listOf(1L)) } returns
        listOf(ticketType(1L).apply { activationOrderCount = 1 })
      every { orderQueryApiService.countByUserIdGroupByStatus(1L) } returns mapOf(OrderModelStatus.COMPLETED to 2L)

      service.findPendingActivationIds(1L, listOf(t)) shouldBe emptySet()
    }
    test("findPendingActivationIds ignores purchases without baseline") {
      val t = ticket(1L, 1L)
      every { ticketPurchaseRepository.findByIds(listOf(1L)) } returns
        listOf(TicketPurchaseModel(id = 1L, userId = 1L, ticketTypeId = 1L, orderNo = "TP1", completedOrdersAtPurchase = null))

      service.findPendingActivationIds(1L, listOf(t)) shouldBe emptySet()
      verify(exactly = 0) { orderQueryApiService.countByUserIdGroupByStatus(any()) }
    }
    test("findPendingActivationIds ignores types without activation enabled") {
      val t = ticket(1L, 1L)
      every { ticketPurchaseRepository.findByIds(listOf(1L)) } returns
        listOf(
          TicketPurchaseModel(id = 1L, userId = 1L, ticketTypeId = 1L, orderNo = "TP1", completedOrdersAtPurchase = 0),
        )
      every { ticketTypeRepository.findAllById(listOf(1L)) } returns listOf(ticketType(1L))

      service.findPendingActivationIds(1L, listOf(t)) shouldBe emptySet()
      verify(exactly = 0) { orderQueryApiService.countByUserIdGroupByStatus(any()) }
    }
    test("findPendingActivationIds ignores used and expired tickets") {
      val used = ticket(1L, 1L, status = UserTicketStatus.USED)
      val expired = ticket(2L, 1L, now.minusDays(1))

      service.findPendingActivationIds(1L, listOf(used, expired)) shouldBe emptySet()
      verify(exactly = 0) { ticketPurchaseRepository.findByIds(any()) }
    }
  })
