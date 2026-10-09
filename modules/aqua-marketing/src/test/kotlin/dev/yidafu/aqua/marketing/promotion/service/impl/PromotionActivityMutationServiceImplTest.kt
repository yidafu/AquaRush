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

package dev.yidafu.aqua.marketing.promotion.service.impl

import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.CreatePromotionActivityInput
import dev.yidafu.aqua.common.graphql.generated.PromotionActivityType
import dev.yidafu.aqua.common.graphql.generated.PromotionScope
import dev.yidafu.aqua.common.graphql.generated.PromotionTierInput
import dev.yidafu.aqua.common.graphql.generated.UpdatePromotionActivityInput
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.promotion.domain.exception.InvalidTierException
import dev.yidafu.aqua.marketing.promotion.domain.exception.PromotionActivityNotFoundException
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime
import java.util.Optional
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType as DomainPromotionActivityType

/** 满减/折扣活动变更服务单元测试(F-MKT-004):档位递增互斥校验、全量替换档位。 */
class PromotionActivityMutationServiceImplTest :
  FunSpec({
    var promotionActivityRepository: PromotionActivityRepository = mockk()
    var promotionActivityTierRepository: PromotionActivityTierRepository = mockk()
    var bizLogger: BizLogger = mockk()
    var service: PromotionActivityMutationServiceImpl = mockk()
    val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

    fun createRequest(
      activityType: PromotionActivityType = PromotionActivityType.FULL_REDUCE,
      scope: PromotionScope = PromotionScope.ALL,
      productId: Long? = null,
      customerGroupId: Long? = null,
      tiers: List<PromotionTierInput> =
        listOf(
          PromotionTierInput(thresholdCents = 10000, discountAmountCents = 1000),
          PromotionTierInput(thresholdCents = 20000, discountAmountCents = 2500),
        ),
    ): CreatePromotionActivityInput =
      CreatePromotionActivityInput(
        name = "满减活动",
        activityType = activityType,
        scope = scope,
        productId = productId,
        customerGroupId = customerGroupId,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
        tiers = tiers,
      )

    fun updateRequest(name: String = "满减活动"): UpdatePromotionActivityInput =
      UpdatePromotionActivityInput(
        name = name,
        activityType = PromotionActivityType.FULL_REDUCE,
        scope = PromotionScope.ALL,
        validFrom = now.minusDays(1),
        validTo = now.plusDays(30),
        tiers =
          listOf(
            PromotionTierInput(thresholdCents = 10000, discountAmountCents = 1000),
            PromotionTierInput(thresholdCents = 20000, discountAmountCents = 2500),
          ),
      )

    beforeTest {
      promotionActivityRepository = mockk()
      promotionActivityTierRepository = mockk()
      bizLogger = mockk(relaxed = true)
      service = PromotionActivityMutationServiceImpl(promotionActivityRepository, promotionActivityTierRepository, bizLogger)
    }
    test("createActivity saves activity and tiers") {
      every { promotionActivityRepository.save(any()) } answers { (firstArg() as PromotionActivityModel).apply { id = 1L } }
      val tierSlot = slot<PromotionActivityTierModel>()
      every { promotionActivityTierRepository.save(capture(tierSlot)) } answers { firstArg() }

      val saved = service.createActivity(1L, "admin", createRequest())

      saved.id shouldBe 1L
      saved.status shouldBe PromotionActivityStatus.ACTIVE
      verify(exactly = 2) { promotionActivityTierRepository.save(any()) }
      tierSlot.captured.activityId shouldBe 1L
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "PROMOTION_CREATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("createActivity invalid time throws") {
      shouldThrow<InvalidTierException> {
        service.createActivity(1L, "admin", createRequest().copy(validFrom = now.plusDays(30), validTo = now.minusDays(1)))
      }
    }
    test("createActivity non increasing tiers throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(
          1L,
          "admin",
          createRequest(
            tiers =
              listOf(
                PromotionTierInput(thresholdCents = 20000, discountAmountCents = 1000),
                PromotionTierInput(thresholdCents = 10000, discountAmountCents = 500),
              ),
          ),
        )
      }
    }
    test("createActivity full reduce tier without amount throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", createRequest(tiers = listOf(PromotionTierInput(thresholdCents = 10000))))
      }
    }
    test("createActivity full discount tier without rate throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(
          1L,
          "admin",
          createRequest(
            activityType = PromotionActivityType.FULL_DISCOUNT,
            tiers = listOf(PromotionTierInput(thresholdCents = 10000, discountAmountCents = 1000)),
          ),
        )
      }
    }
    test("createActivity product scope without product throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", createRequest(scope = PromotionScope.PRODUCT))
      }
    }
    test("createActivity group scope without group throws") {
      shouldThrow<IllegalArgumentException> {
        service.createActivity(1L, "admin", createRequest(scope = PromotionScope.GROUP))
      }
    }
    test("updateActivity replaces tiers") {
      val existing = PromotionActivityModel(id = 1L, name = "旧活动", activityType = DomainPromotionActivityType.FULL_REDUCE)
      every { promotionActivityRepository.findById(1L) } returns Optional.of(existing)
      every { promotionActivityRepository.save(any()) } answers { firstArg() }
      every { promotionActivityTierRepository.findByActivityId(1L) } returns
        listOf(PromotionActivityTierModel(id = 9L, activityId = 1L, thresholdCents = 5000, sort = 0))
      every { promotionActivityTierRepository.save(any()) } answers { firstArg() }
      every { promotionActivityTierRepository.delete(any()) } returns Unit

      val updated = service.updateActivity(1L, "admin", 1L, updateRequest(name = "新活动"))

      updated.name shouldBe "新活动"
      verify { promotionActivityTierRepository.delete(match { it.id == 9L }) }
      verify(exactly = 2) { promotionActivityTierRepository.save(any()) }
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "PROMOTION_UPDATED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("updateActivity not found throws") {
      every { promotionActivityRepository.findById(99L) } returns Optional.empty()

      shouldThrow<PromotionActivityNotFoundException> {
        service.updateActivity(1L, "admin", 99L, updateRequest())
      }
    }
    test("disableActivity sets disabled") {
      every { promotionActivityRepository.findById(1L) } returns Optional.of(PromotionActivityModel(id = 1L, name = "活动"))
      every { promotionActivityRepository.save(any()) } answers { firstArg() }

      val disabled = service.disableActivity(1L, "admin", 1L)

      disabled.status shouldBe PromotionActivityStatus.DISABLED
      verify {
        bizLogger.logMarketingOperation(
          targetId = 1L,
          operation = "PROMOTION_DISABLED",
          result = "SUCCESS",
          userId = 1L,
          username = "admin",
          additionalData = any(),
        )
      }
    }
    test("disableActivity not found throws") {
      every { promotionActivityRepository.findById(99L) } returns Optional.empty()

      shouldThrow<PromotionActivityNotFoundException> {
        service.disableActivity(1L, "admin", 99L)
      }
    }
  })
