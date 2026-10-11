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

package dev.yidafu.aqua.admin.marketing.resolvers

import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.api.service.marketing.TicketQueryApiService
import dev.yidafu.aqua.api.service.marketing.TicketTypeQueryService
import dev.yidafu.aqua.common.domain.model.UserCouponModel
import dev.yidafu.aqua.common.domain.model.UserTicketModel
import dev.yidafu.aqua.common.graphql.generated.UserCouponVo
import dev.yidafu.aqua.common.graphql.generated.UserTicketVo
import dev.yidafu.aqua.marketing.coupon.mapper.toGraphQL
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus as DomainUserCouponStatus
import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus as DomainUserTicketStatus
import dev.yidafu.aqua.common.graphql.generated.UserCouponStatus as GraphQLUserCouponStatus
import dev.yidafu.aqua.common.graphql.generated.UserTicketStatus as GraphQLUserTicketStatus

/**
 * 用户持有券/票查询 Resolver(管理后台,PERMISSION_USER_READ):
 * 供用户详情页「优惠券」「水票」Tab 展示(PRD 10-marketing.md §F-MKT-003/007),
 * 复用 CouponQueryApiService/TicketQueryApiService 用户侧查询,按 userId 显式过滤。
 * 枚举同名条目 when 穷举互转;usable 字段后台列表恒 false(与用户端列表一致)。
 */
@Controller
class AdminUserCouponTicketQueryResolver(
  private val couponQueryApiService: CouponQueryApiService,
  private val ticketQueryApiService: TicketQueryApiService,
  private val ticketTypeQueryService: TicketTypeQueryService,
) {
  /**
   * 用户持有的优惠券(状态空=全部)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_USER_READ')")
  fun userCoupons(
    @Argument userId: Long,
    @Argument status: GraphQLUserCouponStatus?,
  ): List<UserCouponVo> = couponQueryApiService.findUserCoupons(userId, status?.toDomain()).map { it.toGraphQL() }

  /**
   * 用户持有的桶水票(状态空=全部;票种名称/通兑票限定规格取自票种;
   * pendingActivation=G97 复购激活待激活展示态,按该用户口径服务端判定)
   */
  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_USER_READ')")
  fun userTickets(
    @Argument userId: Long,
    @Argument status: GraphQLUserTicketStatus?,
  ): List<UserTicketVo> {
    val tickets = ticketQueryApiService.findUserTickets(userId, status?.toDomain())
    val pendingIds = ticketQueryApiService.findPendingActivationIds(userId, tickets)
    return tickets.map { ticket ->
      val ticketType = ticketTypeQueryService.getTicketType(ticket.ticketTypeId)
      ticket.toGraphQL(
        ticketTypeName = ticketType?.name ?: "水票",
        specification = ticketType?.specification,
        pendingActivation = ticket.id != null && pendingIds.contains(ticket.id!!),
        activationOrderCount = ticketType?.activationOrderCount,
      )
    }
  }

  private fun GraphQLUserCouponStatus.toDomain(): DomainUserCouponStatus =
    when (this) {
      GraphQLUserCouponStatus.EXPIRED -> DomainUserCouponStatus.EXPIRED
      GraphQLUserCouponStatus.RECEIVED -> DomainUserCouponStatus.RECEIVED
      GraphQLUserCouponStatus.RETURNED -> DomainUserCouponStatus.RETURNED
      GraphQLUserCouponStatus.USED -> DomainUserCouponStatus.USED
    }

  private fun GraphQLUserTicketStatus.toDomain(): DomainUserTicketStatus =
    when (this) {
      GraphQLUserTicketStatus.EXPIRED -> DomainUserTicketStatus.EXPIRED
      GraphQLUserTicketStatus.RETURNED -> DomainUserTicketStatus.RETURNED
      GraphQLUserTicketStatus.UNUSED -> DomainUserTicketStatus.UNUSED
      GraphQLUserTicketStatus.USED -> DomainUserTicketStatus.USED
    }

  private fun UserCouponModel.toGraphQL(): UserCouponVo =
    UserCouponVo(
      id = id ?: 0L,
      name = name,
      couponType = couponType.toGraphQL(),
      thresholdCents = thresholdCents,
      discountAmountCents = discountAmountCents,
      discountRatePermille = discountRatePermille,
      scope = scope.toGraphQL(),
      productId = productId,
      status = status.toGraphQL(),
      expireTime = expireTime,
      usable = false,
    )

  private fun UserTicketModel.toGraphQL(
    ticketTypeName: String,
    specification: String?,
    pendingActivation: Boolean,
    activationOrderCount: Int?,
  ): UserTicketVo =
    UserTicketVo(
      id = id ?: 0L,
      ticketNo = ticketNo,
      ticketTypeName = ticketTypeName,
      specification = specification,
      status = status.toGraphQL(),
      expireTime = expireTime,
      isGift = isGift,
      pendingActivation = pendingActivation,
      activationOrderCount = activationOrderCount,
    )
}

private fun DomainUserCouponStatus.toGraphQL(): GraphQLUserCouponStatus =
  when (this) {
    DomainUserCouponStatus.RECEIVED -> GraphQLUserCouponStatus.RECEIVED
    DomainUserCouponStatus.USED -> GraphQLUserCouponStatus.USED
    DomainUserCouponStatus.RETURNED -> GraphQLUserCouponStatus.RETURNED
    DomainUserCouponStatus.EXPIRED -> GraphQLUserCouponStatus.EXPIRED
  }

private fun DomainUserTicketStatus.toGraphQL(): GraphQLUserTicketStatus =
  when (this) {
    DomainUserTicketStatus.UNUSED -> GraphQLUserTicketStatus.UNUSED
    DomainUserTicketStatus.USED -> GraphQLUserTicketStatus.USED
    DomainUserTicketStatus.RETURNED -> GraphQLUserTicketStatus.RETURNED
    DomainUserTicketStatus.EXPIRED -> GraphQLUserTicketStatus.EXPIRED
  }
