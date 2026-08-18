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

package dev.yidafu.aqua.client.barrel.resolver

import dev.yidafu.aqua.api.dto.PurchaseBarrelItemRequest
import dev.yidafu.aqua.api.dto.PurchaseBarrelRequest
import dev.yidafu.aqua.api.service.barrel.BarrelFeeQueryService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseMutationService
import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.client.barrel.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.BarrelFeeVo
import dev.yidafu.aqua.common.graphql.generated.BarrelPurchaseVo
import dev.yidafu.aqua.common.graphql.generated.PurchaseBarrelInput
import dev.yidafu.aqua.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

/**
 * 用户端水桶费(买水桶)Resolver(G06:一次性塑料水桶费,独立支付,每用户总限购一次)
 * 购买流程:校验价目 + 限购 → 建 PENDING 购桶单(orderNo 作为支付 out_trade_no)→ 发起微信小程序支付;
 * 支付未接入(NotImplementedError 桩)时 requiresSimulation=true,前端提示后返回。
 */
@ClientService
@Controller
class ClientBarrelResolver(
  private val barrelFeeQueryService: BarrelFeeQueryService,
  private val barrelPurchaseQueryService: BarrelPurchaseQueryService,
  private val barrelPurchaseMutationService: BarrelPurchaseMutationService,
  private val userQueryApiService: UserQueryApiService,
  private val paymentService: PaymentService,
  private val objectMapper: ObjectMapper,
) {
  /**
   * 水桶费商城:启用价目列表(升序)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun barrelShop(): List<BarrelFeeVo> = barrelFeeQueryService.listEnabled().map { it.toGraphQL() }

  /**
   * 我的水桶:当前用户最近购桶单(未购买返回 null)
   */
  @QueryMapping
  @PreAuthorize("isAuthenticated()")
  fun myBarrelPurchase(
    @AuthenticationPrincipal principal: UserPrincipal,
  ): BarrelPurchaseVo? {
    val purchase = barrelPurchaseQueryService.findLatestByUserId(principal.id) ?: return null
    val items = barrelPurchaseQueryService.findItemsByPurchaseId(purchase.id!!)
    return purchase.toGraphQL(items = items, requiresSimulation = false, paymentParams = null)
  }

  /**
   * 购买水桶(一次性塑料水桶费):建单 → 发起支付(假支付自动完成/真实模式返回 requestPayment 参数)
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun purchaseBarrel(
    @Argument @Valid input: PurchaseBarrelInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): BarrelPurchaseVo {
    val purchase =
      barrelPurchaseMutationService.purchaseBarrel(
        principal.id,
        PurchaseBarrelRequest(
          items =
            input.items.map { item ->
              PurchaseBarrelItemRequest(barrelFeeId = item.barrelFeeId, quantity = item.quantity)
            },
        ),
      )
    val items = barrelPurchaseQueryService.findItemsByPurchaseId(purchase.id!!)
    var requiresSimulation = false
    var paymentParams: ObjectNode? = null
    try {
      val openId = userQueryApiService.findById(principal.id)?.wechatOpenId.orEmpty()
      val params =
        paymentService.createWechatJsapiPayForBarrelPurchase(
          barrelPurchaseId = purchase.id!!,
          orderNo = purchase.orderNo,
          amountCents = purchase.totalAmountCents,
          description = "水桶费",
          openId = openId,
        )
      // 测试环境假支付返回 mock 参数(已自动完成);真实模式返回 requestPayment 参数
      if (params.isNotEmpty()) {
        paymentParams = objectMapper.valueToTree(params) as ObjectNode
      }
    } catch (e: NotImplementedError) {
      requiresSimulation = true
    }
    return purchase.toGraphQL(
      items = items,
      requiresSimulation = requiresSimulation,
      paymentParams = paymentParams,
    )
  }
}
