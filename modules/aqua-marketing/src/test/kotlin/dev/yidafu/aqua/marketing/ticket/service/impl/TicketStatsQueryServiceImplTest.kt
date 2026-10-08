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

import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketPurchaseRepositoryCustom
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepository
import dev.yidafu.aqua.marketing.ticket.domain.repository.UserTicketRepositoryCustom
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * 桶水票票种销售/使用统计服务单元测试(后台详情图表,F-MKT-007):
 * 按日聚合、空数据日补零、可用计数、非法区间抛错。
 */
class TicketStatsQueryServiceImplTest :
  FunSpec({
    var ticketTypeRepository: TicketTypeRepository = mockk()
    var ticketPurchaseRepository: TicketPurchaseRepository = mockk()
    var userTicketRepository: UserTicketRepository = mockk()
    var service: TicketStatsQueryServiceImpl = mockk()

    fun salesDay(
      day: LocalDate,
      purchaseCount: Long,
      ticketCount: Long,
      revenueCents: Long,
    ): TicketPurchaseRepositoryCustom.TicketPaidSalesDay =
      object : TicketPurchaseRepositoryCustom.TicketPaidSalesDay {
        override fun getDay(): LocalDate = day

        override fun getPurchaseCount(): Long = purchaseCount

        override fun getTicketCount(): Long = ticketCount

        override fun getRevenueCents(): Long = revenueCents
      }

    fun usageDay(
      day: LocalDate,
      redeemedCount: Long,
      returnedCount: Long,
      expiredCount: Long,
    ): UserTicketRepositoryCustom.TicketUsageDay =
      object : UserTicketRepositoryCustom.TicketUsageDay {
        override fun getDay(): LocalDate = day

        override fun getRedeemedCount(): Long = redeemedCount

        override fun getReturnedCount(): Long = returnedCount

        override fun getExpiredCount(): Long = expiredCount
      }

    beforeTest {
      ticketTypeRepository = mockk()
      ticketPurchaseRepository = mockk()
      userTicketRepository = mockk()
      service = TicketStatsQueryServiceImpl(ticketTypeRepository, ticketPurchaseRepository, userTicketRepository)
    }

    test("aggregates sales and usage by day and zero-fills missing days") {
      val start = LocalDateTime.of(2026, 8, 1, 0, 0)
      val end = LocalDateTime.of(2026, 8, 3, 0, 0) // 3 天窗口:08-01 ~ 08-03
      every { ticketTypeRepository.findById(1L) } returns
        Optional.of(TicketTypeModel(id = 1L, name = "通兑票", ticketType = dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind.UNIVERSAL))
      every { ticketPurchaseRepository.findPaidSalesGroupedByDay(1L, start, end.plusDays(1)) } returns
        listOf(
          salesDay(LocalDate.of(2026, 8, 1), 1, 10, 10000),
          salesDay(LocalDate.of(2026, 8, 3), 2, 20, 20000),
        )
      every { userTicketRepository.findUsageGroupedByDay(1L, start, end.plusDays(1)) } returns
        listOf(usageDay(LocalDate.of(2026, 8, 2), 3, 1, 0))
      every {
        userTicketRepository.countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(
          1L,
          listOf(UserTicketStatus.UNUSED, UserTicketStatus.RETURNED),
          any(),
        )
      } returns 7

      val stats = service.getTicketTypeStats(1L, start, end)

      stats.ticketTypeId shouldBe 1L
      stats.ticketTypeName shouldBe "通兑票"
      // 汇总:跨 3 天累计
      stats.summary.soldCount shouldBe 30
      stats.summary.revenueCents shouldBe 30000
      stats.summary.redeemedCount shouldBe 3
      stats.summary.returnedCount shouldBe 1
      stats.summary.expiredCount shouldBe 0
      stats.summary.availableCount shouldBe 7
      // 序列:3 天,空数据日补零(08-02 销售为 0,08-01/03 使用为 0)
      stats.sales.size shouldBe 3
      stats.sales[0].ticketCount shouldBe 10
      stats.sales[1].ticketCount shouldBe 0
      stats.sales[2].ticketCount shouldBe 20
      stats.sales[1].purchaseCount shouldBe 0
      stats.sales[1].revenueCents shouldBe 0
      stats.usage.size shouldBe 3
      stats.usage[0].redeemedCount shouldBe 0
      stats.usage[1].redeemedCount shouldBe 3
      stats.usage[1].returnedCount shouldBe 1
      stats.usage[2].redeemedCount shouldBe 0
      // 日期连续
      stats.sales.map { it.date } shouldBe
        listOf(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 3))
    }

    test("falls back ticket type name when not found") {
      val start = LocalDateTime.of(2026, 8, 1, 0, 0)
      val end = LocalDateTime.of(2026, 8, 1, 0, 0)
      every { ticketTypeRepository.findById(99L) } returns Optional.empty()
      every { ticketPurchaseRepository.findPaidSalesGroupedByDay(99L, start, end.plusDays(1)) } returns emptyList()
      every { userTicketRepository.findUsageGroupedByDay(99L, start, end.plusDays(1)) } returns emptyList()
      every {
        userTicketRepository.countByTicketTypeIdAndStatusInAndExpireTimeGreaterThan(99L, any(), any())
      } returns 0

      val stats = service.getTicketTypeStats(99L, start, end)

      stats.ticketTypeName shouldBe "水票"
      stats.sales.size shouldBe 1
      stats.usage.size shouldBe 1
      stats.summary.soldCount shouldBe 0
    }

    test("rejects invalid date range") {
      every { ticketTypeRepository.findById(1L) } returns
        Optional.of(TicketTypeModel(id = 1L, name = "票", ticketType = dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind.UNIVERSAL))

      shouldThrow<BadRequestException> {
        service.getTicketTypeStats(1L, LocalDateTime.of(2026, 8, 3, 0, 0), LocalDateTime.of(2026, 8, 1, 0, 0))
      }
    }
  })
