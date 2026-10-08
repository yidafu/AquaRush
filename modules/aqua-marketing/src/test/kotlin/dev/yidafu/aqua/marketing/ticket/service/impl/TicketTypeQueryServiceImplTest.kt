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
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.util.Optional

/** 桶水票票种查询服务单元测试(F-MKT-007 列表/详情)。 */
class TicketTypeQueryServiceImplTest :
  FunSpec({
    var ticketTypeRepository: TicketTypeRepository = mockk()
    var service: TicketTypeQueryServiceImpl = mockk()

    beforeTest {
      ticketTypeRepository = mockk()
      service = TicketTypeQueryServiceImpl(ticketTypeRepository)
    }
    test("listTicketTypes with filters") {
      val pageable: Pageable = PageRequest.of(0, 10)
      val page: Page<TicketTypeModel> = PageImpl(listOf(TicketTypeModel(id = 1L, name = "通兑票")))
      every { ticketTypeRepository.findAll(any<Specification<TicketTypeModel>>(), any<Pageable>()) } returns page

      val result = service.listTicketTypes("通兑", TicketTypeStatus.ACTIVE, pageable)

      result.totalElements shouldBe 1
      verify { ticketTypeRepository.findAll(any<Specification<TicketTypeModel>>(), any<Pageable>()) }
    }
    test("listTicketTypes without filters") {
      val pageable: Pageable = PageRequest.of(0, 10)
      every { ticketTypeRepository.findAll(any<Specification<TicketTypeModel>>(), any<Pageable>()) } returns PageImpl(emptyList())

      val result = service.listTicketTypes(null, null, pageable)

      result.totalElements shouldBe 0
    }
    test("getTicketType") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(TicketTypeModel(id = 1L, name = "通兑票"))
      every { ticketTypeRepository.findById(99L) } returns Optional.empty()

      service.getTicketType(1L)?.id shouldBe 1L
      service.getTicketType(99L).shouldBeNull()
    }
  })
