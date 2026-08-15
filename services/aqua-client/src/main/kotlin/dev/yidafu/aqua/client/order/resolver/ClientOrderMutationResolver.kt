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

package dev.yidafu.aqua.client.order.resolver

import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryApiService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.client.order.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.common.graphql.generated.CreateOrderInput
import dev.yidafu.aqua.common.graphql.generated.Order
import dev.yidafu.aqua.common.graphql.generated.OrderPaymentParams
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.order.mapper.CreateOrderInputMapper
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

/**
 * 用户端订单变更 Resolver(F-ORD-001 下单 / F-ORD-005 在线支付):
 * - createOrder:用户下单,订单进入「待支付」(G09/R-ORD-01),userId 一律以登录用户为准;
 * - createOrderPayment:对「待支付」订单发起微信支付;测试环境假支付自动完成(autoCompleted=true),
 *   未配置商户参数时 requiresSimulation=true 由前端提示。
 */
@ClientService
@Controller
class ClientOrderMutationResolver(
  private val orderMutationService: OrderMutationService,
  private val orderQueryApiService: OrderQueryApiService,
  private val paymentService: PaymentService,
  private val userQueryApiService: UserQueryApiService,
  private val objectMapper: ObjectMapper,
) {
  /**
   * 创建订单:userId 强制取登录用户(防越权);金额由服务端促销引擎计算(R-MON-01)。
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun createOrder(
    @Argument @Valid input: CreateOrderInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): Order {
    val order =
      orderMutationService.createOrder(
        CreateOrderInputMapper.map(input).copy(userId = principal.id),
      )
    return order.toGraphQL()
  }

  /**
   * 发起订单支付(F-ORD-005):校验订单归属与待支付状态;
   * 假支付模式返回 mock 参数并自动完成支付;真实模式返回 Taro.requestPayment 参数。
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun createOrderPayment(
    @Argument orderId: Long,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): OrderPaymentParams {
    val order =
      orderQueryApiService.findById(orderId)
        ?: throw NotFoundException("订单不存在: $orderId")
    if (order.userId != principal.id) {
      throw BadRequestException("无权操作此订单")
    }
    var requiresSimulation = false
    val params: Map<String, Any> =
      try {
        val openId = userQueryApiService.findById(principal.id)?.wechatOpenId.orEmpty()
        paymentService.createWechatJsapiPay(
          orderId = orderId,
          amountCents = order.payAmountCents,
          description = "桶装水订单",
          openId = openId,
        )
      } catch (e: NotImplementedError) {
        // 微信支付未配置:提示前端支付待接入
        requiresSimulation = true
        emptyMap()
      }
    return OrderPaymentParams(
      paymentParams = if (params.isEmpty()) null else toObjectNode(params),
      requiresSimulation = requiresSimulation,
      autoCompleted = params["mock"] == true,
    )
  }

  private fun toObjectNode(params: Map<String, Any>): ObjectNode = objectMapper.valueToTree(params) as ObjectNode
}
