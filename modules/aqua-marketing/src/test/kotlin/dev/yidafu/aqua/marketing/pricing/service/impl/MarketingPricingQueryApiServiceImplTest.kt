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

package dev.yidafu.aqua.marketing.pricing.service.impl

import dev.yidafu.aqua.api.service.marketing.PriceResolution
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.FirstOrderActivityType
import dev.yidafu.aqua.common.domain.model.enums.PriceSource
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.model.enums.PromotionActivityStatus
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import dev.yidafu.aqua.marketing.flashsale.domain.repository.FlashSaleActivityRepository
import dev.yidafu.aqua.marketing.pricing.domain.repository.GroupProductPriceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 营销取价服务单元测试(R-MKT-02 单价链:②分组价 → ③限时特价 → ④新人专享价 → ⑤目录价)
 */
class MarketingPricingQueryApiServiceImplTest {
  private lateinit var userQueryApiService: UserQueryApiService
  private lateinit var productQueryApiService: ProductQueryApiService
  private lateinit var groupProductPriceRepository: GroupProductPriceRepository
  private lateinit var flashSaleActivityRepository: FlashSaleActivityRepository
  private lateinit var firstOrderActivityRepository: FirstOrderActivityRepository
  private lateinit var orderQueryApiService: OrderQueryApiService
  private lateinit var service: MarketingPricingQueryApiServiceImpl

  @BeforeEach
  fun setUp() {
    userQueryApiService = mockk()
    productQueryApiService = mockk()
    groupProductPriceRepository = mockk()
    flashSaleActivityRepository = mockk()
    firstOrderActivityRepository = mockk()
    orderQueryApiService = mockk()
    service =
      MarketingPricingQueryApiServiceImpl(
        userQueryApiService,
        productQueryApiService,
        groupProductPriceRepository,
        flashSaleActivityRepository,
        firstOrderActivityRepository,
        orderQueryApiService,
      )
    // 默认:无进行中特价/新人价配置(具体用例各自覆盖)
    every { flashSaleActivityRepository.findActiveByProductId(any(), any(), any()) } returns null
    every { firstOrderActivityRepository.findActiveNewcomerPriceByProductId(any(), any(), any(), any()) } returns null
  }

  private fun user(groupId: Long?): UserModel =
    UserModel(
      id = 1L,
      wechatOpenId = "openid-1",
      nickname = "测试用户",
      phone = null,
      avatarUrl = null,
      email = "",
      status = UserStatus.ACTIVE,
      role = UserRole.USER,
      lastLoginAt = LocalDateTime.now(),
      customerGroupId = groupId,
    )

  private fun product(price: Long): ProductModel =
    ProductModel(
      id = 1L,
      name = "桶装水",
      price = price,
      originalPrice = null,
      depositPrice = null,
      coverImageUrl = "http://x/cover.png",
      imageGallery = null,
      specification = "19L",
      waterSource = "山泉水",
      mineralContent = null,
      stock = 100,
      salesVolume = 0,
      status = ProductModelStatus.ONLINE,
      sortOrder = 1,
      tags = null,
      detailContent = null,
      certificateImages = null,
      deliverySettings = null,
    )

  @Test
  @DisplayName("用户无分组 → 目录价(AC-ORD-023-02)")
  fun `no group falls back to catalog price`() {
    every { userQueryApiService.findById(1L) } returns user(null)
    every { productQueryApiService.findById(1L) } returns product(2100)

    val result: PriceResolution = service.resolveUnitPrice(1L, 1L)

    assertEquals(2100, result.unitPriceCents)
    assertEquals(PriceSource.CATALOG_PRICE, result.priceSource)
    assertFalse(result.hasGroupPrice)
    assertNull(service.findGroupId(1L))
  }

  @Test
  @DisplayName("用户属分组但该商品无分组价 → 目录价")
  fun `group without product price falls back to catalog`() {
    every { userQueryApiService.findById(1L) } returns user(5L)
    every { productQueryApiService.findById(1L) } returns product(2100)
    every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns null

    val result = service.resolveUnitPrice(1L, 1L)

    assertEquals(2100, result.unitPriceCents)
    assertEquals(PriceSource.CATALOG_PRICE, result.priceSource)
    assertEquals(5L, service.findGroupId(1L))
  }

  @Test
  @DisplayName("用户属分组且商品有分组价 → 分组价(AC-ORD-023-01)")
  fun `group price applies`() {
    every { userQueryApiService.findById(1L) } returns user(5L)
    every { productQueryApiService.findById(1L) } returns product(2100)
    every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns
      dev.yidafu.aqua.common.domain.model.GroupProductPriceModel(
        id = 1L,
        customerGroupId = 5L,
        productId = 1L,
        priceCents = 1500,
      )

    val result = service.resolveUnitPrice(1L, 1L)

    assertEquals(1500, result.unitPriceCents)
    assertEquals(PriceSource.GROUP_PRICE, result.priceSource)
    assertTrue(result.hasGroupPrice)
  }

