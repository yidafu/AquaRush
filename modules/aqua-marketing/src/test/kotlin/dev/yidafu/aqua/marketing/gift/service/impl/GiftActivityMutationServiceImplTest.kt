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

package dev.yidafu.aqua.marketing.gift.service.impl

import dev.yidafu.aqua.api.dto.GiftActivityCreateRequest
import dev.yidafu.aqua.api.dto.GiftActivityUpdateRequest
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.GiftActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.gift.domain.exception.GiftActivityNotFoundException
import dev.yidafu.aqua.marketing.gift.domain.exception.InvalidGiftActivityException
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional

/** 买赠/满赠活动变更服务单元测试(F-MKT-008):门槛/范围/赠品校验。 */
class GiftActivityMutationServiceImplTest :
  FunSpec({
    var giftActivityRepository: GiftActivityRepository = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var bizLogger: BizLogger = mockk()
    var service: GiftActivityMutationServiceImpl = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun request(
      giftType: GiftActivityType = GiftActivityType.FULL_GIFT,
      thresholdCents: Long? = 10000,
      thresholdQuantity: Int? = null,
      scope: PromotionScope = PromotionScope.ALL,
      productId: Long? = null,
      customerGroupId: Long? = null,
      giftQuantity: Int = 1,
    ): GiftActivityCreateRequest =
      GiftActivityCreateRequest(
        name = "买赠活动",
        giftType = giftType,
        thresholdCents = thresholdCents,
        thresholdQuantity = thresholdQuantity,
        scope = scope,
        productId = productId,
        customerGroupId = customerGroupId,
        giftProductId = 2L,
        giftQuantity = giftQuantity,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
      )

    beforeTest {
      giftActivityRepository = mockk()
      productQueryApiService = mockk()
      bizLogger = mockk(relaxed = true)
      service = GiftActivityMutationServiceImpl(giftActivityRepository, productQueryApiService, bizLogger)
    }
    test("createActivity full gift") {
      every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)
      every { giftActivityRepository.save(any()) } answers { (firstArg() as GiftActivityModel).apply { id = 1L } }

      val saved = service.createActivity(1L, "admin", request())

      saved.id shouldBe 1L
      saved.giftType shouldBe GiftActivityType.FULL_GIFT
      saved.status shouldBe PromotionActivityStatus.ACTIVE
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "GIFT_ACTIVITY_CREATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("createActivity buy gift") {
      every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)
      every { giftActivityRepository.save(any()) } answers { (firstArg() as GiftActivityModel).apply { id = 1L } }

      val saved =
        service.createActivity(
          1L,
          "admin",
          request(giftType = GiftActivityType.BUY_GIFT, thresholdCents = null, thresholdQuantity = 5),
        )

      saved.thresholdQuantity shouldBe 5
    }
    test("createActivity invalid time throws") {
      shouldThrow<InvalidGiftActivityException> {
        service.createActivity(1L, "admin", request().copy(validFrom = now.plusDays(30), validTo = now.minusDays(1)))
      }
    }
    test("createActivity full gift missing threshold throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", request(thresholdCents = null))
      }
    }
    test("createActivity buy gift missing threshold throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", request(giftType = GiftActivityType.BUY_GIFT, thresholdCents = null, thresholdQuantity = null))
      }
    }
    test("createActivity product scope without product throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", request(scope = PromotionScope.PRODUCT))
      }
    }
    test("createActivity group scope without group throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", request(scope = PromotionScope.GROUP))
      }
    }
    test("createActivity gift product missing throws") {
      every { productQueryApiService.findById(2L) } returns null

      shouldThrow<InvalidGiftActivityException> {
        service.createActivity(1L, "admin", request())
      }
    }
    test("createActivity invalid gift quantity throws") {
      every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)

      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", request(giftQuantity = 0))
      }
    }
    test("updateActivity updates fields") {
      val existing = GiftActivityModel(id = 1L, name = "旧活动", giftType = GiftActivityType.FULL_GIFT)
      every { giftActivityRepository.findById(1L) } returns Optional.of(existing)
      every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)
      every { giftActivityRepository.save(any()) } answers { firstArg() }

      val updated =
        service.updateActivity(
          1L,
          "admin",
          1L,
          GiftActivityUpdateRequest(
            name = "新活动",
            giftType = GiftActivityType.FULL_GIFT,
            thresholdCents = 20000,
            scope = PromotionScope.ALL,
            giftProductId = 2L,
            giftQuantity = 2,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
          ),
        )

      updated.name shouldBe "新活动"
      updated.giftQuantity shouldBe 2
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "GIFT_ACTIVITY_UPDATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("updateActivity not found throws") {
      every { giftActivityRepository.findById(99L) } returns Optional.empty()

      shouldThrow<GiftActivityNotFoundException> {
        service.updateActivity(
          1L,
          "admin",
          99L,
          GiftActivityUpdateRequest(
            name = "x",
            giftType = GiftActivityType.FULL_GIFT,
            thresholdCents = 10000,
            scope = PromotionScope.ALL,
            giftProductId = 2L,
            giftQuantity = 1,
            validFrom = now.minusDays(1),
            validTo = now.plusDays(30),
          ),
        )
      }
    }
    test("disableActivity sets disabled") {
      every { giftActivityRepository.findById(1L) } returns Optional.of(GiftActivityModel(id = 1L, name = "活动"))
      every { giftActivityRepository.save(any()) } answers { firstArg() }

      val disabled = service.disableActivity(1L, "admin", 1L)

      disabled.status shouldBe PromotionActivityStatus.DISABLED
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "GIFT_ACTIVITY_DISABLED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("disableActivity not found throws") {
      every { giftActivityRepository.findById(99L) } returns Optional.empty()

      shouldThrow<GiftActivityNotFoundException> {
        service.disableActivity(1L, "admin", 99L)
      }
    }
  })
