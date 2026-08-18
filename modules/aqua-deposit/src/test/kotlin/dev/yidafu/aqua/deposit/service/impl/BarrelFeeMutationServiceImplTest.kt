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

package dev.yidafu.aqua.deposit.service.impl

import dev.yidafu.aqua.api.dto.CreateBarrelFeeRequest
import dev.yidafu.aqua.api.dto.UpdateBarrelFeeRequest
import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.deposit.domain.repository.BarrelFeeRepository
import dev.yidafu.aqua.logging.util.BizLogger
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 水桶费价目变更服务单元测试(G06:独立价目表,与商品解耦):
 * 新增(规格唯一)、编辑、停用(幂等)。
 */
class BarrelFeeMutationServiceImplTest :
  FunSpec({
    var barrelFeeRepository: BarrelFeeRepository = mockk()
    var bizLogger: BizLogger = mockk()
    var service: BarrelFeeMutationServiceImpl = mockk()

    fun fee(
      id: Long = 1L,
      specification: String = "19L/桶",
      priceCents: Long = 4000,
      status: BarrelFeeStatus = BarrelFeeStatus.ENABLED,
    ): BarrelFeeModel =
      BarrelFeeModel(
        id = id,
        specification = specification,
        priceCents = priceCents,
        status = status,
        sortOrder = 1,
      )

    beforeTest {
      barrelFeeRepository = mockk()
      bizLogger = mockk(relaxed = true)
      service = BarrelFeeMutationServiceImpl(barrelFeeRepository, bizLogger)
    }

    test("create saves enabled fee") {
      every { barrelFeeRepository.findBySpecification("19L/桶") } returns null
      every { barrelFeeRepository.save(any()) } answers { firstArg() }

      val saved = service.create(CreateBarrelFeeRequest(specification = "19L/桶", priceCents = 4000, sortOrder = 1))

      saved.status shouldBe BarrelFeeStatus.ENABLED
      saved.priceCents shouldBe 4000
    }

    test("create rejects duplicate specification") {
      every { barrelFeeRepository.findBySpecification("19L/桶") } returns fee()

      shouldThrow<BadRequestException> {
        service.create(CreateBarrelFeeRequest(specification = "19L/桶", priceCents = 4000))
      }
      verify(exactly = 0) { barrelFeeRepository.save(any()) }
    }

    test("update modifies price and sort") {
      val f = fee()
      every { barrelFeeRepository.findById(1L) } returns Optional.of(f)
      every { barrelFeeRepository.save(any()) } answers { firstArg() }

      val saved = service.update(1L, UpdateBarrelFeeRequest(priceCents = 5000, sortOrder = 3))

      saved.priceCents shouldBe 5000
      saved.sortOrder shouldBe 3
    }

    test("disable marks DISABLED") {
      val f = fee()
      every { barrelFeeRepository.findById(1L) } returns Optional.of(f)
      every { barrelFeeRepository.save(any()) } answers { firstArg() }

      val saved = service.disable(1L)

      saved.status shouldBe BarrelFeeStatus.DISABLED
    }

    test("disable is idempotent when already disabled") {
      val f = fee(status = BarrelFeeStatus.DISABLED)
      every { barrelFeeRepository.findById(1L) } returns Optional.of(f)

      val saved = service.disable(1L)

      saved.status shouldBe BarrelFeeStatus.DISABLED
      verify(exactly = 0) { barrelFeeRepository.save(any()) }
    }
  })
