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

/** 买赠/满赠活动变更服务单元测试(F-MKT-008):门槛/范围/赠品校验。 */
class GiftActivityMutationServiceImplTest {
  private lateinit var giftActivityRepository: GiftActivityRepository
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: GiftActivityMutationServiceImpl

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
    giftActivityRepository = mockk()
    productQueryApiService = mockk()
    bizLogger = mockk(relaxed = true)
    service = GiftActivityMutationServiceImpl(giftActivityRepository, productQueryApiService, bizLogger)
  }

  private fun request(
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

  @Test
  @DisplayName("建活动:满赠保存并记录日志(AC-MKT-008-01)")
  fun `createActivity full gift`() {
    every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)
    every { giftActivityRepository.save(any()) } answers { (firstArg() as GiftActivityModel).apply { id = 1L } }

    val saved = service.createActivity(1L, "admin", request())

    assertEquals(1L, saved.id)
    assertEquals(GiftActivityType.FULL_GIFT, saved.giftType)
    assertEquals(PromotionActivityStatus.ACTIVE, saved.status)
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

  @Test
  @DisplayName("建活动:买赠保存(按数量门槛)")
  fun `createActivity buy gift`() {
    every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)
    every { giftActivityRepository.save(any()) } answers { (firstArg() as GiftActivityModel).apply { id = 1L } }

    val saved =
      service.createActivity(
        1L,
        "admin",
        request(giftType = GiftActivityType.BUY_GIFT, thresholdCents = null, thresholdQuantity = 5),
      )

    assertEquals(5, saved.thresholdQuantity)
  }

  @Test
  @DisplayName("建活动:时间窗不合法 → InvalidGiftActivityException")
  fun `createActivity invalid time throws`() {
    assertFailsWith<InvalidGiftActivityException> {
      service.createActivity(1L, "admin", request().copy(validFrom = now.plusDays(30), validTo = now.minusDays(1)))
    }
  }

  @Test
  @DisplayName("建活动:满赠缺金额门槛 → IllegalArgumentException")
  fun `createActivity full gift missing threshold throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", request(thresholdCents = null))
    }
  }

  @Test
  @DisplayName("建活动:买赠缺数量门槛 → IllegalArgumentException")
  fun `createActivity buy gift missing threshold throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", request(giftType = GiftActivityType.BUY_GIFT, thresholdCents = null, thresholdQuantity = null))
    }
  }

  @Test
  @DisplayName("建活动:指定商品范围未选商品 → IllegalArgumentException")
  fun `createActivity product scope without product throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", request(scope = PromotionScope.PRODUCT))
    }
  }

  @Test
  @DisplayName("建活动:指定分组范围未选分组 → IllegalArgumentException")
  fun `createActivity group scope without group throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", request(scope = PromotionScope.GROUP))
    }
  }

  @Test
  @DisplayName("建活动:赠品商品不存在 → InvalidGiftActivityException")
  fun `createActivity gift product missing throws`() {
    every { productQueryApiService.findById(2L) } returns null

    assertFailsWith<InvalidGiftActivityException> {
      service.createActivity(1L, "admin", request())
    }
  }

  @Test
  @DisplayName("建活动:赠品数量无效 → IllegalArgumentException")
  fun `createActivity invalid gift quantity throws`() {
    every { productQueryApiService.findById(2L) } returns ProductModel(id = 2L, name = "赠品水", price = 1200)

    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", request(giftQuantity = 0))
    }
  }

  @Test
  @DisplayName("编辑:全量更新活动(AC-MKT-008-05)")
  fun `updateActivity updates fields`() {
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

    assertEquals("新活动", updated.name)
    assertEquals(2, updated.giftQuantity)
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

  @Test
  @DisplayName("编辑:活动不存在 → GiftActivityNotFoundException")
  fun `updateActivity not found throws`() {
    every { giftActivityRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<GiftActivityNotFoundException> {
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

  @Test
  @DisplayName("停用:状态置 DISABLED")
  fun `disableActivity sets disabled`() {
    every { giftActivityRepository.findById(1L) } returns Optional.of(GiftActivityModel(id = 1L, name = "活动"))
    every { giftActivityRepository.save(any()) } answers { firstArg() }

    val disabled = service.disableActivity(1L, "admin", 1L)

    assertEquals(PromotionActivityStatus.DISABLED, disabled.status)
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

  @Test
  @DisplayName("停用:活动不存在 → GiftActivityNotFoundException")
  fun `disableActivity not found throws`() {
    every { giftActivityRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<GiftActivityNotFoundException> {
      service.disableActivity(1L, "admin", 99L)
    }
  }
}
