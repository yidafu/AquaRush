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
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 桶水票票种查询服务单元测试(F-MKT-007 列表/详情)。 */
class TicketTypeQueryServiceImplTest {
  private lateinit var ticketTypeRepository: TicketTypeRepository
  private lateinit var service: TicketTypeQueryServiceImpl

  @BeforeEach
  fun setUp() {
    ticketTypeRepository = mockk()
    service = TicketTypeQueryServiceImpl(ticketTypeRepository)
  }

  @Test
  @DisplayName("列表:带关键字与状态")
  fun `listTicketTypes with filters`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    val page: Page<TicketTypeModel> = PageImpl(listOf(TicketTypeModel(id = 1L, name = "通兑票")))
    every { ticketTypeRepository.findAll(any<Specification<TicketTypeModel>>(), any<Pageable>()) } returns page

    val result = service.listTicketTypes("通兑", TicketTypeStatus.ACTIVE, pageable)

    assertEquals(1, result.totalElements)
    verify { ticketTypeRepository.findAll(any<Specification<TicketTypeModel>>(), any<Pageable>()) }
  }

  @Test
  @DisplayName("列表:无过滤条件")
  fun `listTicketTypes without filters`() {
    val pageable: Pageable = PageRequest.of(0, 10)
    every { ticketTypeRepository.findAll(any<Specification<TicketTypeModel>>(), any<Pageable>()) } returns PageImpl(emptyList())

    val result = service.listTicketTypes(null, null, pageable)

    assertEquals(0, result.totalElements)
  }

  @Test
  @DisplayName("详情:存在返回,不存在返回 null")
  fun `getTicketType`() {
    every { ticketTypeRepository.findById(1L) } returns Optional.of(TicketTypeModel(id = 1L, name = "通兑票"))
    every { ticketTypeRepository.findById(99L) } returns Optional.empty()

    assertEquals(1L, service.getTicketType(1L)?.id)
    assertNull(service.getTicketType(99L))
  }
}
