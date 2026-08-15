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

package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** 桶水票核销规则单元测试(R-MKT-08,聚焦引擎测试未覆盖分支:FIFO/上限/票种缺失)。 */
class TicketRuleTest {
  private lateinit var ticketQueryApiService: TicketQueryApiService
  private lateinit var userTicketRepository: UserTicketRepository
  private lateinit var ticketTypeRepository: TicketTypeRepository
  private lateinit var rule: TicketRule

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
    ticketQueryApiService = mockk()
    userTicketRepository = mockk()
    ticketTypeRepository = mockk()
    rule = TicketRule(ticketQueryApiService, userTicketRepository, ticketTypeRepository)
  }

  private fun ctx(
    quantity: Int = 5,
    ticketRedeemCount: Int = 0,
    orderId: Long? = null,
  ): PricingContext =
    PricingContext(
      userId = 1L,
      items =
        listOf(
          PricingItemContext(
            productId = 1L,
            quantity = quantity,
            product = ProductModel(id = 1L, name = "水", price = 2100, specification = "19L"),
          ),
        ),
      isSelfCollect = false,
      preferredCouponId = null,
      ticketRedeemCount = ticketRedeemCount,
      now = now,
      orderId = orderId,
    )

  private fun state(quantity: Int = 5): PricingState {
    val s = PricingState()
    s.items =
      mutableListOf(
        dev.yidafu.aqua.api.dto.OrderPricingItemResult(
          productId = 1L,
          quantity = quantity,
          unitPriceCents = 2100,
          priceSource = dev.yidafu.aqua.common.domain.model.enums.PriceSource.CATALOG_PRICE,
          catalogPriceCents = 2100,
          subtotalCents = 2100L * quantity,
          ticketCoveredQuantity = 0,
          cashSubtotalCents = 2100L * quantity,
        ),
      )
    s.cashSubtotalCents = 2100L * quantity
    return s
  }

  private fun ticket(
    id: Long,
    expire: LocalDateTime = now.plusDays(10),
  ): UserTicketModel =
    UserTicketModel(
      id = id,
      userId = 1L,
      ticketTypeId = 1L,
      purchaseId = 1L,
      status = UserTicketStatus.UNUSED,
      expireTime = expire,
      isGift = false,
    )

  @Test
  @DisplayName("先过期先用(FIFO):核销优先取最早过期票")
  fun `redeems earliest expiring first`() {
    val late = ticket(1L, now.plusDays(30))
    val early = ticket(2L, now.plusDays(5))
    every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(late, early)
    every { ticketTypeRepository.findById(1L) } returns
      Optional.of(TicketTypeModel(id = 1L, name = "通兑", ticketType = TicketTypeKind.UNIVERSAL, specification = "19L"))

    val s = state()
    rule.evaluate(ctx(quantity = 5, ticketRedeemCount = 1), s)

    assertEquals(1, s.ticketRedeemCount)
    assertEquals(1, s.ticketCoveredQuantity)
    assertEquals(4 * 2100, s.cashSubtotalCents)
  }

  @Test
  @DisplayName("核销张数上限=可用池大小(预检超池)")
  fun `redeem capped by pool size`() {
    every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(ticket(1L), ticket(2L))
    every { ticketTypeRepository.findById(1L) } returns
      Optional.of(TicketTypeModel(id = 1L, name = "通兑", ticketType = TicketTypeKind.UNIVERSAL, specification = "19L"))

    val s = state()
    rule.evaluate(ctx(quantity = 5, ticketRedeemCount = 10), s)

    assertEquals(2, s.ticketRedeemCount)
    assertEquals(2, s.availableTicketCount)
  }

  @Test
  @DisplayName("核销张数上限=订单总桶数(请求超过商品数量)")
  fun `redeem capped by total quantity`() {
    every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(ticket(1L), ticket(2L), ticket(3L))
    every { ticketTypeRepository.findById(1L) } returns
      Optional.of(TicketTypeModel(id = 1L, name = "通兑", ticketType = TicketTypeKind.UNIVERSAL, specification = "19L"))

    val s = state(quantity = 2)
    rule.evaluate(ctx(quantity = 2, ticketRedeemCount = 5), s)

    assertEquals(2, s.ticketRedeemCount)
    assertEquals(0, s.cashSubtotalCents)
  }

  @Test
  @DisplayName("票种不存在:该票不可用(不计入池)")
  fun `missing ticket type excluded from pool`() {
    every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(ticket(1L))
    every { ticketTypeRepository.findById(1L) } returns Optional.empty()

    val s = state()
    rule.evaluate(ctx(quantity = 5, ticketRedeemCount = 1), s)

    assertEquals(0, s.availableTicketCount)
    assertEquals(0, s.ticketRedeemCount)
  }

  @Test
  @DisplayName("下单消费:部分核销失败 → BadRequestException 整体回滚")
  fun `applyForOrder partial redemption rolls back`() {
    every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(ticket(1L), ticket(2L))
    every { ticketTypeRepository.findById(1L) } returns
      Optional.of(TicketTypeModel(id = 1L, name = "通兑", ticketType = TicketTypeKind.UNIVERSAL, specification = "19L"))
    every { userTicketRepository.redeemAtomically(1L, 1L, 99L, now) } returns 1
    every { userTicketRepository.redeemAtomically(2L, 1L, 99L, now) } returns 0

    assertFailsWith<BadRequestException> {
      rule.evaluate(ctx(quantity = 5, ticketRedeemCount = 2, orderId = 99L), state())
    }
    verify { userTicketRepository.redeemAtomically(1L, 1L, 99L, now) }
    verify { userTicketRepository.redeemAtomically(2L, 1L, 99L, now) }
  }

  @Test
  @DisplayName("下单消费:全部核销成功,无异常")
  fun `applyForOrder full redemption ok`() {
    every { ticketQueryApiService.findUsableTickets(1L) } returns listOf(ticket(1L), ticket(2L))
    every { ticketTypeRepository.findById(1L) } returns
      Optional.of(TicketTypeModel(id = 1L, name = "通兑", ticketType = TicketTypeKind.UNIVERSAL, specification = "19L"))
    every { userTicketRepository.redeemAtomically(any(), any(), any(), any()) } returns 1

    val s = state()
    rule.evaluate(ctx(quantity = 5, ticketRedeemCount = 2, orderId = 99L), s)

    assertEquals(2, s.ticketRedeemCount)
    verify(exactly = 2) { userTicketRepository.redeemAtomically(any(), any(), any(), any()) }
  }
}
