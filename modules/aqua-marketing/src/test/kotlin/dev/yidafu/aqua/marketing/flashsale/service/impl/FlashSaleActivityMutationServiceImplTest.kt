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

package dev.yidafu.aqua.marketing.flashsale.service.impl

import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreateFlashSaleActivityInput
import dev.yidafu.aqua.common.graphql.generated.UpdateFlashSaleActivityInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.flashsale.domain.exception.FlashSaleActivityNotFoundException
import dev.yidafu.aqua.marketing.flashsale.domain.exception.InvalidFlashSaleActivityException
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

/** 限时特价活动变更服务单元测试(F-MKT-005):特价低于目录价、时间窗重叠拦截。 */
class FlashSaleActivityMutationServiceImplTest :
  FunSpec({
    var flashSaleActivityRepository: FlashSaleActivityRepository = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: FlashSaleActivityMutationServiceImpl = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun request(salePriceCents: Long = 1500): CreateFlashSaleActivityInput =
      CreateFlashSaleActivityInput(
        name = "限时特价",
        productId = 1L,
        salePriceCents = salePriceCents,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
      )

    beforeTest {
      flashSaleActivityRepository = mockk()
      productQueryApiService = mockk()
      bizLogger = mockk(relaxed = true)
      service = FlashSaleActivityMutationServiceImpl(flashSaleActivityRepository, productQueryApiService, bizLogger)
    }
    test("createActivity saves when below catalog") {
      every { productQueryApiService.findById(1L) } returns ProductModel(id = 1L, name = "水", price = 2100)
      every { flashSaleActivityRepository.existsOverlap(1L, PromotionActivityStatus.ACTIVE, any(), any(), null) } returns false
      every { flashSaleActivityRepository.save(any()) } answers { (firstArg() as FlashSaleActivityModel).apply { id = 1L } }

      val saved = service.createActivity(1L, "admin", request())

      saved.id shouldBe 1L
      saved.salePriceCents shouldBe 1500
      saved.status shouldBe PromotionActivityStatus.ACTIVE
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "FLASH_SALE_CREATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("createActivity invalid time window throws") {
      shouldThrow<InvalidFlashSaleActivityException> {
        service.createActivity(1L, "admin", request().copy(validFrom = now.plusDays(30), validTo = now.minusDays(1)))
      }
    }
    test("createActivity missing product throws") {
      every { productQueryApiService.findById(1L) } returns null

      shouldThrow<InvalidFlashSaleActivityException> {
        service.createActivity(1L, "admin", request())
      }
    }
    test("createActivity price not below catalog throws") {
      every { productQueryApiService.findById(1L) } returns ProductModel(id = 1L, name = "水", price = 1500)

      shouldThrow<InvalidFlashSaleActivityException> {
        service.createActivity(1L, "admin", request(salePriceCents = 1500))
      }
    }
    test("createActivity overlap throws") {
      every { productQueryApiService.findById(1L) } returns ProductModel(id = 1L, name = "水", price = 2100)
      every { flashSaleActivityRepository.existsOverlap(1L, PromotionActivityStatus.ACTIVE, any(), any(), null) } returns true

      shouldThrow<InvalidFlashSaleActivityException> {
        service.createActivity(1L, "admin", request())
      }
    }
    test("updateActivity updates fields") {
      val existing = FlashSaleActivityModel(id = 1L, name = "旧特价", productId = 1L, salePriceCents = 1000)
      every { flashSaleActivityRepository.findById(1L) } returns Optional.of(existing)
      every { productQueryApiService.findById(1L) } returns ProductModel(id = 1L, name = "水", price = 2100)
      every { flashSaleActivityRepository.existsOverlap(1L, PromotionActivityStatus.ACTIVE, any(), any(), 1L) } returns false
      every { flashSaleActivityRepository.save(any()) } answers { firstArg() }

      val updated =
        service.updateActivity(
          1L,
          "admin",
          1L,
          UpdateFlashSaleActivityInput(
            name = "新特价",
            productId = 1L,
            salePriceCents = 1200,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
          ),
        )

      updated.name shouldBe "新特价"
      updated.salePriceCents shouldBe 1200
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "FLASH_SALE_UPDATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("updateActivity not found throws") {
      every { flashSaleActivityRepository.findById(99L) } returns Optional.empty()

      shouldThrow<FlashSaleActivityNotFoundException> {
        service.updateActivity(
          1L,
          "admin",
          99L,
          UpdateFlashSaleActivityInput(
            name = "x",
            productId = 1L,
            salePriceCents = 1000,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
          ),
        )
      }
    }
    test("disableActivity sets disabled") {
      every { flashSaleActivityRepository.findById(1L) } returns Optional.of(FlashSaleActivityModel(id = 1L, name = "特价"))
      every { flashSaleActivityRepository.save(any()) } answers { firstArg() }

      val disabled = service.disableActivity(1L, "admin", 1L)

      disabled.status shouldBe PromotionActivityStatus.DISABLED
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "FLASH_SALE_DISABLED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("disableActivity not found throws") {
      every { flashSaleActivityRepository.findById(99L) } returns Optional.empty()

      shouldThrow<FlashSaleActivityNotFoundException> {
        service.disableActivity(1L, "admin", 99L)
      }
    }
  })
