/**
 * AquaRush Admin Subscription Resolver
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

package dev.yidafu.aqua.admin.subscription.resolvers

import dev.yidafu.aqua.api.dto.CreateSubscriptionRequest
import dev.yidafu.aqua.api.dto.SubscriptionItemRequest
import dev.yidafu.aqua.api.dto.SubscriptionListQuery
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.order.SubscriptionMutationService
import dev.yidafu.aqua.api.service.order.SubscriptionQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import dev.yidafu.aqua.common.graphql.generated.CreateSubscriptionInput
import dev.yidafu.aqua.common.graphql.generated.SubscriptionItemVo
import dev.yidafu.aqua.common.graphql.generated.SubscriptionListInput
import dev.yidafu.aqua.common.graphql.generated.SubscriptionVo
import dev.yidafu.aqua.common.graphql.generated.SubscriptionVoPage
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.subscription.domain.repository.SubscriptionRepository
import dev.yidafu.aqua.subscription.mapper.toDomain
import dev.yidafu.aqua.subscription.mapper.toGraphQL
import dev.yidafu.aqua.user.mapper.toGraphQL
import org.slf4j.LoggerFactory
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller

/**
 * 周期订购计划 resolver(G77/F-ORD-026,后台入口):
 * 查询 PERMISSION_SUBSCRIPTION_READ,创建/暂停/恢复/终止 PERMISSION_SUBSCRIPTION_WRITE;
 * VO 组装时逐行联查用户/地址/明细商品名/绑定送水工(单页有界,一页一请求)。
 * 明细仓储直注与 AdminOrderMapper 注入 OrderItemRepository 同模式(admin 依赖 trade)。
 */
@Controller("adminSubscriptionResolver")
class AdminSubscriptionResolver(
  private val subscriptionQueryService: SubscriptionQueryService,
  private val subscriptionMutationService: SubscriptionMutationService,
  private val subscriptionRepository: SubscriptionRepository,
  private val userQueryApiService: UserQueryApiService,
  private val addressQueryApiService: AddressQueryApiService,
  private val productQueryApiService: ProductQueryApiService,
  private val adminService: AdminService,
) {
  private val logger = LoggerFactory.getLogger(AdminSubscriptionResolver::class.java)

  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_SUBSCRIPTION_READ')")
  fun subscriptions(
    @Argument input: SubscriptionListInput?,
  ): SubscriptionVoPage {
    val page =
      subscriptionQueryService.search(
        SubscriptionListQuery(
          keyword = input?.search,
          status = input?.status?.name,
          nextRunFrom = input?.nextRunFrom,
          nextRunTo = input?.nextRunTo,
          page = input?.page ?: 0,
          size = input?.size ?: 20,
        ),
      )
    return SubscriptionVoPage(
      content = page.content.map { toVo(it) },
      totalElements = page.totalElements.toInt(),
      totalPages = page.totalPages,
      size = page.size,
      number = page.number,
    )
  }

  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_SUBSCRIPTION_READ')")
  fun subscription(
    @Argument subscriptionId: Long,
  ): SubscriptionVo = toVo(subscriptionQueryService.getById(subscriptionId))

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_SUBSCRIPTION_WRITE')")
  fun createSubscription(
    @Argument input: CreateSubscriptionInput,
    @AuthenticationPrincipal principal: UserPrincipal,
  ): SubscriptionVo {
    logger.info(
      "Admin ${principal.username}(${principal.id}) creating subscription: userId={}, addressId={}, cycle={}",
      input.userId,
      input.addressId,
      input.cycleType,
    )
    return subscriptionMutationService
      .create(principal.id, input.toRequest())
      .let { toVo(it) }
  }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_SUBSCRIPTION_WRITE')")
  fun pauseSubscription(
    @Argument subscriptionId: Long,
  ): SubscriptionVo = subscriptionMutationService.pause(subscriptionId).let { toVo(it) }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_SUBSCRIPTION_WRITE')")
  fun resumeSubscription(
    @Argument subscriptionId: Long,
  ): SubscriptionVo = subscriptionMutationService.resume(subscriptionId).let { toVo(it) }

  @MutationMapping
  @PreAuthorize("hasAuthority('PERMISSION_SUBSCRIPTION_WRITE')")
  fun terminateSubscription(
    @Argument subscriptionId: Long,
  ): SubscriptionVo = subscriptionMutationService.terminate(subscriptionId).let { toVo(it) }

  private fun CreateSubscriptionInput.toRequest(): CreateSubscriptionRequest =
    CreateSubscriptionRequest(
      userId = userId,
      addressId = addressId,
      workerId = workerId,
      items = items.map { SubscriptionItemRequest(productId = it.productId, quantity = it.quantity) },
      cycleType = cycleType.toDomain(),
      weekdays = weekdays,
      monthDay = monthDay,
      timeSlot = timeSlot.toDomain(),
      startDate = startDate,
      endDate = endDate,
      maxPeriods = maxPeriods,
      remark = remark,
    )

  /** 计划 → VO:联查用户/地址/明细商品名/绑定送水工 */
  private fun toVo(plan: SubscriptionModel): SubscriptionVo {
    val items = subscriptionRepository.findItemsBySubscriptionId(plan.id!!)
    val productNames =
      productQueryApiService
        .findByIds(items.map { it.productId })
        .associate { it.id!! to it.name }
    return SubscriptionVo(
      id = plan.id ?: 0L,
      user = userQueryApiService.findById(plan.userId)?.toGraphQL(),
      address = addressQueryApiService.findById(plan.addressId)?.toGraphQL(),
      workerId = plan.workerId,
      workerName =
        plan.workerId?.let { workerId ->
          adminService.findById(workerId)?.let { it.realName ?: it.username }
        },
      cycleType = plan.cycleType.toGraphQL(),
      weekdays =
        plan.weekdays
          ?.split(",")
          ?.mapNotNull { it.trim().toIntOrNull() }
          ?: emptyList(),
      monthDay = plan.monthDay,
      timeSlot = plan.timeSlot.toGraphQL(),
      startDate = plan.startDate.toString(),
      endDate = plan.endDate?.toString(),
      nextRunDate = plan.nextRunDate.toString(),
      maxPeriods = plan.maxPeriods,
      executedPeriods = plan.executedPeriods,
      status = plan.status.toGraphQL(),
      remark = plan.remark,
      lastError = plan.lastError,
      lastRunAt = plan.lastRunAt,
      items =
        items.map { item ->
          SubscriptionItemVo(
            productId = item.productId,
            productName = productNames[item.productId] ?: "#${item.productId}",
            quantity = item.quantity,
          )
        },
      createdAt = plan.createdAt,
    )
  }
}
