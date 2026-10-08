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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import jakarta.persistence.EntityManager
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 评价仓储 QueryDSL 实现单元测试:过滤分页/计数/平均分。
 */
class ReviewRepositoryImplTest :
  FunSpec({
    var selectQuery: JPAQuery<ReviewModel> = mockk()
    var countQuery: JPAQuery<Void> = mockk()
    var avgQuery: JPAQuery<Double> = mockk()
    var repo: ReviewRepositoryImpl = mockk()
    val pageable = PageRequest.of(0, 10)

    fun review() = ReviewModel(orderId = 1L, userId = 100L, deliveryWorkerId = 5L, rating = 4)

    beforeTest {
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
    afterTest {
      unmockkConstructor(JPAQueryFactory::class)
    }
    test("findReviewsWithFilters applies all filters") {
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

      page.content shouldBe reviews
      page.totalElements shouldBe 1L
    }
    test("findReviewsWithFilters handles null filters") {
      val reviews = listOf(review())
      every { countQuery.fetchCount() } returns 1L
      every { selectQuery.fetch() } returns reviews

      val page = repo.findReviewsWithFilters(null, null, null, null, null, null, pageable)

      page.content shouldBe reviews
    }
    test("countByDeliveryWorkerIdAndRating counts") {
      every { countQuery.fetchCount() } returns 2L

      repo.countByDeliveryWorkerIdAndRating(5L, 4) shouldBe 2L
    }
    test("findAverageRatingByDeliveryWorkerId returns average or null") {
      every { avgQuery.fetchOne() } returns 4.5
      repo.findAverageRatingByDeliveryWorkerId(5L) shouldBe 4.5

      every { avgQuery.fetchOne() } returns null
      repo.findAverageRatingByDeliveryWorkerId(5L).shouldBeNull()
    }
  })
