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

package dev.yidafu.aqua.admin.statistics.resolvers

import dev.yidafu.aqua.analytics.statistics.dto.DailyLoginStatisticDTO
import dev.yidafu.aqua.analytics.statistics.dto.DeliveryWorkerRankingItemDTO
import dev.yidafu.aqua.analytics.statistics.dto.DeliveryWorkerStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.LoginStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.dto.UserStatisticsDTO
import dev.yidafu.aqua.analytics.statistics.service.DeliveryWorkerStatisticsService
import dev.yidafu.aqua.analytics.statistics.service.UserStatisticsService
import dev.yidafu.aqua.common.graphql.generated.DailyStatistic
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AdminStatisticsQueryResolverTest :
  FunSpec({
    var userStatisticsService: UserStatisticsService = mockk()
    var deliveryWorkerStatisticsService: DeliveryWorkerStatisticsService = mockk()
    var resolver: AdminStatisticsQueryResolver = mockk()

    beforeTest {
      userStatisticsService = mockk(relaxed = true)
      deliveryWorkerStatisticsService = mockk(relaxed = true)
      resolver = AdminStatisticsQueryResolver(userStatisticsService, deliveryWorkerStatisticsService)
    }
    test("userStatistics should aggregate user and login statistics") {
      // Given
      every { userStatisticsService.getUserStatistics() } returns
        UserStatisticsDTO(totalUsers = 100L, todayNewUsers = 5L, monthNewUsers = 30L, activeUsers = 50L)
      every { userStatisticsService.getDailyNewUsers(any(), any()) } returns
        listOf(DailyStatistic(date = "2025-01-01", orderCount = 1, orderProductCount = 1, revenueCents = 100L, refundCents = 0L))
      every { userStatisticsService.getLoginStatistics(any(), any()) } returns
        LoginStatisticsDTO(todayLogins = 10L, totalLogins = 1000L, dailyLogins = listOf(DailyLoginStatisticDTO("2025-01-01", 3)))

      // When
      val result = resolver.userStatistics()

      // Then
      result.totalUsers shouldBe 100
      result.todayNewUsers shouldBe 5
      result.monthNewUsers shouldBe 30
      result.activeUsers shouldBe 50
      result.dailyNewUsers.size shouldBe 1
      result.loginStatistics!!.todayLogins shouldBe 10
      result.loginStatistics!!.totalLogins shouldBe 1000
      result.loginStatistics!!.dailyLogins.size shouldBe 1
      verify { userStatisticsService.getUserStatistics() }
    }
    test("deliveryWorkerStatistics should map overview") {
      // Given
      every { deliveryWorkerStatisticsService.getDeliveryWorkerStatistics() } returns
        DeliveryWorkerStatisticsDTO(totalWorkers = 8L, todayActiveWorkers = 6L, deliveringOrders = 3L, todayCompletedOrders = 12L)

      // When
      val result = resolver.deliveryWorkerStatistics()

      // Then
      result.totalWorkers shouldBe 8
      result.todayActiveWorkers shouldBe 6
      result.deliveringOrders shouldBe 3
      result.todayCompletedOrders shouldBe 12
    }
    test("deliveryWorkerRanking should map ranking items") {
      // Given
      every { deliveryWorkerStatisticsService.getDeliveryWorkerRanking(5) } returns
        listOf(
          DeliveryWorkerRankingItemDTO(
            workerId = 1L,
            name = "送水员甲",
            todayCompletedOrders = 4,
            rating = 4.9f,
            totalEarnings = 8800L,
          ),
        )

      // When
      val result = resolver.deliveryWorkerRanking(5)

      // Then
      result.size shouldBe 1
      result[0].workerId shouldBe "1"
      result[0].name shouldBe "送水员甲"
      result[0].todayCompletedOrders shouldBe 4
      result[0].totalEarningsCents shouldBe 8800L
      verify { deliveryWorkerStatisticsService.getDeliveryWorkerRanking(5) }
    }
  })
