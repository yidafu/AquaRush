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

import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 桶水票用户侧查询服务单元测试(水票商城/我的水票/可用张数预检)。 */
class TicketQueryApiServiceImplTest {
  private lateinit var ticketTypeRepository: TicketTypeRepository
  private lateinit var userTicketRepository: UserTicketRepository
  private lateinit var ticketPurchaseRepository: TicketPurchaseRepository
  private lateinit var service: TicketQueryApiServiceImpl

  private val now: LocalDateTime = LocalDateTime.now()

  @BeforeEach
  fun setUp() {
    ticketTypeRepository = mockk()
    userTicketRepository = mockk()
    ticketPurchaseRepository = mockk()
    service = TicketQueryApiServiceImpl(ticketTypeRepository, userTicketRepository, ticketPurchaseRepository)
  }

  private fun ticketType(
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

  private fun ticket(
    id: Long,
    typeId: Long,
    expireTime: LocalDateTime = now.plusDays(30),
    status: UserTicketStatus = UserTicketStatus.UNUSED,
  ): UserTicketModel =
    UserTicketModel(
      id = id,
      userId = 1L,
      ticketTypeId = typeId,
      purchaseId = 1L,
      status = status,
      expireTime = expireTime,
      isGift = false,
    )

  @Test
  @DisplayName("水票商城:仅 ACTIVE 票种")
  fun `findActiveTicketTypes`() {
    every { ticketTypeRepository.findByStatus(TicketTypeStatus.ACTIVE) } returns listOf(ticketType(1L))

    assertEquals(1, service.findActiveTicketTypes().size)
  }

  @Test
  @DisplayName("我的水票:无状态=全部,有状态=过滤")
  fun `findUserTickets by status`() {
    every { userTicketRepository.findByUserId(1L) } returns listOf(ticket(1L, 1L))
    every { userTicketRepository.findByUserIdAndStatus(1L, UserTicketStatus.USED) } returns
      listOf(ticket(2L, 1L, status = UserTicketStatus.USED))

    assertEquals(1, service.findUserTickets(1L, null).size)
    assertEquals(1, service.findUserTickets(1L, UserTicketStatus.USED).size)
  }

  @Test
  @DisplayName("可用票:仅未过期 UNUSED/RETURNED")
  fun `findUsableTickets filters expired`() {
    every {
      userTicketRepository.findByUserIdAndStatusIn(1L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED))
    } returns
      listOf(
        ticket(1L, 1L, now.plusDays(1)),
        ticket(2L, 1L, now.minusDays(1)),
        ticket(3L, 1L, now.plusDays(1), UserTicketStatus.RETURNED),
      )

    val result = service.findUsableTickets(1L)

    assertEquals(listOf(1L, 3L), result.map { it.id })
  }

  @Test
  @DisplayName("可用张数:指定商品票按 productId 匹配")
  fun `countUsableFor specific product`() {
    every { userTicketRepository.findByUserIdAndStatusIn(1L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED)) } returns
      listOf(ticket(1L, 1L), ticket(2L, 2L))
    every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(1L, TicketTypeKind.SPECIFIC_PRODUCT, productId = 10L))
    every { ticketTypeRepository.findById(2L) } returns Optional.of(ticketType(2L, TicketTypeKind.SPECIFIC_PRODUCT, productId = 20L))

    assertEquals(1, service.countUsableFor(1L, productId = 10L, specification = "19L"))
    assertEquals(0, service.countUsableFor(1L, productId = 99L, specification = "19L"))
  }

  @Test
  @DisplayName("可用张数:通兑票按规格字符串匹配(跨规格不通用)")
  fun `countUsableFor universal spec match`() {
    every { userTicketRepository.findByUserIdAndStatusIn(1L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED)) } returns
      listOf(ticket(1L, 1L), ticket(2L, 1L))
    every { ticketTypeRepository.findById(1L) } returns Optional.of(ticketType(1L, TicketTypeKind.UNIVERSAL, specification = "19L"))

    assertEquals(2, service.countUsableFor(1L, productId = 1L, specification = "19L"))
    assertEquals(0, service.countUsableFor(1L, productId = 1L, specification = "5L"))
  }

  @Test
  @DisplayName("可用张数:票种不存在则不计入")
  fun `countUsableFor missing ticket type`() {
    every { userTicketRepository.findByUserIdAndStatusIn(1L, listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED)) } returns
      listOf(ticket(1L, 99L))
    every { ticketTypeRepository.findById(99L) } returns Optional.empty()

    assertEquals(0, service.countUsableFor(1L, productId = 1L, specification = "19L"))
  }

  @Test
  @DisplayName("已购张数:委托购票单仓库")
  fun `countPurchasedByTicketType`() {
    every { ticketPurchaseRepository.countByUserIdAndTicketTypeIdAndPaymentStatus(1L, 5L, TicketPurchaseStatus.PAID) } returns 3

    assertEquals(3, service.countPurchasedByTicketType(1L, 5L))
  }

  @Test
  @DisplayName("购票单:按 id / orderNo 查询")
  fun `findPurchaseById and byOrderNo`() {
    val purchase = TicketPurchaseModel(id = 1L, userId = 1L, ticketTypeId = 5L, orderNo = "TP5")
    every { ticketPurchaseRepository.findById(1L) } returns Optional.of(purchase)
    every { ticketPurchaseRepository.findById(99L) } returns Optional.empty()
    every { ticketPurchaseRepository.findByOrderNo("TP5") } returns purchase
    every { ticketPurchaseRepository.findByOrderNo("NOPE") } returns null

    assertEquals(1L, service.findPurchaseById(1L)?.id)
    assertNull(service.findPurchaseById(99L))
    assertEquals("TP5", service.findPurchaseByOrderNo("TP5")?.orderNo)
    assertNull(service.findPurchaseByOrderNo("NOPE"))
  }
}
