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

import dev.yidafu.aqua.api.dto.TicketTypeCreateRequest
import dev.yidafu.aqua.api.dto.TicketTypeUpdateRequest
import dev.yidafu.aqua.common.domain.model.TicketTypeModel
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeKind
import dev.yidafu.aqua.common.domain.model.enums.TicketTypeStatus
import dev.yidafu.aqua.common.domain.model.enums.ValidityType
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.ticket.domain.exception.TicketTypeNotFoundException
import dev.yidafu.aqua.marketing.ticket.domain.repository.TicketTypeRepository
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

/** 桶水票票种变更服务单元测试(F-MKT-007):类型/有效期校验、停用。 */
class TicketTypeMutationServiceImplTest {
  private lateinit var ticketTypeRepository: TicketTypeRepository
  private lateinit var bizLogger: BizLogger
  private lateinit var service: TicketTypeMutationServiceImpl

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
    ticketTypeRepository = mockk()
    bizLogger = mockk(relaxed = true)
    service = TicketTypeMutationServiceImpl(ticketTypeRepository, bizLogger)
  }

  private fun createRequest(
    ticketType: TicketTypeKind = TicketTypeKind.UNIVERSAL,
    productId: Long? = null,
    specification: String? = "19L",
  ): TicketTypeCreateRequest =
    TicketTypeCreateRequest(
      name = "通兑票",
      ticketType = ticketType,
      productId = productId,
      specification = specification,
      packageCount = 10,
      packagePriceCents = 10000,
      giftCount = 1,
      validityType = ValidityType.FIXED_RANGE,
      validFrom = now.minusDays(1),
      validTo = now.plusDays(365),
      perUserLimit = 0,
    )

  @Test
  @DisplayName("建票种:通兑票保存(AC-MKT-007-01)")
  fun `createTicketType universal`() {
    every { ticketTypeRepository.save(any()) } answers { (firstArg() as TicketTypeModel).apply { id = 1L } }

    val saved = service.createTicketType(1L, "admin", createRequest())

    assertEquals(1L, saved.id)
    assertEquals(TicketTypeStatus.ACTIVE, saved.status)
    assertEquals(TicketTypeKind.UNIVERSAL, saved.ticketType)
    verify {
      bizLogger.logMarketingOperation(
        targetId = 1L,
        operation = "TICKET_TYPE_CREATED",
        result = "SUCCESS",
        userId = 1L,
        username = "admin",
        additionalData = any(),
      )
    }
  }

  @Test
  @DisplayName("建票种:指定商品票保存")
  fun `createTicketType specific product`() {
    every { ticketTypeRepository.save(any()) } answers { (firstArg() as TicketTypeModel).apply { id = 1L } }

    val saved =
      service.createTicketType(
        1L,
        "admin",
        createRequest(ticketType = TicketTypeKind.SPECIFIC_PRODUCT, productId = 1L, specification = null),
      )

    assertEquals(1L, saved.productId)
  }

  @Test
  @DisplayName("建票种:指定商品票未选商品 → IllegalArgumentException")
  fun `createTicketType specific without product throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTicketType(1L, "admin", createRequest(ticketType = TicketTypeKind.SPECIFIC_PRODUCT, specification = null))
    }
  }

  @Test
  @DisplayName("建票种:通兑票未填规格 → IllegalArgumentException")
  fun `createTicketType universal without spec throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTicketType(1L, "admin", createRequest(specification = null))
    }
  }

  @Test
  @DisplayName("建票种:固定区间开始不早于结束 → IllegalArgumentException")
  fun `createTicketType invalid range throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTicketType(1L, "admin", createRequest().copy(validFrom = now.plusDays(10), validTo = now.minusDays(1)))
    }
  }

  @Test
  @DisplayName("建票种:购票后N天必须为正数 → IllegalArgumentException")
  fun `createTicketType invalid days throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createTicketType(
        1L,
        "admin",
        createRequest().copy(validityType = ValidityType.DAYS_AFTER_PURCHASE, validFrom = null, validTo = null, validDays = 0),
      )
    }
  }

  @Test
  @DisplayName("编辑:全量更新票种")
  fun `updateTicketType updates fields`() {
    val existing = TicketTypeModel(id = 1L, name = "旧票", ticketType = TicketTypeKind.UNIVERSAL, specification = "19L")
    every { ticketTypeRepository.findById(1L) } returns Optional.of(existing)
    every { ticketTypeRepository.save(any()) } answers { firstArg() }

    val updated = service.updateTicketType(1L, "admin", 1L, updateRequest(name = "新票"))

    assertEquals("新票", updated.name)
    verify {
      bizLogger.logMarketingOperation(
        targetId = 1L,
        operation = "TICKET_TYPE_UPDATED",
        result = "SUCCESS",
        userId = 1L,
        username = "admin",
        additionalData = any(),
      )
    }
  }

  private fun updateRequest(name: String = "通兑票"): TicketTypeUpdateRequest =
    TicketTypeUpdateRequest(
      name = name,
      ticketType = TicketTypeKind.UNIVERSAL,
      specification = "19L",
      packageCount = 10,
      packagePriceCents = 10000,
      giftCount = 1,
      validityType = ValidityType.FIXED_RANGE,
      validFrom = now.minusDays(1),
      validTo = now.plusDays(365),
      perUserLimit = 0,
    )

  @Test
  @DisplayName("编辑:票种不存在 → TicketTypeNotFoundException")
  fun `updateTicketType not found throws`() {
    every { ticketTypeRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<TicketTypeNotFoundException> {
      service.updateTicketType(1L, "admin", 99L, updateRequest())
    }
  }

  @Test
  @DisplayName("停用:状态置 DISABLED")
  fun `disableTicketType sets disabled`() {
    every { ticketTypeRepository.findById(1L) } returns Optional.of(TicketTypeModel(id = 1L, name = "票"))
    every { ticketTypeRepository.save(any()) } answers { firstArg() }

    val disabled = service.disableTicketType(1L, "admin", 1L)

    assertEquals(TicketTypeStatus.DISABLED, disabled.status)
    verify {
      bizLogger.logMarketingOperation(
        targetId = 1L,
        operation = "TICKET_TYPE_DISABLED",
        result = "SUCCESS",
        userId = 1L,
        username = "admin",
        additionalData = any(),
      )
    }
  }

  @Test
  @DisplayName("停用:票种不存在 → TicketTypeNotFoundException")
  fun `disableTicketType not found throws`() {
    every { ticketTypeRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<TicketTypeNotFoundException> {
      service.disableTicketType(1L, "admin", 99L)
    }
  }
}
