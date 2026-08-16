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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

/** 桶水票票种变更服务单元测试(F-MKT-007):类型/有效期校验、停用。 */
class TicketTypeMutationServiceImplTest :
  FunSpec({
    var ticketTypeRepository: TicketTypeRepository = mockk()
    var bizLogger: BizLogger = mockk()
    var service: TicketTypeMutationServiceImpl = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun createRequest(
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

    fun updateRequest(name: String = "通兑票"): TicketTypeUpdateRequest =
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

    beforeTest {
      ticketTypeRepository = mockk()
      bizLogger = mockk(relaxed = true)
      service = TicketTypeMutationServiceImpl(ticketTypeRepository, bizLogger)
    }
    test("createTicketType universal") {
      every { ticketTypeRepository.save(any()) } answers { (firstArg() as TicketTypeModel).apply { id = 1L } }

      val saved = service.createTicketType(1L, "admin", createRequest())

      saved.id shouldBe 1L
      saved.status shouldBe TicketTypeStatus.ACTIVE
      saved.ticketType shouldBe TicketTypeKind.UNIVERSAL
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
    test("createTicketType specific product") {
      every { ticketTypeRepository.save(any()) } answers { (firstArg() as TicketTypeModel).apply { id = 1L } }

      val saved =
        service.createTicketType(
          1L,
          "admin",
          createRequest(ticketType = TicketTypeKind.SPECIFIC_PRODUCT, productId = 1L, specification = null),
        )

      saved.productId shouldBe 1L
    }
    test("createTicketType specific without product throws") {
      shouldThrow<IllegalArgumentException> {
        service.createTicketType(1L, "admin", createRequest(ticketType = TicketTypeKind.SPECIFIC_PRODUCT, specification = null))
      }
    }
    test("createTicketType universal without spec throws") {
      shouldThrow<IllegalArgumentException> {
        service.createTicketType(1L, "admin", createRequest(specification = null))
      }
    }
    test("createTicketType invalid range throws") {
      shouldThrow<IllegalArgumentException> {
        service.createTicketType(1L, "admin", createRequest().copy(validFrom = now.plusDays(10), validTo = now.minusDays(1)))
      }
    }
    test("createTicketType invalid days throws") {
      shouldThrow<IllegalArgumentException> {
        service.createTicketType(
          1L,
          "admin",
          createRequest().copy(validityType = ValidityType.DAYS_AFTER_PURCHASE, validFrom = null, validTo = null, validDays = 0),
        )
      }
    }
    test("updateTicketType updates fields") {
      val existing = TicketTypeModel(id = 1L, name = "旧票", ticketType = TicketTypeKind.UNIVERSAL, specification = "19L")
      every { ticketTypeRepository.findById(1L) } returns Optional.of(existing)
      every { ticketTypeRepository.save(any()) } answers { firstArg() }

      val updated = service.updateTicketType(1L, "admin", 1L, updateRequest(name = "新票"))

      updated.name shouldBe "新票"
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
    test("updateTicketType not found throws") {
      every { ticketTypeRepository.findById(99L) } returns Optional.empty()

      shouldThrow<TicketTypeNotFoundException> {
        service.updateTicketType(1L, "admin", 99L, updateRequest())
      }
    }
    test("disableTicketType sets disabled") {
      every { ticketTypeRepository.findById(1L) } returns Optional.of(TicketTypeModel(id = 1L, name = "票"))
      every { ticketTypeRepository.save(any()) } answers { firstArg() }

      val disabled = service.disableTicketType(1L, "admin", 1L)

      disabled.status shouldBe TicketTypeStatus.DISABLED
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
    test("disableTicketType not found throws") {
      every { ticketTypeRepository.findById(99L) } returns Optional.empty()

      shouldThrow<TicketTypeNotFoundException> {
        service.disableTicketType(1L, "admin", 99L)
      }
    }
  })
