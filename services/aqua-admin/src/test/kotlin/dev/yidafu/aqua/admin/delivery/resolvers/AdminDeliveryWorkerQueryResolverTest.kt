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

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerStatisticsQueryApiService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.LocalDateTime

class AdminDeliveryWorkerQueryResolverTest :
  FunSpec({
    var adminService: AdminService = mockk()
    var deliveryWorkerStatisticsQueryApiService: DeliveryWorkerStatisticsQueryApiService = mockk()
    var resolver: AdminDeliveryWorkerQueryResolver = mockk()

    fun createSampleWorker(): AdminModel =
      AdminModel(
        id = 2L,
        username = "delivery_2",
        passwordHash = "hashed_password",
        realName = "送水员甲",
        phone = "13900139000",
        role = AdminRoleModel.DELIVERY_WORKER,
        accountStatus = AdminAccountStatus.ACTIVE,
        onlineStatus = DeliverWorkerModelStatus.ONLINE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      adminService = mockk(relaxed = true)
      deliveryWorkerStatisticsQueryApiService = mockk(relaxed = true)
      resolver = AdminDeliveryWorkerQueryResolver(adminService, deliveryWorkerStatisticsQueryApiService)
    }
    test("deliveryWorkers should return all delivery capable admins") {
      // Given
      every { adminService.findAllDeliveryCapable() } returns listOf(createSampleWorker())

      // When
      val result = resolver.deliveryWorkers()

      // Then
      result.size shouldBe 1
      result[0].name shouldBe "送水员甲"
      verify { adminService.findAllDeliveryCapable() }
    }
    test("onlineDeliveryWorkers should filter by online status") {
      // Given
      every { adminService.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE) } returns listOf(createSampleWorker())

      // When
      val result = resolver.onlineDeliveryWorkers()

      // Then
      result.size shouldBe 1
      verify { adminService.findByOnlineStatus(DeliverWorkerModelStatus.ONLINE) }
    }
    test("deliveryWorker should map delivery capable roles") {
      // Given
      every { adminService.findById(2L) } returns createSampleWorker()

      // When
      val result = resolver.deliveryWorker(2L)

      // Then
      result.shouldNotBeNull()
      result.id shouldBe 2L
    }
    test("deliveryWorker should return null for non delivery roles") {
      // Given
      every { adminService.findById(3L) } returns createSampleWorker().copy(id = 3L, role = AdminRoleModel.SUPER_ADMIN)

      // When
      val result = resolver.deliveryWorker(3L)

      // Then
      result.shouldBeNull()
    }
    test("deliveryWorkerRatingStatistics should return zeroed stats when missing") {
      // Given
      every { deliveryWorkerStatisticsQueryApiService.findByDeliveryWorkerId(2L) } returns null
      every { adminService.findById(2L) } returns createSampleWorker()

      // When
      val result = resolver.deliveryWorkerRatingStatistics(2L)

      // Then
      result.deliveryWorkerId shouldBe 2L
      result.averageRating shouldBe BigDecimal.ZERO
      result.totalReviews shouldBe 0L
      result.workerName.shouldNotBeNull()
    }
    test("deliveryWorkerRatingStatistics should map stats distribution") {
      // Given
      every { deliveryWorkerStatisticsQueryApiService.findByDeliveryWorkerId(2L) } returns
        DeliveryWorkerStatisticsModel(
          id = 1L,
          deliveryWorkerId = 2L,
          averageRating = BigDecimal("4.80"),
          totalReviews = 10,
          oneStarReviews = 1,
          twoStarReviews = 0,
          threeStarReviews = 1,
          fourStarReviews = 2,
          fiveStarReviews = 6,
          lastUpdated = LocalDateTime.now(),
        )
      every { adminService.findById(2L) } returns createSampleWorker()

      // When
      val result = resolver.deliveryWorkerRatingStatistics(2L)

      // Then
      result.averageRating shouldBe BigDecimal("4.80")
      result.totalReviews shouldBe 10L
      result.fiveStarReviews shouldBe 6L
      result.ratingDistribution["5"] shouldBe 6L
    }
  })
