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

package dev.yidafu.aqua.analytics.statistics.service.impl

import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsOrderRepositoryCustom
import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsUserRepositoryCustom
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.DailyStatistic
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

/**
 * 用户统计服务单元测试: 总量/新增/活跃口径、每日新增与登录趋势补零
 */
class UserStatisticsServiceImplTest :
  FunSpec({
    var userQueryApiService: UserQueryApiService = mockk()
    var statisticsUserRepository: StatisticsUserRepositoryCustom = mockk()
    var statisticsOrderRepository: StatisticsOrderRepositoryCustom = mockk()
    var userActionLogRepository: UserActionLogRepository = mockk()
    var service: UserStatisticsServiceImpl = mockk()

    beforeTest {
      userQueryApiService = mockk(relaxed = true)
      statisticsUserRepository = mockk(relaxed = true)
      statisticsOrderRepository = mockk(relaxed = true)
      userActionLogRepository = mockk(relaxed = true)
      service = UserStatisticsServiceImpl(userQueryApiService, statisticsUserRepository, statisticsOrderRepository, userActionLogRepository)
    }
    test("getUserStatistics aggregates totals and actives") {
      every { userQueryApiService.findAll() } returns listOf(mockk<UserModel>(relaxed = true), mockk(relaxed = true))
      every { statisticsUserRepository.countUsersCreatedAfter(any()) } returnsMany listOf(5L, 20L)
      every { statisticsUserRepository.countActiveUsersSince(any()) } returns 15L

      val result = service.getUserStatistics()

      result.totalUsers shouldBe 2L
      result.todayNewUsers shouldBe 5L
      result.monthNewUsers shouldBe 20L
      result.activeUsers shouldBe 15L
    }
    test("getDailyNewUsers fills missing dates with zero") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 3)
      every { statisticsUserRepository.countDailyNewUsers(any(), any()) } returns mapOf(start to 2L)

      val result = service.getDailyNewUsers(start, end)

      result.size shouldBe 3
      result[0].date shouldBe "2025-06-01"
      result[0].orderCount shouldBe 2
      result[1] shouldBe DailyStatistic(date = "2025-06-02", orderCount = 0, orderProductCount = 0, revenueCents = 0L, refundCents = 0L)
      result[2] shouldBe DailyStatistic(date = "2025-06-03", orderCount = 0, orderProductCount = 0, revenueCents = 0L, refundCents = 0L)
    }
    test("getLoginStatistics aggregates today and range logins") {
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 2)
      every { userActionLogRepository.countDistinctUsersByActionTypeAndDateRange(any(), any(), any()) } returnsMany listOf(4L, 9L)
      every { userActionLogRepository.countDailyLoginsByDateRange(any(), any()) } returns mapOf(start to 3L)

      val result = service.getLoginStatistics(start, end)

      result.todayLogins shouldBe 4L
      result.totalLogins shouldBe 9L
      result.dailyLogins.size shouldBe 2
      result.dailyLogins[0].loginCount shouldBe 3
      result.dailyLogins[1].loginCount shouldBe 0
    }
    test("getOrderUserDistribution tiers ratio and repurchase") {
      // G35: 近30天下单用户分布 —— 1次/2~3次/4次以上 分层 + 占比 + 复购率
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 30)
      every { statisticsOrderRepository.countOrdersPerUserInRange(any(), any()) } returns
        mapOf(
          1L to 1L, // 1次
          2L to 1L, // 1次
          3L to 2L, // 2~3次
          4L to 3L, // 2~3次
          5L to 5L, // 4次以上
        )
      every { userQueryApiService.findAll() } returns listOf(mockk<UserModel>(relaxed = true), mockk(relaxed = true))

      val result = service.getOrderUserDistribution(start, end)

      result.orderingUsers shouldBe 5L
      result.orderingUserRatio shouldBe 2.5f // 5 / 2
      result.repurchaseRate shouldBe 0.6f // 3 / 5
      result.tiers.size shouldBe 3
      result.tiers[0].label shouldBe "1次"
      result.tiers[0].userCount shouldBe 2L
      result.tiers[1].label shouldBe "2~3次"
      result.tiers[1].userCount shouldBe 2L
      result.tiers[2].label shouldBe "4次以上"
      result.tiers[2].userCount shouldBe 1L
    }
    test("getOrderUserDistribution guards division by zero") {
      // G35 边界:无下单用户 / 无总用户 —— 占比与复购率为 0,不抛异常
      val start = LocalDate.of(2025, 6, 1)
      val end = LocalDate.of(2025, 6, 30)
      every { statisticsOrderRepository.countOrdersPerUserInRange(any(), any()) } returns emptyMap()
      every { userQueryApiService.findAll() } returns emptyList()

      val result = service.getOrderUserDistribution(start, end)

      result.orderingUsers shouldBe 0L
      result.orderingUserRatio shouldBe 0f
      result.repurchaseRate shouldBe 0f
      result.tiers.all { it.userCount == 0L } shouldBe true
    }
  })
