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

import dev.yidafu.aqua.api.dto.PromotionActivityCreateRequest
import dev.yidafu.aqua.api.dto.PromotionActivityUpdateRequest
import dev.yidafu.aqua.api.dto.PromotionTierItem
import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.domain.model.PromotionActivityTierModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.promotion.domain.exception.InvalidTierException
import dev.yidafu.aqua.marketing.promotion.domain.exception.PromotionActivityNotFoundException
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityRepository
import dev.yidafu.aqua.marketing.promotion.domain.repository.PromotionActivityTierRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** 满减/折扣活动变更服务单元测试(F-MKT-004):档位递增互斥校验、全量替换档位。 */
class PromotionActivityMutationServiceImplTest {
  private lateinit var promotionActivityRepository: PromotionActivityRepository
  private lateinit var promotionActivityTierRepository: PromotionActivityTierRepository
  private lateinit var bizLogger: BizLogger
  private lateinit var service: PromotionActivityMutationServiceImpl

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
    promotionActivityRepository = mockk()
    promotionActivityTierRepository = mockk()
    bizLogger = mockk(relaxed = true)
    service = PromotionActivityMutationServiceImpl(promotionActivityRepository, promotionActivityTierRepository, bizLogger)
  }

  private fun createRequest(
    activityType: PromotionActivityType = PromotionActivityType.FULL_REDUCE,
    scope: PromotionScope = PromotionScope.ALL,
    productId: Long? = null,
    customerGroupId: Long? = null,
    tiers: List<PromotionTierItem> =
      listOf(
        PromotionTierItem(thresholdCents = 10000, discountAmountCents = 1000),
        PromotionTierItem(thresholdCents = 20000, discountAmountCents = 2500),
      ),
  ): PromotionActivityCreateRequest =
    PromotionActivityCreateRequest(
      name = "满减活动",
      activityType = activityType,
      scope = scope,
      productId = productId,
      customerGroupId = customerGroupId,
      validFrom = now.minusDays(1),
      validTo = now.plusDays(30),
      tiers = tiers,
    )

  @Test
  @DisplayName("建活动:保存活动与档位并记录日志(AC-MKT-004-01)")
  fun `createActivity saves activity and tiers`() {
    every { promotionActivityRepository.save(any()) } answers { (firstArg() as PromotionActivityModel).apply { id = 1L } }
    val tierSlot = slot<PromotionActivityTierModel>()
    every { promotionActivityTierRepository.save(capture(tierSlot)) } answers { firstArg() }

    val saved = service.createActivity(1L, "admin", createRequest())

    assertEquals(1L, saved.id)
    assertEquals(PromotionActivityStatus.ACTIVE, saved.status)
    verify(exactly = 2) { promotionActivityTierRepository.save(any()) }
    assertEquals(1L, tierSlot.captured.activityId)
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

  @Test
  @DisplayName("建活动:时间窗不合法 → InvalidTierException")
  fun `createActivity invalid time throws`() {
    assertFailsWith<InvalidTierException> {
      service.createActivity(1L, "admin", createRequest().copy(validFrom = now.plusDays(30), validTo = now.minusDays(1)))
    }
  }

  @Test
  @DisplayName("建活动:档位金额未递增 → IllegalArgumentException")
  fun `createActivity non increasing tiers throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(
        1L,
        "admin",
        createRequest(
          tiers =
            listOf(
              PromotionTierItem(thresholdCents = 20000, discountAmountCents = 1000),
              PromotionTierItem(thresholdCents = 10000, discountAmountCents = 500),
            ),
        ),
      )
    }
  }

  @Test
  @DisplayName("建活动:满减档位缺满减金额 → IllegalArgumentException")
  fun `createActivity full reduce tier without amount throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", createRequest(tiers = listOf(PromotionTierItem(thresholdCents = 10000))))
    }
  }

  @Test
  @DisplayName("建活动:满折档位缺折扣率 → IllegalArgumentException")
  fun `createActivity full discount tier without rate throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(
        1L,
        "admin",
        createRequest(
          activityType = PromotionActivityType.FULL_DISCOUNT,
          tiers = listOf(PromotionTierItem(thresholdCents = 10000, discountAmountCents = 1000)),
        ),
      )
    }
  }

  @Test
  @DisplayName("建活动:指定商品范围未选商品 → IllegalArgumentException")
  fun `createActivity product scope without product throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", createRequest(scope = PromotionScope.PRODUCT))
    }
  }

  @Test
  @DisplayName("建活动:指定分组范围未选分组 → IllegalArgumentException")
  fun `createActivity group scope without group throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", createRequest(scope = PromotionScope.GROUP))
    }
  }

  @Test
  @DisplayName("编辑:全量替换档位(旧档位删除)(AC-MKT-004-03)")
  fun `updateActivity replaces tiers`() {
    val existing = PromotionActivityModel(id = 1L, name = "旧活动", activityType = PromotionActivityType.FULL_REDUCE)
    every { promotionActivityRepository.findById(1L) } returns Optional.of(existing)
    every { promotionActivityRepository.save(any()) } answers { firstArg() }
    every { promotionActivityTierRepository.findByActivityId(1L) } returns
      listOf(PromotionActivityTierModel(id = 9L, activityId = 1L, thresholdCents = 5000, sort = 0))
    every { promotionActivityTierRepository.save(any()) } answers { firstArg() }
    every { promotionActivityTierRepository.delete(any()) } returns Unit

    val updated = service.updateActivity(1L, "admin", 1L, updateRequest(name = "新活动"))

    assertEquals("新活动", updated.name)
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

  private fun updateRequest(name: String = "满减活动"): PromotionActivityUpdateRequest =
    PromotionActivityUpdateRequest(
      name = name,
      activityType = PromotionActivityType.FULL_REDUCE,
      scope = PromotionScope.ALL,
      validFrom = now.minusDays(1),
      validTo = now.plusDays(30),
      tiers =
        listOf(
          PromotionTierItem(thresholdCents = 10000, discountAmountCents = 1000),
          PromotionTierItem(thresholdCents = 20000, discountAmountCents = 2500),
        ),
    )

  @Test
  @DisplayName("编辑:活动不存在 → PromotionActivityNotFoundException")
  fun `updateActivity not found throws`() {
    every { promotionActivityRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<PromotionActivityNotFoundException> {
      service.updateActivity(1L, "admin", 99L, updateRequest())
    }
  }

  @Test
  @DisplayName("停用:状态置 DISABLED")
  fun `disableActivity sets disabled`() {
    every { promotionActivityRepository.findById(1L) } returns Optional.of(PromotionActivityModel(id = 1L, name = "活动"))
    every { promotionActivityRepository.save(any()) } answers { firstArg() }

    val disabled = service.disableActivity(1L, "admin", 1L)

    assertEquals(PromotionActivityStatus.DISABLED, disabled.status)
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

  @Test
  @DisplayName("停用:活动不存在 → PromotionActivityNotFoundException")
  fun `disableActivity not found throws`() {
    every { promotionActivityRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<PromotionActivityNotFoundException> {
      service.disableActivity(1L, "admin", 99L)
    }
  }
}