  // ============ 限时特价/新人专享价单价链(③④⑤) ============

  @Test
  @DisplayName("无配置时不触发订单查询(避免列表页每商品查订单)")
  fun `no newcomer config skips order query`() {
    every { userQueryApiService.findById(1L) } returns user(null)
    every { productQueryApiService.findById(1L) } returns product(2100)

    service.resolveUnitPrice(1L, 1L)

    verify(exactly = 0) { orderQueryApiService.hasCompletedOrders(any()) }
  }

  @Test
  @DisplayName("限时特价:活动期按特价解析(AC-MKT-005-03);无活动回退目录价(AC-MKT-005-04)")
  fun `flash sale price applies while active`() {
    every { userQueryApiService.findById(1L) } returns user(null)
    every { productQueryApiService.findById(1L) } returns product(2100)
    every { flashSaleActivityRepository.findActiveByProductId(1L, PromotionActivityStatus.ACTIVE, any()) } returns
      FlashSaleActivityModel(
        id = 1L,
        name = "限时特价",
        productId = 1L,
        salePriceCents = 1500,
        validFrom = LocalDateTime.now().minusDays(1),
        validTo = LocalDateTime.now().plusDays(1),
        status = PromotionActivityStatus.ACTIVE,
      )

    val result = service.resolveUnitPrice(1L, 1L)

    assertEquals(1500, result.unitPriceCents)
    assertEquals(PriceSource.FLASH_SALE_PRICE, result.priceSource)
    assertTrue(result.hasFlashSale)
    assertFalse(result.hasGroupPrice)
  }

  @Test
  @DisplayName("新人专享价:新用户命中,老用户回退目录价(AC-MKT-009-04/05)")
  fun `newcomer price applies to new user only`() {
    every { userQueryApiService.findById(1L) } returns user(null)
    every { productQueryApiService.findById(1L) } returns product(2100)
    every {
      firstOrderActivityRepository.findActiveNewcomerPriceByProductId(1L, FirstOrderActivityType.NEWCOMER_PRICE, any(), any())
    } returns
      FirstOrderActivityModel(
        id = 1L,
        name = "新人价",
        activityType = FirstOrderActivityType.NEWCOMER_PRICE,
        scope = dev.yidafu.aqua.common.domain.model.enums.PromotionScope.PRODUCT,
        productId = 1L,
        newcomerPriceCents = 1500,
        validFrom = LocalDateTime.now().minusDays(1),
        validTo = LocalDateTime.now().plusDays(1),
        status = PromotionActivityStatus.ACTIVE,
      )

    // 新用户(无已完成订单)
    every { orderQueryApiService.hasCompletedOrders(1L) } returns false
    val newUser = service.resolveUnitPrice(1L, 1L)
    assertEquals(1500, newUser.unitPriceCents)
    assertEquals(PriceSource.NEWCOMER_PRICE, newUser.priceSource)
    assertTrue(newUser.hasNewcomerPrice)

    // 老用户(已有完成订单):回退目录价
    every { orderQueryApiService.hasCompletedOrders(1L) } returns true
    val oldUser = service.resolveUnitPrice(1L, 1L)
    assertEquals(2100, oldUser.unitPriceCents)
    assertEquals(PriceSource.CATALOG_PRICE, oldUser.priceSource)
    assertFalse(oldUser.hasNewcomerPrice)
  }

  @Test
  @DisplayName("单价链优先级:分组价 > 特价 > 新人价 > 目录价(AC-MKT-005-05)")
  fun `price chain priority group beats flash beats newcomer`() {
    every { userQueryApiService.findById(1L) } returns user(5L)
    every { productQueryApiService.findById(1L) } returns product(2100)
    every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns
      dev.yidafu.aqua.common.domain.model.GroupProductPriceModel(
        id = 1L,
        customerGroupId = 5L,
        productId = 1L,
        priceCents = 1800,
      )
    // 同时存在特价与新人价配置,均被分组价压过
    every { flashSaleActivityRepository.findActiveByProductId(1L, PromotionActivityStatus.ACTIVE, any()) } returns
      FlashSaleActivityModel(
        id = 1L,
        name = "特价",
        productId = 1L,
        salePriceCents = 1500,
        validFrom = LocalDateTime.now().minusDays(1),
        validTo = LocalDateTime.now().plusDays(1),
        status = PromotionActivityStatus.ACTIVE,
      )

    val result = service.resolveUnitPrice(1L, 1L)

    assertEquals(1800, result.unitPriceCents)
    assertEquals(PriceSource.GROUP_PRICE, result.priceSource)
    assertTrue(result.hasGroupPrice)
  }
}
