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

package dev.yidafu.aqua.review.domain.repository

import com.querydsl.core.types.EntityPath
import com.querydsl.core.types.Expression
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Predicate
import com.querydsl.jpa.impl.JPAQuery
import com.querydsl.jpa.impl.JPAQueryFactory
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 配送员统计仓储 QueryDSL 实现单元测试:排行榜/评分区间/平均分。
 */
class DeliveryWorkerStatisticsRepositoryImplTest {
  private lateinit var selectQuery: JPAQuery<DeliveryWorkerStatisticsModel>
  private lateinit var countQuery: JPAQuery<Void>
  private lateinit var avgQuery: JPAQuery<Double>
  private lateinit var repo: DeliveryWorkerStatisticsRepositoryImpl

  private val pageable = PageRequest.of(0, 10)

  private fun stats(id: Long) =
    DeliveryWorkerStatisticsModel(
      id = id,
      deliveryWorkerId = id,
      averageRating = BigDecimal.valueOf(4.5),
      totalReviews = 10,
    )

  @BeforeEach
  fun setUp() {
    mockkConstructor(JPAQueryFactory::class)
    selectQuery = mockk(relaxed = true)
    countQuery = mockk(relaxed = true)
    avgQuery = mockk(relaxed = true)
    repo = DeliveryWorkerStatisticsRepositoryImpl()
    val em = mockk<EntityManager>(relaxed = true)
    val field = DeliveryWorkerStatisticsRepositoryImpl::class.java.getDeclaredField("entityManager")
    field.isAccessible = true
    field.set(repo, em)

    every { anyConstructed<JPAQueryFactory>().query() } returns countQuery
    every { anyConstructed<JPAQueryFactory>().selectFrom(any<EntityPath<DeliveryWorkerStatisticsModel>>()) } returns selectQuery
    every { countQuery.from(any()) } returns countQuery
    every { countQuery.where(any<Predicate>()) } returns countQuery
    every { countQuery.select(any<Expression<Double>>()) } returns avgQuery
    every { selectQuery.where(any<Predicate>()) } returns selectQuery
    every { selectQuery.orderBy(any<OrderSpecifier<*>>(), any<OrderSpecifier<*>>()) } returns selectQuery
    every { selectQuery.offset(any()) } returns selectQuery
    every { selectQuery.limit(any()) } returns selectQuery
  }

  @AfterEach
  fun tearDown() {
    unmockkConstructor(JPAQueryFactory::class)
  }

  @Test
  @DisplayName("findDeliveryWorkersRanking 按评分排序返回分页结果")
  fun `findDeliveryWorkersRanking sorts by rating`() {
    val rows = listOf(stats(1L), stats(2L))
    every { countQuery.fetchCount() } returns 2L
    every { selectQuery.fetch() } returns rows

    val page = repo.findDeliveryWorkersRanking("rating", 1, pageable)

    assertEquals(rows, page.content)
    assertEquals(2L, page.totalElements)
  }

  @Test
  @DisplayName("findDeliveryWorkersRanking 按评论数排序")
  fun `findDeliveryWorkersRanking sorts by reviews`() {
    val rows = listOf(stats(1L))
    every { countQuery.fetchCount() } returns 1L
    every { selectQuery.fetch() } returns rows

    val page = repo.findDeliveryWorkersRanking("reviews", 1, pageable)

    assertEquals(rows, page.content)
  }

  @Test
  @DisplayName("findDeliveryWorkersByRatingRange 评分区间过滤")
  fun `findDeliveryWorkersByRatingRange filters by rating`() {
    val rows = listOf(stats(1L))
    every { countQuery.fetchCount() } returns 1L
    every { selectQuery.fetch() } returns rows

    val page = repo.findDeliveryWorkersByRatingRange(4.0, 5.0, 1, null, "rating", pageable)
    val page2 = repo.findDeliveryWorkersByRatingRange(4.0, 5.0, 1, 20, "reviews", pageable)

    assertEquals(rows, page.content)
    assertEquals(rows, page2.content)
  }

  @Test
  @DisplayName("countDeliveryWorkersByMinRating 计数")
  fun `countDeliveryWorkersByMinRating counts`() {
    every { countQuery.fetchCount() } returns 3L

    assertEquals(3L, repo.countDeliveryWorkersByMinRating(4.0, 1))
  }

  @Test
  @DisplayName("findOverallAverageRating 返回平均分,空结果返回 null")
  fun `findOverallAverageRating returns average or null`() {
    every { avgQuery.fetchOne() } returns 4.6
    assertEquals(4.6, repo.findOverallAverageRating(1))

    every { avgQuery.fetchOne() } returns null
    assertNull(repo.findOverallAverageRating(1))
  }
}
