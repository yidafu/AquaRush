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

import dev.yidafu.aqua.api.dto.FirstOrderActivityCreateRequest
import dev.yidafu.aqua.api.dto.FirstOrderActivityUpdateRequest
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionScope
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.firstorder.domain.exception.FirstOrderActivityNotFoundException
import dev.yidafu.aqua.marketing.firstorder.domain.exception.InvalidFirstOrderActivityException
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
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

/** 首单立减/新人专享价活动变更服务单元测试(F-MKT-009)。 */
class FirstOrderActivityMutationServiceImplTest {
  private lateinit var firstOrderActivityRepository: FirstOrderActivityRepository
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: FirstOrderActivityMutationServiceImpl

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
    firstOrderActivityRepository = mockk()
    productQueryApiService = mockk()
    bizLogger = mockk(relaxed = true)
    service = FirstOrderActivityMutationServiceImpl(firstOrderActivityRepository, productQueryApiService, bizLogger)
  }

  private fun product(price: Long): ProductModel =
    ProductModel(
      id = 1L,
      name = "桶装水",
      price = price,
      specification = "19L",
    )

  private fun createRequest(
    activityType: FirstOrderActivityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
    thresholdCents: Long? = 5000,
    discountAmountCents: Long? = 1000,
    discountRatePermille: Long? = null,
    scope: PromotionScope = PromotionScope.ALL,
    productId: Long? = null,
    newcomerPriceCents: Long? = null,
  ): FirstOrderActivityCreateRequest =
    FirstOrderActivityCreateRequest(
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

  @Test
  @DisplayName("建活动:首单立减(满X减Y)保存并记录日志(AC-MKT-009-01)")
  fun `createActivity first order reduce`() {
    every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

    val saved = service.createActivity(1L, "admin", createRequest())

    assertEquals(1L, saved.id)
    assertEquals(PromotionActivityStatus.ACTIVE, saved.status)
    assertEquals(FirstOrderActivityType.FIRST_ORDER_REDUCE, saved.activityType)
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

  @Test
  @DisplayName("建活动:首单立减打N折(与减金额二选一)")
  fun `createActivity first order reduce with rate`() {
    every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

    val saved = service.createActivity(1L, "admin", createRequest(discountAmountCents = null, discountRatePermille = 9000))

    assertEquals(9000, saved.discountRatePermille)
  }

  @Test
  @DisplayName("建活动:时间窗不合法 → InvalidFirstOrderActivityException")
  fun `createActivity invalid time window throws`() {
    assertFailsWith<InvalidFirstOrderActivityException> {
      service.createActivity(
        1L,
        "admin",
        createRequest().copy(validFrom = now.plusDays(30), validTo = now.minusDays(1)),
      )
    }
  }

  @Test
  @DisplayName("建活动:门槛金额无效 → IllegalArgumentException")
  fun `createActivity invalid threshold throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", createRequest(thresholdCents = 0))
    }
  }

  @Test
  @DisplayName("建活动:减金额与折扣率同时填 → IllegalArgumentException")
  fun `createActivity both amount and rate throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", createRequest(discountAmountCents = 1000, discountRatePermille = 9000))
    }
  }

  @Test
  @DisplayName("建活动:首单立减仅支持全场/指定商品 → IllegalArgumentException")
  fun `createActivity reduce scope group throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", createRequest(scope = PromotionScope.GROUP))
    }
  }

  @Test
  @DisplayName("建活动:指定商品范围未选商品 → IllegalArgumentException")
  fun `createActivity product scope without product throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(1L, "admin", createRequest(scope = PromotionScope.PRODUCT, productId = null))
    }
  }

  @Test
  @DisplayName("建活动:新人专享价保存(低于目录价)(AC-MKT-009-04)")
  fun `createActivity newcomer price`() {
    every { productQueryApiService.findById(1L) } returns product(price = 2100)
    every {
      firstOrderActivityRepository.existsOverlap(
        1L,
        FirstOrderActivityType.NEWCOMER_PRICE,
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

    assertEquals(1500, saved.newcomerPriceCents)
  }

  @Test
  @DisplayName("建活动:新人价仅支持指定商品范围 → IllegalArgumentException")
  fun `createActivity newcomer scope not product throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(
        1L,
        "admin",
        createRequest(activityType = FirstOrderActivityType.NEWCOMER_PRICE, scope = PromotionScope.ALL, newcomerPriceCents = 1500),
      )
    }
  }

  @Test
  @DisplayName("建活动:新人价必须选择商品 → IllegalArgumentException")
  fun `createActivity newcomer without product throws`() {
    assertFailsWith<IllegalArgumentException> {
      service.createActivity(
        1L,
        "admin",
        createRequest(activityType = FirstOrderActivityType.NEWCOMER_PRICE, scope = PromotionScope.PRODUCT, newcomerPriceCents = 1500),
      )
    }
  }

  @Test
  @DisplayName("建活动:新人价商品不存在 → InvalidFirstOrderActivityException")
  fun `createActivity newcomer product missing throws`() {
    every { productQueryApiService.findById(1L) } returns null

    assertFailsWith<InvalidFirstOrderActivityException> {
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

  @Test
  @DisplayName("建活动:新人价不低于目录价 → InvalidFirstOrderActivityException")
  fun `createActivity newcomer price not below catalog throws`() {
    every { productQueryApiService.findById(1L) } returns product(price = 1500)

    assertFailsWith<InvalidFirstOrderActivityException> {
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

  @Test
  @DisplayName("建活动:同商品新人价时间窗重叠 → InvalidFirstOrderActivityException")
  fun `createActivity newcomer overlap throws`() {
    every { productQueryApiService.findById(1L) } returns product(price = 2100)
    every {
      firstOrderActivityRepository.existsOverlap(
        1L,
        FirstOrderActivityType.NEWCOMER_PRICE,
        PromotionActivityStatus.ACTIVE,
        any(),
        any(),
        null,
      )
    } returns true

    assertFailsWith<InvalidFirstOrderActivityException> {
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

  @Test
  @DisplayName("编辑:全量更新活动(AC-MKT-009-03)")
  fun `updateActivity updates fields`() {
    val existing = FirstOrderActivityModel(id = 1L, name = "旧名", activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE)
    every { firstOrderActivityRepository.findById(1L) } returns Optional.of(existing)
    every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

    val updated = service.updateActivity(1L, "admin", 1L, updateRequest(name = "新名"))

    assertEquals("新名", updated.name)
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

  private fun updateRequest(name: String = "首单活动"): FirstOrderActivityUpdateRequest =
    FirstOrderActivityUpdateRequest(
      name = name,
      activityType = FirstOrderActivityType.FIRST_ORDER_REDUCE,
      thresholdCents = 5000,
      discountAmountCents = 1000,
      scope = PromotionScope.ALL,
      validFrom = now.minusDays(1),
      validTo = now.plusDays(30),
    )

  @Test
  @DisplayName("编辑:活动不存在 → FirstOrderActivityNotFoundException")
  fun `updateActivity not found throws`() {
    every { firstOrderActivityRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<FirstOrderActivityNotFoundException> {
      service.updateActivity(1L, "admin", 99L, updateRequest())
    }
  }

  @Test
  @DisplayName("停用:状态置 DISABLED")
  fun `disableActivity sets disabled`() {
    every { firstOrderActivityRepository.findById(1L) } returns Optional.of(FirstOrderActivityModel(id = 1L, name = "活动"))
    every { firstOrderActivityRepository.save(any()) } answers { (firstArg() as FirstOrderActivityModel).apply { id = 1L } }

    val disabled = service.disableActivity(1L, "admin", 1L)

    assertEquals(PromotionActivityStatus.DISABLED, disabled.status)
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

  @Test
  @DisplayName("停用:活动不存在 → FirstOrderActivityNotFoundException")
  fun `disableActivity not found throws`() {
    every { firstOrderActivityRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<FirstOrderActivityNotFoundException> {
      service.disableActivity(1L, "admin", 99L)
    }
  }
}
