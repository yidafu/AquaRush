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

package dev.yidafu.aqua.client.barrel.resolvers

import dev.yidafu.aqua.api.dto.PurchaseBarrelRequest
import dev.yidafu.aqua.api.service.barrel.BarrelFeeQueryService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseMutationService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.BarrelFeeModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelFeeStatus
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.common.graphql.generated.PurchaseBarrelInput
import dev.yidafu.aqua.common.graphql.generated.PurchaseBarrelItemInput
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.security.core.authority.SimpleGrantedAuthority
import tools.jackson.databind.ObjectMapper

/**
 * 用户端水桶费 Resolver 测试(G06:一次性塑料水桶费,独立支付,每用户总限购一次):
 * 价目商城 / 我的水桶 / 购买(假支付自动完成、桩实现 requiresSimulation)。
 */
class ClientBarrelResolverTest :
  FunSpec({
    var barrelFeeQueryService: BarrelFeeQueryService = mockk()
    var barrelPurchaseQueryService: BarrelPurchaseQueryService = mockk()
    var barrelPurchaseMutationService: BarrelPurchaseMutationService = mockk()
    var userQueryApiService: UserQueryApiService = mockk()
    var paymentService: PaymentService = mockk()
    var resolver: ClientBarrelResolver = mockk()
    var principal: UserPrincipal = mockk()

    fun fee(id: Long = 1L) =
      BarrelFeeModel(
        id = id,
        specification = "19L/桶",
        priceCents = 4000,
        status = BarrelFeeStatus.ENABLED,
        sortOrder = 1,
      )

    fun purchase(status: BarrelPaymentStatus = BarrelPaymentStatus.PENDING) =
      BarrelPurchaseModel(
        id = 1L,
        userId = 100L,
        orderNo = "BK1001690000000000",
        totalAmountCents = 8000,
        paymentStatus = status,
      )

    beforeTest {
      barrelFeeQueryService = mockk(relaxed = true)
      barrelPurchaseQueryService = mockk(relaxed = true)
      barrelPurchaseMutationService = mockk(relaxed = true)
      userQueryApiService = mockk(relaxed = true)
      paymentService = mockk(relaxed = true)
      resolver =
        ClientBarrelResolver(
          barrelFeeQueryService,
          barrelPurchaseQueryService,
          barrelPurchaseMutationService,
          userQueryApiService,
          paymentService,
          ObjectMapper(),
        )
      principal =
        UserPrincipal(
          id = 100L,
          _username = "user100",
          userType = "USER",
          _authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
        )
    }

    test("barrelShop returns enabled fees") {
      every { barrelFeeQueryService.listEnabled() } returns listOf(fee())

      val result = resolver.barrelShop()

      result.size shouldBe 1
      result[0].specification shouldBe "19L/桶"
      result[0].priceCents shouldBe 4000L
    }

    test("myBarrelPurchase returns null when not purchased") {
      every { barrelPurchaseQueryService.findLatestByUserId(100L) } returns null

      val result = resolver.myBarrelPurchase(principal)

      result shouldBe null
    }

    test("myBarrelPurchase returns purchase with items") {
      every { barrelPurchaseQueryService.findLatestByUserId(100L) } returns purchase()
      every { barrelPurchaseQueryService.findItemsByPurchaseId(1L) } returns emptyList()

      val result = resolver.myBarrelPurchase(principal)

      result.shouldNotBeNull()
      result.orderNo shouldBe "BK1001690000000000"
    }

    test("purchaseBarrel creates purchase and auto-completes payment in mock mode") {
      val p = purchase()
      every { barrelPurchaseMutationService.purchaseBarrel(100L, any<PurchaseBarrelRequest>()) } returns p
      every { barrelPurchaseQueryService.findItemsByPurchaseId(1L) } returns emptyList()
      every { userQueryApiService.findById(100L) } returns
        UserModel(id = 100L, wechatOpenId = "openid-100", nickname = "用户")
      every { paymentService.createWechatJsapiPayForBarrelPurchase(1L, "BK1001690000000000", 8000, "水桶费", "openid-100") } returns
        mapOf("mock" to true, "paid" to true, "transactionId" to "MOCK-BK1001690000000000")

      val result =
        resolver.purchaseBarrel(
          PurchaseBarrelInput(items = listOf(PurchaseBarrelItemInput(barrelFeeId = 1L, quantity = 2))),
          principal,
        )

      result.shouldNotBeNull()
      result.requiresSimulation shouldBe false
      result.paymentParams.shouldNotBeNull()
      verify { barrelPurchaseMutationService.purchaseBarrel(100L, any<PurchaseBarrelRequest>()) }
    }

    test("purchaseBarrel sets requiresSimulation when payment not configured") {
      val p = purchase()
      every { barrelPurchaseMutationService.purchaseBarrel(100L, any<PurchaseBarrelRequest>()) } returns p
      every { barrelPurchaseQueryService.findItemsByPurchaseId(1L) } returns emptyList()
      every {
        paymentService.createWechatJsapiPayForBarrelPurchase(any(), any(), any(), any(), any())
      } throws NotImplementedError("微信支付未配置")

      val result =
        resolver.purchaseBarrel(
          PurchaseBarrelInput(items = listOf(PurchaseBarrelItemInput(barrelFeeId = 1L, quantity = 1))),
          principal,
        )

      result.requiresSimulation shouldBe true
    }
  })
