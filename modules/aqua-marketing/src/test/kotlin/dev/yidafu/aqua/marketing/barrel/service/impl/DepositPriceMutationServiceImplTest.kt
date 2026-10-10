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

package dev.yidafu.aqua.marketing.barrel.service.impl

import dev.yidafu.aqua.api.dto.UpsertDepositPriceRequest
import dev.yidafu.aqua.common.domain.model.DepositPriceModel
import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

/**
 * 押金价目变更服务单元测试(R-MON-03 upsert 合一):
 * 新增(规格唯一)、编辑、金额范围校验。
 */
class DepositPriceMutationServiceImplTest :
  FunSpec({
    var depositPriceRepository: DepositPriceRepository = mockk()
    var bizLogger: BizLogger = mockk()
    var service: DepositPriceMutationServiceImpl = mockk()

    fun price(
      id: Long = 1L,
      specification: String = "19L/桶",
      depositCents: Long = 4000,
      status: DepositPriceStatus = DepositPriceStatus.ENABLED,
    ): DepositPriceModel =
      DepositPriceModel(
        id = id,
        specification = specification,
        priceCents = depositCents,
        status = status,
        sortOrder = 1,
      )

    beforeTest {
      depositPriceRepository = mockk()
      bizLogger = mockk(relaxed = true)
      service = DepositPriceMutationServiceImpl(depositPriceRepository, bizLogger)
    }

    test("upsert 新增(id 空)落启用价目") {
      every { depositPriceRepository.findBySpecification("19L/桶") } returns null
      every { depositPriceRepository.save(any()) } answers { firstArg() }

      val saved =
        service.upsert(UpsertDepositPriceRequest(id = null, specification = "19L/桶", depositCents = 4000, sortOrder = 1))

      saved.status shouldBe DepositPriceStatus.ENABLED
      saved.priceCents shouldBe 4000
      verify(exactly = 0) { depositPriceRepository.findById(any<Long>()) }
    }

    test("upsert 新增规格重复拦截") {
      every { depositPriceRepository.findBySpecification("19L/桶") } returns price()

      shouldThrow<BadRequestException> {
        service.upsert(UpsertDepositPriceRequest(id = null, specification = "19L/桶", depositCents = 4000))
      }
      verify(exactly = 0) { depositPriceRepository.save(any()) }
    }

    test("upsert 编辑(id 命中)改价改状态,同规格自身不冲突") {
      val f = price()
      every { depositPriceRepository.findById(1L) } returns Optional.of(f)
      every { depositPriceRepository.findBySpecification("19L/桶") } returns f
      every { depositPriceRepository.save(any()) } answers { firstArg() }

      val saved =
        service.upsert(
          UpsertDepositPriceRequest(
            id = 1L,
            specification = "19L/桶",
            depositCents = 5000,
            status = DepositPriceStatus.DISABLED,
            sortOrder = 3,
          ),
        )

      saved.priceCents shouldBe 5000
      saved.status shouldBe DepositPriceStatus.DISABLED
      saved.sortOrder shouldBe 3
    }

    test("upsert 金额越界拦截") {
      shouldThrow<BadRequestException> {
        service.upsert(UpsertDepositPriceRequest(specification = "19L/桶", depositCents = 10_000_000L))
      }
      shouldThrow<BadRequestException> {
        service.upsert(UpsertDepositPriceRequest(specification = "19L/桶", depositCents = -1L))
      }
      verify(exactly = 0) { depositPriceRepository.save(any()) }
    }
  })
