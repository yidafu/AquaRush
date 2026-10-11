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

package dev.yidafu.aqua.subscription.service.impl

import dev.yidafu.aqua.api.dto.CreateSubscriptionRequest
import dev.yidafu.aqua.api.dto.SubscriptionItemRequest
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.product.ProductQueryApiService
import dev.yidafu.aqua.api.service.user.AddressQueryApiService
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.SubscriptionItemModel
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryTimeSlot
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionCycleType
import dev.yidafu.aqua.common.domain.model.enums.SubscriptionStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.subscription.domain.repository.SubscriptionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 周期订购计划管理测试(R-ORD-13):创建校验(地址归属/商品在售/周期参数/绑定送水工/首期日期)
 * 与状态机(暂停/恢复/终止,恢复时错过期次不补)。
 */
class SubscriptionMutationServiceImplTest :
  FunSpec({
    var subscriptionRepository: SubscriptionRepository = mockk()
    var addressQueryApiService: AddressQueryApiService = mockk()
    var productQueryApiService: ProductQueryApiService = mockk()
    var adminService: AdminService = mockk()
    var service: SubscriptionMutationServiceImpl = mockk()

    val onlineProduct = ProductModel(id = 5L, name = "桶装水19L", price = 2000, stock = 50, status = ProductModelStatus.ONLINE)

    fun address(
      id: Long = 10L,
      userId: Long = 100L,
    ) = AddressModel(
      id = id,
      userId = userId,
      receiverName = "张三",
      phone = "13800138000",
      province = "浙江省",
      city = "杭州市",
      district = "西湖区",
      detailAddress = "文一西路100号",
      floor = 5,
    )

    fun weeklyRequest(startDate: LocalDate = LocalDate.of(2026, 3, 2)) =
      CreateSubscriptionRequest(
        userId = 100L,
        addressId = 10L,
        items = listOf(SubscriptionItemRequest(productId = 5L, quantity = 2)),
        cycleType = SubscriptionCycleType.WEEKLY,
        weekdays = listOf(1),
        timeSlot = DeliveryTimeSlot.MORNING,
        startDate = startDate.toString(),
        remark = "公司前台",
      )

    fun stubHappyPath() {
      every { addressQueryApiService.findById(10L) } returns address()
      every { productQueryApiService.findById(5L) } returns onlineProduct
      every { subscriptionRepository.save(any<SubscriptionModel>()) } answers {
        (firstArg() as SubscriptionModel).also { it.id = 1L }
      }
    }

    beforeTest {
      subscriptionRepository = mockk(relaxed = true)
      addressQueryApiService = mockk(relaxed = true)
      productQueryApiService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      service = SubscriptionMutationServiceImpl(subscriptionRepository, addressQueryApiService, productQueryApiService, adminService)
      stubHappyPath()
    }

    test("create weekly plan computes first run, persists items with plan id") {
      val plan = service.create(7L, weeklyRequest())

      plan.id shouldBe 1L
      plan.status shouldBe SubscriptionStatus.ACTIVE
      plan.executedPeriods shouldBe 0
      plan.createdBy shouldBe 7L
      plan.weekdays shouldBe "1"
      // 2026-03-02 是周一,weekdays=[1] → 首期即生效日
      plan.nextRunDate shouldBe LocalDate.of(2026, 3, 2)

      val itemsSlot = slot<List<SubscriptionItemModel>>()
      verify { subscriptionRepository.saveItems(capture(itemsSlot)) }
      itemsSlot.captured.single().subscriptionId shouldBe 1L
      itemsSlot.captured.single().productId shouldBe 5L
      itemsSlot.captured.single().quantity shouldBe 2
    }

    test("create monthly plan computes first monthly run") {
      val request =
        weeklyRequest().copy(
          cycleType = SubscriptionCycleType.MONTHLY,
          weekdays = null,
          monthDay = 10,
          startDate = "2026-03-15",
        )
      val plan = service.create(7L, request)
      plan.nextRunDate shouldBe LocalDate.of(2026, 4, 10)
      plan.monthDay shouldBe 10
      plan.weekdays shouldBe null
    }

    test("create rejects address belonging to another user") {
      every { addressQueryApiService.findById(10L) } returns address(userId = 999L)
      shouldThrow<BadRequestException> { service.create(7L, weeklyRequest()) }
    }

    test("create rejects missing address") {
      every { addressQueryApiService.findById(10L) } returns null
      shouldThrow<NotFoundException> { service.create(7L, weeklyRequest()) }
    }

    test("create rejects offline product") {
      every { productQueryApiService.findById(5L) } returns
        ProductModel(id = 5L, name = "桶装水19L", price = 2000, stock = 50, status = ProductModelStatus.OFFLINE)
      val error = shouldThrow<BadRequestException> { service.create(7L, weeklyRequest()) }
      error.message!!.contains("下架") shouldBe true
    }

    test("create rejects duplicate products") {
      val request =
        weeklyRequest().copy(
          items =
            listOf(
              SubscriptionItemRequest(productId = 5L, quantity = 1),
              SubscriptionItemRequest(productId = 5L, quantity = 2),
            ),
        )
      shouldThrow<BadRequestException> { service.create(7L, request) }
    }

    test("create rejects weekly without weekdays") {
      val request = weeklyRequest().copy(weekdays = null)
      shouldThrow<BadRequestException> { service.create(7L, request) }
    }

    test("create rejects invalid month day") {
      val request =
        weeklyRequest().copy(cycleType = SubscriptionCycleType.MONTHLY, weekdays = null, monthDay = 32)
      shouldThrow<BadRequestException> { service.create(7L, request) }
    }

    test("create rejects endDate before startDate") {
      val request = weeklyRequest().copy(endDate = "2026-03-01")
      shouldThrow<BadRequestException> { service.create(7L, request) }
    }

    test("create rejects malformed startDate") {
      val request = weeklyRequest().copy(startDate = "2026/03/02")
      shouldThrow<BadRequestException> { service.create(7L, request) }
    }

    test("create rejects unknown bound worker") {
      every { adminService.findById(9L) } returns null
      val request = weeklyRequest().copy(workerId = 9L)
      shouldThrow<BadRequestException> { service.create(7L, request) }
    }

    test("create accepts bound worker") {
      every { adminService.findById(9L) } returns
        AdminModel(
          id = 9L,
          username = "worker1",
          passwordHash = "x",
          role = AdminRoleModel.DELIVERY_WORKER,
          phone = "13800138001",
        )
      val plan = service.create(7L, weeklyRequest().copy(workerId = 9L))
      plan.workerId shouldBe 9L
    }

    test("pause only from ACTIVE") {
      val active =
        SubscriptionModel(
          id = 1L,
          userId = 100L,
          addressId = 10L,
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = "1",
          timeSlot = DeliveryTimeSlot.MORNING,
          startDate = LocalDate.now().minusDays(1),
          nextRunDate = LocalDate.now(),
        )
      every { subscriptionRepository.findById(1L) } returns active
      service.pause(1L).status shouldBe SubscriptionStatus.PAUSED

      active.status = SubscriptionStatus.PAUSED
      shouldThrow<BadRequestException> { service.pause(1L) }

      active.status = SubscriptionStatus.TERMINATED
      shouldThrow<BadRequestException> { service.pause(1L) }
    }

    test("resume only from PAUSED and skips missed run dates (不补送)") {
      val paused =
        SubscriptionModel(
          id = 2L,
          userId = 100L,
          addressId = 10L,
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = "1,2,3,4,5,6,7",
          timeSlot = DeliveryTimeSlot.MORNING,
          startDate = LocalDate.now().minusMonths(2),
          nextRunDate = LocalDate.now().minusDays(5),
          status = SubscriptionStatus.PAUSED,
        )
      every { subscriptionRepository.findById(2L) } returns paused

      val resumed = service.resume(2L)

      resumed.status shouldBe SubscriptionStatus.ACTIVE
      (resumed.nextRunDate.isBefore(LocalDate.now())) shouldBe false
    }

    test("resume keeps future run date untouched") {
      val future = LocalDate.now().plusDays(3)
      val paused =
        SubscriptionModel(
          id = 3L,
          userId = 100L,
          addressId = 10L,
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = "1",
          timeSlot = DeliveryTimeSlot.MORNING,
          startDate = LocalDate.now().minusMonths(1),
          nextRunDate = future,
          status = SubscriptionStatus.PAUSED,
        )
      every { subscriptionRepository.findById(3L) } returns paused

      service.resume(3L).nextRunDate shouldBe future
    }

    test("terminate from ACTIVE and reject double terminate") {
      val active =
        SubscriptionModel(
          id = 4L,
          userId = 100L,
          addressId = 10L,
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = "1",
          timeSlot = DeliveryTimeSlot.MORNING,
          startDate = LocalDate.now().minusDays(1),
          nextRunDate = LocalDate.now(),
        )
      every { subscriptionRepository.findById(4L) } returns active

      service.terminate(4L).status shouldBe SubscriptionStatus.TERMINATED
      shouldThrow<BadRequestException> { service.terminate(4L) }
    }

    test("mutating operations throw NotFound for missing plan") {
      every { subscriptionRepository.findById(404L) } returns null
      shouldThrow<NotFoundException> { service.pause(404L) }
      shouldThrow<NotFoundException> { service.resume(404L) }
      shouldThrow<NotFoundException> { service.terminate(404L) }
    }

    test("create stamps updatedAt on status transitions") {
      val active =
        SubscriptionModel(
          id = 5L,
          userId = 100L,
          addressId = 10L,
          cycleType = SubscriptionCycleType.WEEKLY,
          weekdays = "1",
          timeSlot = DeliveryTimeSlot.MORNING,
          startDate = LocalDate.now().minusDays(1),
          nextRunDate = LocalDate.now(),
          updatedAt = LocalDateTime.now().minusHours(1),
        )
      every { subscriptionRepository.findById(5L) } returns active

      service.pause(5L)

      (active.updatedAt > LocalDateTime.now().minusMinutes(1)) shouldBe true
    }
  })
