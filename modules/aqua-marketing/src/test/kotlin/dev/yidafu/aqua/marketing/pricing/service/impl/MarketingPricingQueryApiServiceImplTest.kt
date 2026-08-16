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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

/**
 * 营销取价服务单元测试(R-MKT-02 单价链:②分组价 → ③限时特价 → ④新人专享价 → ⑤目录价)
 */
class MarketingPricingQueryApiServiceImplTest :
  FunSpec({
    var userQueryApiService: UserQueryApiService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var groupProductPriceRepository: GroupProductPriceRepository = mockk()
    var flashSaleActivityRepository: FlashSaleActivityRepository = mockk()
    var firstOrderActivityRepository: FirstOrderActivityRepository = mockk()
    var orderQueryApiService: OrderQueryApiService = mockk()
    var service: MarketingPricingQueryApiServiceImpl = mockk()

    fun user(groupId: Long?): UserModel =
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

    fun product(price: Long): ProductModel =
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
      )

    // ============ 限时特价/新人专享价单价链(③④⑤) ============

    beforeTest {
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
    test("no group falls back to catalog price") {
      every { userQueryApiService.findById(1L) } returns user(null)
      every { productQueryApiService.findById(1L) } returns product(2100)

      val result: PriceResolution = service.resolveUnitPrice(1L, 1L)

      result.unitPriceCents shouldBe 2100
      result.priceSource shouldBe PriceSource.CATALOG_PRICE
      result.hasGroupPrice shouldBe false
      service.findGroupId(1L).shouldBeNull()
    }
    test("group without product price falls back to catalog") {
      every { userQueryApiService.findById(1L) } returns user(5L)
      every { productQueryApiService.findById(1L) } returns product(2100)
      every { groupProductPriceRepository.findByCustomerGroupIdAndProductId(5L, 1L) } returns null

      val result = service.resolveUnitPrice(1L, 1L)

      result.unitPriceCents shouldBe 2100
      result.priceSource shouldBe PriceSource.CATALOG_PRICE
      service.findGroupId(1L) shouldBe 5L
    }
    test("group price applies") {
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

      result.unitPriceCents shouldBe 1500
      result.priceSource shouldBe PriceSource.GROUP_PRICE
      result.hasGroupPrice shouldBe true
    }
    test("no newcomer config skips order query") {
      every { userQueryApiService.findById(1L) } returns user(null)
      every { productQueryApiService.findById(1L) } returns product(2100)

      service.resolveUnitPrice(1L, 1L)

      verify(exactly = 0) { orderQueryApiService.hasCompletedOrders(any()) }
    }
    test("flash sale price applies while active") {
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

      result.unitPriceCents shouldBe 1500
      result.priceSource shouldBe PriceSource.FLASH_SALE_PRICE
      result.hasFlashSale shouldBe true
      result.hasGroupPrice shouldBe false
    }
    test("newcomer price applies to new user only") {
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
      newUser.unitPriceCents shouldBe 1500
      newUser.priceSource shouldBe PriceSource.NEWCOMER_PRICE
      newUser.hasNewcomerPrice shouldBe true

      // 老用户(已有完成订单):回退目录价
      every { orderQueryApiService.hasCompletedOrders(1L) } returns true
      val oldUser = service.resolveUnitPrice(1L, 1L)
      oldUser.unitPriceCents shouldBe 2100
      oldUser.priceSource shouldBe PriceSource.CATALOG_PRICE
      oldUser.hasNewcomerPrice shouldBe false
    }
    test("price chain priority group beats flash beats newcomer") {
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

      result.unitPriceCents shouldBe 1800
      result.priceSource shouldBe PriceSource.GROUP_PRICE
      result.hasGroupPrice shouldBe true
    }
  })
