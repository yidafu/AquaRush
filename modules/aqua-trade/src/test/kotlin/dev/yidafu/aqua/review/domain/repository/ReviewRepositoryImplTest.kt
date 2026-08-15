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
import dev.yidafu.aqua.common.domain.model.ReviewModel
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
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 评价仓储 QueryDSL 实现单元测试:过滤分页/计数/平均分。
 */
class ReviewRepositoryImplTest {
  private lateinit var selectQuery: JPAQuery<ReviewModel>
  private lateinit var countQuery: JPAQuery<Void>
  private lateinit var avgQuery: JPAQuery<Double>
  private lateinit var repo: ReviewRepositoryImpl

  private val pageable = PageRequest.of(0, 10)

  private fun review() = ReviewModel(orderId = 1L, userId = 100L, deliveryWorkerId = 5L, rating = 4)

  @BeforeEach
  fun setUp() {
    mockkConstructor(JPAQueryFactory::class)
    selectQuery = mockk(relaxed = true)
    countQuery = mockk(relaxed = true)
    avgQuery = mockk(relaxed = true)
    repo = ReviewRepositoryImpl()
    val em = mockk<EntityManager>(relaxed = true)
    val field = ReviewRepositoryImpl::class.java.getDeclaredField("entityManager")
    field.isAccessible = true
    field.set(repo, em)

    every { anyConstructed<JPAQueryFactory>().query() } returns countQuery
    every { anyConstructed<JPAQueryFactory>().selectFrom(any<EntityPath<ReviewModel>>()) } returns selectQuery
    every { countQuery.from(any()) } returns countQuery
    every { countQuery.where(any<Predicate>()) } returns countQuery
    every { countQuery.select(any<Expression<Double>>()) } returns avgQuery
    every { selectQuery.where(any<Predicate>()) } returns selectQuery
    every { selectQuery.orderBy(any<OrderSpecifier<*>>()) } returns selectQuery
    every { selectQuery.offset(any()) } returns selectQuery
    every { selectQuery.limit(any()) } returns selectQuery
  }

  @AfterEach
  fun tearDown() {
    unmockkConstructor(JPAQueryFactory::class)
  }

  @Test
  @DisplayName("findReviewsWithFilters 全过滤条件分页")
  fun `findReviewsWithFilters applies all filters`() {
    val reviews = listOf(review())
    every { countQuery.fetchCount() } returns 1L
    every { selectQuery.fetch() } returns reviews

    val page =
      repo.findReviewsWithFilters(
        deliveryWorkerId = 5L,
        minRating = 3,
        maxRating = 5,
        dateFrom = LocalDateTime.of(2025, 1, 1, 0, 0),
        dateTo = LocalDateTime.of(2025, 1, 2, 0, 0),
        userId = 100L,
        pageable = pageable,
      )

    assertEquals(reviews, page.content)
    assertEquals(1L, page.totalElements)
  }

  @Test
  @DisplayName("findReviewsWithFilters 无过滤条件")
  fun `findReviewsWithFilters handles null filters`() {
    val reviews = listOf(review())
    every { countQuery.fetchCount() } returns 1L
    every { selectQuery.fetch() } returns reviews

    val page = repo.findReviewsWithFilters(null, null, null, null, null, null, pageable)

    assertEquals(reviews, page.content)
  }

  @Test
  @DisplayName("countByDeliveryWorkerIdAndRating 计数")
  fun `countByDeliveryWorkerIdAndRating counts`() {
    every { countQuery.fetchCount() } returns 2L

    assertEquals(2L, repo.countByDeliveryWorkerIdAndRating(5L, 4))
  }

  @Test
  @DisplayName("findAverageRatingByDeliveryWorkerId 返回平均分,空结果 null")
  fun `findAverageRatingByDeliveryWorkerId returns average or null`() {
    every { avgQuery.fetchOne() } returns 4.5
    assertEquals(4.5, repo.findAverageRatingByDeliveryWorkerId(5L))

    every { avgQuery.fetchOne() } returns null
    assertNull(repo.findAverageRatingByDeliveryWorkerId(5L))
  }
}
