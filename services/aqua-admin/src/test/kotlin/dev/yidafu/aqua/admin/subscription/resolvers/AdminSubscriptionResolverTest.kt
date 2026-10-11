/**
 * AquaRush Admin Subscription Resolver Test
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
import dev.yidafu.aqua.api.dto.SubscriptionListQuery
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.order.SubscriptionMutationService
import dev.yidafu.aqua.api.service.order.SubscriptionQueryService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.SubscriptionItemModel
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionStatus
import dev.yidafu.aqua.common.graphql.generated.CreateSubscriptionInput
import dev.yidafu.aqua.common.graphql.generated.SubscriptionItemInput
import dev.yidafu.aqua.common.graphql.generated.SubscriptionListInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.subscription.domain.repository.SubscriptionRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.LocalDateTime
import dev.yidafu.aqua.common.graphql.generated.DeliveryTimeSlot as DeliveryTimeSlotG
import dev.yidafu.aqua.common.graphql.generated.SubscriptionCycleType as SubscriptionCycleTypeG
import dev.yidafu.aqua.common.graphql.generated.SubscriptionStatus as SubscriptionStatusG

/**
 * 周期订购计划 resolver 单测:分页默认值/筛选透传、创建入参映射(域枚举互转)、
 * VO 组装(用户/地址/明细商品名/绑定送水工)。
 */
