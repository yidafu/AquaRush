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

import dev.yidafu.aqua.api.dto.FlashSaleActivityCreateRequest
import dev.yidafu.aqua.api.dto.FlashSaleActivityUpdateRequest
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.logging.util.BizLogger
import dev.yidafu.aqua.marketing.flashsale.domain.exception.FlashSaleActivityNotFoundException
import dev.yidafu.aqua.marketing.flashsale.domain.exception.InvalidFlashSaleActivityException
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
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

/** 限时特价活动变更服务单元测试(F-MKT-005):特价低于目录价、时间窗重叠拦截。 */
class FlashSaleActivityMutationServiceImplTest {
  private lateinit var flashSaleActivityRepository: FlashSaleActivityRepository
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var bizLogger: BizLogger
  private lateinit var service: FlashSaleActivityMutationServiceImpl

  private val now: LocalDateTime = LocalDateTime.of(2026, 8, 14, 10, 0)

  @BeforeEach
  fun setUp() {
    flashSaleActivityRepository = mockk()
    productQueryApiService = mockk()
    bizLogger = mockk(relaxed = true)
    service = FlashSaleActivityMutationServiceImpl(flashSaleActivityRepository, productQueryApiService, bizLogger)
  }

  private fun request(salePriceCents: Long = 1500): FlashSaleActivityCreateRequest =
    FlashSaleActivityCreateRequest(
      name = "限时特价",
      productId = 1L,
      salePriceCents = salePriceCents,
      validFrom = now.minusDays(1),
      validTo = now.plusDays(30),
    )

  @Test
  @DisplayName("建活动:特价低于目录价保存(AC-MKT-005-01)")
  fun `createActivity saves when below catalog`() {
    every { productQueryApiService.findById(1L) } returns ProductModel(id = 1L, name = "水", price = 2100)
    every { flashSaleActivityRepository.existsOverlap(1L, PromotionActivityStatus.ACTIVE, any(), any(), null) } returns false
    every { flashSaleActivityRepository.save(any()) } answers { (firstArg() as FlashSaleActivityModel).apply { id = 1L } }

    val saved = service.createActivity(1L, "admin", request())

    assertEquals(1L, saved.id)
    assertEquals(1500, saved.salePriceCents)
    assertEquals(PromotionActivityStatus.ACTIVE, saved.status)
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

  @Test
  @DisplayName("建活动:时间窗不合法 → InvalidFlashSaleActivityException")
  fun `createActivity invalid time window throws`() {
    assertFailsWith<InvalidFlashSaleActivityException> {
      service.createActivity(1L, "admin", request().copy(validFrom = now.plusDays(30), validTo = now.minusDays(1)))
    }
  }

  @Test
  @DisplayName("建活动:商品不存在 → InvalidFlashSaleActivityException")
  fun `createActivity missing product throws`() {
    every { productQueryApiService.findById(1L) } returns null

    assertFailsWith<InvalidFlashSaleActivityException> {
      service.createActivity(1L, "admin", request())
    }
  }

  @Test
  @DisplayName("建活动:特价不低于目录价 → InvalidFlashSaleActivityException")
  fun `createActivity price not below catalog throws`() {
    every { productQueryApiService.findById(1L) } returns ProductModel(id = 1L, name = "水", price = 1500)

    assertFailsWith<InvalidFlashSaleActivityException> {
      service.createActivity(1L, "admin", request(salePriceCents = 1500))
    }
  }

  @Test
  @DisplayName("建活动:同商品时间窗重叠 → InvalidFlashSaleActivityException")
  fun `createActivity overlap throws`() {
    every { productQueryApiService.findById(1L) } returns ProductModel(id = 1L, name = "水", price = 2100)
    every { flashSaleActivityRepository.existsOverlap(1L, PromotionActivityStatus.ACTIVE, any(), any(), null) } returns true

    assertFailsWith<InvalidFlashSaleActivityException> {
      service.createActivity(1L, "admin", request())
    }
  }

  @Test
  @DisplayName("编辑:全量更新特价活动(AC-MKT-005-05)")
  fun `updateActivity updates fields`() {
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
        FlashSaleActivityUpdateRequest(
          name = "新特价",
          productId = 1L,
          salePriceCents = 1200,
          validFrom = now.minusDays(1),
          validTo = now.plusDays(30),
        ),
      )

    assertEquals("新特价", updated.name)
    assertEquals(1200, updated.salePriceCents)
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

  @Test
  @DisplayName("编辑:活动不存在 → FlashSaleActivityNotFoundException")
  fun `updateActivity not found throws`() {
    every { flashSaleActivityRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<FlashSaleActivityNotFoundException> {
      service.updateActivity(
        1L,
        "admin",
        99L,
        FlashSaleActivityUpdateRequest(
          name = "x",
          productId = 1L,
          salePriceCents = 1000,
          validFrom = now.minusDays(1),
          validTo = now.plusDays(30),
        ),
      )
    }
  }

  @Test
  @DisplayName("停用:状态置 DISABLED")
  fun `disableActivity sets disabled`() {
    every { flashSaleActivityRepository.findById(1L) } returns Optional.of(FlashSaleActivityModel(id = 1L, name = "特价"))
    every { flashSaleActivityRepository.save(any()) } answers { firstArg() }

    val disabled = service.disableActivity(1L, "admin", 1L)

    assertEquals(PromotionActivityStatus.DISABLED, disabled.status)
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

  @Test
  @DisplayName("停用:活动不存在 → FlashSaleActivityNotFoundException")
  fun `disableActivity not found throws`() {
    every { flashSaleActivityRepository.findById(99L) } returns Optional.empty()

    assertFailsWith<FlashSaleActivityNotFoundException> {
      service.disableActivity(1L, "admin", 99L)
    }
  }
}
