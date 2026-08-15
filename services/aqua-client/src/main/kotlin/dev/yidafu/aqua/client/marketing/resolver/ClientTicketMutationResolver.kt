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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.yidafu.aqua.client.marketing.resolver

import dev.yidafu.aqua.api.dto.PurchaseTicketsRequest
import dev.yidafu.aqua.api.service.marketing.TicketMutationApiService
import dev.yidafu.aqua.api.service.marketing.TicketTypeQueryService
import dev.yidafu.aqua.api.service.payment.PaymentService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.client.marketing.mapper.toGraphQL
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.PurchaseTicketsInput
import dev.yidafu.aqua.common.graphql.generated.TicketPurchaseVo
import dev.yidafu.aqua.common.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

/**
 * 用户端桶水票变更 Resolver(F-MKT-007 购票)
 * 购票即预收款项,一经购买不支持退款(R-MKT-08);支付为桩实现,待微信支付 SDK 接入。
 */
@ClientService
@Controller
class ClientTicketMutationResolver(
  private val ticketMutationApiService: TicketMutationApiService,
  private val ticketTypeQueryService: TicketTypeQueryService,
  private val userQueryApiService: UserQueryApiService,
  private val paymentService: PaymentService,
  private val objectMapper: ObjectMapper,
) {
  /**
   * 购买桶水票:创建 PENDING 购票单(orderNo 作为支付 out_trade_no)后发起微信小程序支付;
   * 支付未接入(NotImplementedError 桩)时 requiresSimulation=true,前端提示后返回。
   */
  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun purchaseTickets(
    @Argument @Valid input: PurchaseTicketsInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): TicketPurchaseVo {
    val purchase =
      ticketMutationApiService.purchaseTickets(
        principal.id,
        PurchaseTicketsRequest(ticketTypeId = input.ticketTypeId, packageCount = input.packageCount),
      )
    val ticketTypeName = ticketTypeQueryService.getTicketType(purchase.ticketTypeId)?.name ?: "桶水票"
    var requiresSimulation = false
    var paymentParams: ObjectNode? = null
    try {
      val openId = userQueryApiService.findById(principal.id)?.wechatOpenId.orEmpty()
      val params =
        paymentService.createWechatJsapiPayForTicket(
          ticketPurchaseId = purchase.id!!,
          orderNo = purchase.orderNo,
          amountCents = purchase.packagePriceCents,
          description = "桶水票",
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
      ticketTypeName = ticketTypeName,
      requiresSimulation = requiresSimulation,
      paymentParams = paymentParams,
    )
  }
}