class AdminSubscriptionResolverTest :
  FunSpec({
    var subscriptionQueryService: SubscriptionQueryService = mockk()
    var subscriptionMutationService: SubscriptionMutationService = mockk()
    var subscriptionRepository: SubscriptionRepository = mockk()
    var userQueryApiService: UserQueryApiService = mockk()
    var addressQueryApiService: AddressQueryApiService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var adminService: AdminService = mockk()
    var resolver: AdminSubscriptionResolver = mockk()

    fun plan(workerId: Long? = null) =
      SubscriptionModel(
        id = 1L,
        userId = 100L,
        addressId = 10L,
        workerId = workerId,
        cycleType = SubscriptionCycleType.WEEKLY,
        weekdays = "1,3,5",
        monthDay = null,
        timeSlot = DeliveryTimeSlot.MORNING,
        startDate = LocalDate.of(2026, 3, 2),
        nextRunDate = LocalDate.of(2026, 3, 4),
        status = SubscriptionStatus.ACTIVE,
        remark = "公司前台",
        createdBy = 7L,
        createdAt = LocalDateTime.of(2026, 3, 1, 10, 0),
      )

    beforeTest {
      subscriptionQueryService = mockk(relaxed = true)
      subscriptionMutationService = mockk(relaxed = true)
      subscriptionRepository = mockk(relaxed = true)
      userQueryApiService = mockk(relaxed = true)
      addressQueryApiService = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      resolver =
        AdminSubscriptionResolver(
          subscriptionQueryService,
          subscriptionMutationService,
          subscriptionRepository,
          userQueryApiService,
          addressQueryApiService,
          productQueryApiService,
          adminService,
        )
      every { subscriptionRepository.findItemsBySubscriptionId(1L) } returns
        listOf(SubscriptionItemModel(id = 1L, subscriptionId = 1L, productId = 5L, quantity = 2))
      every { productQueryApiService.findByIds(any()) } returns
        listOf(
          dev.yidafu.aqua.common.domain.model.ProductModel(
            id = 5L,
            name = "桶装水19L",
            price = 2000,
            stock = 50,
          ),
        )
    }

    test("subscriptions maps input filters and paginates") {
      every { subscriptionQueryService.search(any()) } returns
        PageImpl(listOf(plan()), PageRequest.of(0, 20), 1)

      val page =
        resolver.subscriptions(
          SubscriptionListInput(
            page = 0,
            size = 20,
            search = "周期客户",
            status = SubscriptionStatusG.ACTIVE,
            nextRunFrom = "2026-03-01",
            nextRunTo = "2026-03-31",
          ),
        )

      val querySlot = slot<SubscriptionListQuery>()
      verify { subscriptionQueryService.search(capture(querySlot)) }
      querySlot.captured.keyword shouldBe "周期客户"
      querySlot.captured.status shouldBe "ACTIVE"
      querySlot.captured.nextRunFrom shouldBe "2026-03-01"
      querySlot.captured.nextRunTo shouldBe "2026-03-31"

      page.totalElements shouldBe 1
      val vo = page.content.single()
      vo.id shouldBe 1L
      vo.status shouldBe SubscriptionStatusG.ACTIVE
      vo.cycleType shouldBe SubscriptionCycleTypeG.WEEKLY
      vo.timeSlot shouldBe DeliveryTimeSlotG.MORNING
      vo.weekdays shouldBe listOf(1, 3, 5)
      vo.nextRunDate shouldBe "2026-03-04"
      vo.items.single().productName shouldBe "桶装水19L"
      vo.items.single().quantity shouldBe 2
      vo.remark shouldBe "公司前台"
    }

    test("subscriptions null input uses defaults") {
      every { subscriptionQueryService.search(any()) } returns
        PageImpl(emptyList(), PageRequest.of(0, 20), 0)

      val page = resolver.subscriptions(null)

      val querySlot = slot<SubscriptionListQuery>()
      verify { subscriptionQueryService.search(capture(querySlot)) }
      querySlot.captured.page shouldBe 0
      querySlot.captured.size shouldBe 20
      querySlot.captured.keyword shouldBe null
      page.content shouldBe emptyList()
      page.totalElements shouldBe 0
    }

    test("createSubscription maps generated input to domain request with enum conversion") {
      val requestSlot = slot<CreateSubscriptionRequest>()
      every { subscriptionMutationService.create(any(), capture(requestSlot)) } returns plan()
      val principal = mockk<UserPrincipal>()
      every { principal.id } returns 7L
      every { principal.username } returns "admin"

      resolver.createSubscription(
        CreateSubscriptionInput(
          userId = 100L,
          addressId = 10L,
          workerId = 42L,
          items = listOf(SubscriptionItemInput(productId = 5L, quantity = 2)),
          cycleType = SubscriptionCycleTypeG.WEEKLY,
          weekdays = listOf(1, 3, 5),
          monthDay = null,
          timeSlot = DeliveryTimeSlotG.MORNING,
          startDate = "2026-03-02",
          endDate = null,
          maxPeriods = 12,
          remark = "公司前台",
        ),
        principal,
      )

      requestSlot.captured.userId shouldBe 100L
      requestSlot.captured.workerId shouldBe 42L
      requestSlot.captured.cycleType shouldBe SubscriptionCycleType.WEEKLY
      requestSlot.captured.timeSlot shouldBe DeliveryTimeSlot.MORNING
      requestSlot.captured.weekdays shouldBe listOf(1, 3, 5)
      requestSlot.captured.maxPeriods shouldBe 12
      requestSlot.captured.items
        .single()
        .quantity shouldBe 2
      verify { subscriptionMutationService.create(eq(7L), any()) }
    }

    test("pause/resume/terminate delegate and map status") {
      every { subscriptionMutationService.pause(1L) } returns plan().apply { status = SubscriptionStatus.PAUSED }
      every { subscriptionMutationService.resume(1L) } returns plan()
      every { subscriptionMutationService.terminate(1L) } returns plan().apply { status = SubscriptionStatus.TERMINATED }

      resolver.pauseSubscription(1L).status shouldBe SubscriptionStatusG.PAUSED
      resolver.resumeSubscription(1L).status shouldBe SubscriptionStatusG.ACTIVE
      resolver.terminateSubscription(1L).status shouldBe SubscriptionStatusG.TERMINATED

      verify { subscriptionMutationService.pause(1L) }
      verify { subscriptionMutationService.resume(1L) }
      verify { subscriptionMutationService.terminate(1L) }
    }

    test("vo carries bound worker name") {
      every { adminService.findById(42L) } returns
        AdminModel(id = 42L, username = "worker1", passwordHash = "x", realName = "王送水", role = AdminRoleModel.DELIVERY_WORKER)
      every { subscriptionQueryService.getById(1L) } returns plan(workerId = 42L)

      val vo = resolver.subscription(1L)

      vo.workerId shouldBe 42L
      vo.workerName shouldBe "王送水"
    }
  })
