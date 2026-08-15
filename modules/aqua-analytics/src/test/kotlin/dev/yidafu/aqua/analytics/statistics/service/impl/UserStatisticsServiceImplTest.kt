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

import dev.yidafu.aqua.analytics.statistics.domain.repository.StatisticsUserRepositoryCustom
import dev.yidafu.aqua.api.service.user.UserQueryApiService
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.DailyStatistic
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * 用户统计服务单元测试: 总量/新增/活跃口径、每日新增与登录趋势补零
 */
class UserStatisticsServiceImplTest {
  private lateinit var userQueryApiService: UserQueryApiService
  private lateinit var statisticsUserRepository: StatisticsUserRepositoryCustom
  private lateinit var userActionLogRepository: UserActionLogRepository
  private lateinit var service: UserStatisticsServiceImpl

  @BeforeEach
  fun setUp() {
    userQueryApiService = mockk(relaxed = true)
    statisticsUserRepository = mockk(relaxed = true)
    userActionLogRepository = mockk(relaxed = true)
    service = UserStatisticsServiceImpl(userQueryApiService, statisticsUserRepository, userActionLogRepository)
  }

  @Test
  fun `getUserStatistics aggregates totals and actives`() {
    every { userQueryApiService.findAll() } returns listOf(mockk<UserModel>(relaxed = true), mockk(relaxed = true))
    every { statisticsUserRepository.countUsersCreatedAfter(any()) } returnsMany listOf(5L, 20L)
    every { statisticsUserRepository.countActiveUsersSince(any()) } returns 15L

    val result = service.getUserStatistics()

    assertEquals(2L, result.totalUsers)
    assertEquals(5L, result.todayNewUsers)
    assertEquals(20L, result.monthNewUsers)
    assertEquals(15L, result.activeUsers)
  }

  @Test
  fun `getDailyNewUsers fills missing dates with zero`() {
    val start = LocalDate.of(2025, 6, 1)
    val end = LocalDate.of(2025, 6, 3)
    every { statisticsUserRepository.countDailyNewUsers(any(), any()) } returns mapOf(start to 2L)

    val result = service.getDailyNewUsers(start, end)

    assertEquals(3, result.size)
    assertEquals("2025-06-01", result[0].date)
    assertEquals(2, result[0].orderCount)
    assertEquals(DailyStatistic(date = "2025-06-02", orderCount = 0, orderProductCount = 0, revenueCents = 0L), result[1])
    assertEquals(DailyStatistic(date = "2025-06-03", orderCount = 0, orderProductCount = 0, revenueCents = 0L), result[2])
  }

  @Test
  fun `getLoginStatistics aggregates today and range logins`() {
    val start = LocalDate.of(2025, 6, 1)
    val end = LocalDate.of(2025, 6, 2)
    every { userActionLogRepository.countDistinctUsersByActionTypeAndDateRange(any(), any(), any()) } returnsMany listOf(4L, 9L)
    every { userActionLogRepository.countDailyLoginsByDateRange(any(), any()) } returns mapOf(start to 3L)

    val result = service.getLoginStatistics(start, end)

    assertEquals(4L, result.todayLogins)
    assertEquals(9L, result.totalLogins)
    assertEquals(2, result.dailyLogins.size)
    assertEquals(3, result.dailyLogins[0].loginCount)
    assertEquals(0, result.dailyLogins[1].loginCount)
  }
}
