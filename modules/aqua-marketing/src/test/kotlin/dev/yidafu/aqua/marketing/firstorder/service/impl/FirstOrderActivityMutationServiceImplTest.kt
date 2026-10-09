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

package dev.yidafu.aqua.marketing.firstorder.service.impl

import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreateFirstOrderActivityInput
import dev.yidafu.aqua.common.graphql.generated.FirstOrderActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionScope
import dev.yidafu.aqua.common.graphql.generated.UpdateFirstOrderActivityInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.firstorder.domain.exception.FirstOrderActivityNotFoundException
import dev.yidafu.aqua.marketing.firstorder.domain.exception.InvalidFirstOrderActivityException
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType as DomainFirstOrderActivityType

/** 首单立减/新人专享价活动变更服务单元测试(F-MKT-009)。 */
class FirstOrderActivityMutationServiceImplTest :
  FunSpec({
    var firstOrderActivityRepository: FirstOrderActivityRepository = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: FirstOrderActivityMutationServiceImpl = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun product(price: Long): ProductModel =
      ProductModel(
        id = 1L,
        name = "桶装水",
        price = price,
        specification = "19L",
      )

    fun createRequest(
      activityType: FirstOrderActivityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
      thresholdCents: Long? = 5000,
      discountAmountCents: Long? = 1000,
      discountRatePermille: Long? = null,
      scope: PromotionScope = PromotionScope.ALL,
      productId: Long? = null,
      newcomerPriceCents: Long? = null,
    ): CreateFirstOrderActivityInput =
      CreateFirstOrderActivityInput(
        name = "首单活动",
        activityType = activityType,
        thresholdCents = thresholdCents,
        discountAmountCents = discountAmountCents,
        discountRatePermille = discountRatePermille,
        scope = scope,
        productId = productId,
        newcomerPriceCents = newcomerPriceCents,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
      )

    fun updateRequest(name: String = "首单活动"): UpdateFirstOrderActivityInput =
      UpdateFirstOrderActivityInput(
        name = name,
        activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
        thresholdCents = 5000,
        discountAmountCents = 1000,
        scope = PromotionScope.ALL,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
      )

    beforeTest {
      firstOrderActivityRepository = mockk()
      productQueryApiService = mockk()
      bizLogger = mockk(relaxed = true)
      service = FirstOrderActivityMutationServiceImpl(firstOrderActivityRepository, productQueryApiService, bizLogger)
    }
    test("createActivity first order reduce") {
      every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

      val saved = service.createActivity(1L, "admin", createRequest())

      saved.id shouldBe 1L
      saved.status shouldBe PromotionActivityStatus.ACTIVE
      saved.activityType shouldBe DomainFirstOrderActivityType.FIRST_ORDER_REDUCE
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "FIRST_ORDER_ACTIVITY_CREATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("createActivity first order reduce with rate") {
      every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

      val saved = service.createActivity(1L, "admin", createRequest(discountAmountCents = null, discountRatePermille = 9000))

      saved.discountRatePermille shouldBe 9000
    }
    test("createActivity invalid time window throws") {
      shouldThrow<InvalidFirstOrderActivityException> {
        service.createActivity(
          1L,
          "admin",
          createRequest().copy(validFrom = now.plusDays(30), validTo = now.minusDays(1)),
        )
      }
    }
    test("createActivity invalid threshold throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", createRequest(thresholdCents = 0))
      }
    }
    test("createActivity both amount and rate throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", createRequest(discountAmountCents = 1000, discountRatePermille = 9000))
      }
    }
    test("createActivity reduce scope group throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", createRequest(scope = PromotionScope.GROUP))
      }
    }
    test("createActivity product scope without product throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", createRequest(scope = PromotionScope.PRODUCT, productId = null))
      }
    }
    test("createActivity newcomer price") {
      every { productQueryApiService.findById(1L) } returns product(price = 2100)
      every {
        firstOrderActivityRepository.existsOverlap(
          1L,
          DomainFirstOrderActivityType.NEWCOMER_PRICE,
          PromotionActivityStatus.ACTIVE,
          any(),
          any(),
          null,
        )
      } returns false
      every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

      val saved =
        service.createActivity(
          1L,
          "admin",
          createRequest(
            activityType = FirstOrderActivityType.NEWCOMER_PRICE,
            scope = PromotionScope.PRODUCT,
            productId = 1L,
            newcomerPriceCents = 1500,
          ),
        )

      saved.newcomerPriceCents shouldBe 1500
    }
    test("createActivity newcomer scope not product throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(
          1L,
          "admin",
          createRequest(activityType = FirstOrderActivityType.NEWCOMER_PRICE, scope = PromotionScope.ALL, newcomerPriceCents = 1500),
        )
      }
    }
    test("createActivity newcomer without product throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(
          1L,
          "admin",
          createRequest(activityType = FirstOrderActivityType.NEWCOMER_PRICE, scope = PromotionScope.PRODUCT, newcomerPriceCents = 1500),
        )
      }
    }
    test("createActivity newcomer product missing throws") {
      every { productQueryApiService.findById(1L) } returns null

      shouldThrow<InvalidFirstOrderActivityException> {
        service.createActivity(
          1L,
          "admin",
          createRequest(
            activityType = FirstOrderActivityType.NEWCOMER_PRICE,
            scope = PromotionScope.PRODUCT,
            productId = 1L,
            newcomerPriceCents = 1500,
          ),
        )
      }
    }
    test("createActivity newcomer price not below catalog throws") {
      every { productQueryApiService.findById(1L) } returns product(price = 1500)

      shouldThrow<InvalidFirstOrderActivityException> {
        service.createActivity(
          1L,
          "admin",
          createRequest(
            activityType = FirstOrderActivityType.NEWCOMER_PRICE,
            scope = PromotionScope.PRODUCT,
            productId = 1L,
            newcomerPriceCents = 1500,
          ),
        )
      }
    }
    test("createActivity newcomer overlap throws") {
      every { productQueryApiService.findById(1L) } returns product(price = 2100)
      every {
        firstOrderActivityRepository.existsOverlap(
          1L,
          DomainFirstOrderActivityType.NEWCOMER_PRICE,
          PromotionActivityStatus.ACTIVE,
          any(),
          any(),
          null,
        )
      } returns true

      shouldThrow<InvalidFirstOrderActivityException> {
        service.createActivity(
          1L,
          "admin",
          createRequest(
            activityType = FirstOrderActivityType.NEWCOMER_PRICE,
            scope = PromotionScope.PRODUCT,
            productId = 1L,
            newcomerPriceCents = 1500,
          ),
        )
      }
    }
    test("updateActivity updates fields") {
      val existing = FirstOrderActivityModel(id = 1L, name = "旧名", activityType = DomainFirstOrderActivityType.FIRST_ORDER_REDUCE)
      every { firstOrderActivityRepository.findById(1L) } returns Optional.of(existing)
      every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

      val updated = service.updateActivity(1L, "admin", 1L, updateRequest(name = "新名"))

      updated.name shouldBe "新名"
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "FIRST_ORDER_ACTIVITY_UPDATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("updateActivity not found throws") {
      every { firstOrderActivityRepository.findById(99L) } returns Optional.empty()

      shouldThrow<FirstOrderActivityNotFoundException> {
        service.updateActivity(1L, "admin", 99L, updateRequest())
      }
    }
    test("disableActivity sets disabled") {
      every { firstOrderActivityRepository.findById(1L) } returns Optional.of(FirstOrderActivityModel(id = 1L, name = "活动"))
      every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

      val disabled = service.disableActivity(1L, "admin", 1L)

      disabled.status shouldBe PromotionActivityStatus.DISABLED
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "FIRST_ORDER_ACTIVITY_DISABLED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("disableActivity not found throws") {
      every { firstOrderActivityRepository.findById(99L) } returns Optional.empty()

      shouldThrow<FirstOrderActivityNotFoundException> {
        service.disableActivity(1L, "admin", 99L)
      }
    }
  })
