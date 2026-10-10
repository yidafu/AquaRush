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

import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.table.ReviewsTable
import dev.yidafu.aqua.trade.common.exposed.TradePgTestSupport
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

/**
 * 订单评价仓储 Exposed 实现测试(Testcontainers 真实 PG,替代原 QueryDSL mockk 测试):
 * save 双分支/软删可见性/一单一查/用户分页 createdAt 降序破平/动态过滤分页。
 */
class ReviewRepositoryImplTest :
  FunSpec({
    val repo = ReviewRepositoryImpl(TradePgTestSupport.database)

    beforeTest { TradePgTestSupport.reset() }

    test("save 新增回填雪花 id,findByOrderId/existsByOrderId 可查") {
      val saved =
        repo.save(
          ReviewModel(
            orderId = 2001L,
            userId = 100L,
            deliveryWorkerId = 5L,
            rating = 4,
            comment = "送水很快",
          ),
        )
      saved.id.shouldNotBeNull()
      repo.findByOrderId(2001L).shouldNotBeNull().rating shouldBe 4
      repo.existsByOrderId(2001L) shouldBe true
      repo.existsByOrderId(9999L) shouldBe false
      repo.findByOrderId(9999L).shouldBeNull()
    }

    test("save 更新已有实体走 update 分支,不重复插入") {
      val id =
        seedReview(
          orderId = 2001L,
          userId = 100L,
          workerId = 5L,
          rating = 3,
        )
      val loaded =
        ReviewModel(
          id = id,
          orderId = 2001L,
          userId = 100L,
          deliveryWorkerId = 5L,
          rating = 5,
          comment = "改好评",
        )
      repo.save(loaded)

      val reloaded = repo.findByOrderId(2001L).shouldNotBeNull()
      reloaded.rating shouldBe 5
      reloaded.comment shouldBe "改好评"
      repo.findAllCount() shouldBe 1
    }

    test("软删评价不可见:findByOrderId/existsByOrderId/用户分页均过滤") {
      seedReview(orderId = 2001L, userId = 100L, workerId = 5L, rating = 4, deleted = true)
      val keep = seedReview(orderId = 2002L, userId = 100L, workerId = 5L, rating = 5)

      repo.findByOrderId(2001L).shouldBeNull()
      repo.existsByOrderId(2001L) shouldBe false
      val page = repo.findByUserIdOrderByCreatedAtDesc(100L, PageRequest.of(0, 10))
      page.totalElements shouldBe 1L
      page.content.map { it.id } shouldContainExactly listOf(keep)
    }

    test("用户评价分页:createdAt 降序 + id 降序破平,跨页不重不漏") {
      val base = LocalDateTime.of(2026, 3, 1, 10, 0)
      // 两条相同 createdAt(id 不同)验证破平
      val a = seedReview(orderId = 2001L, userId = 100L, workerId = 5L, createdAt = base)
      val b = seedReview(orderId = 2002L, userId = 100L, workerId = 5L, createdAt = base)
      val c = seedReview(orderId = 2003L, userId = 100L, workerId = 5L, createdAt = base.minusDays(1))
      seedReview(orderId = 2004L, userId = 999L, workerId = 5L, createdAt = base)

      val page0 = repo.findByUserIdOrderByCreatedAtDesc(100L, PageRequest.of(0, 2))
      page0.totalElements shouldBe 3L
      page0.content.map { it.id } shouldContainExactly listOf(b, a)
      val page1 = repo.findByUserIdOrderByCreatedAtDesc(100L, PageRequest.of(1, 2))
      page1.content.map { it.id } shouldContainExactly listOf(c)
    }

    test("findReviewsWithFilters:全过滤条件命中,软删不进结果") {
      seedReview(orderId = 2001L, userId = 100L, workerId = 5L, rating = 5, deleted = true)
      val hit =
        seedReview(
          orderId = 2002L,
          userId = 100L,
          workerId = 5L,
          rating = 4,
          createdAt = LocalDateTime.of(2026, 3, 2, 10, 0),
        )
      seedReview(orderId = 2003L, userId = 101L, workerId = 6L, rating = 2)

      val page =
        repo.findReviewsWithFilters(
          deliveryWorkerId = 5L,
          minRating = 3,
          maxRating = 5,
          dateFrom = LocalDateTime.of(2026, 3, 1, 0, 0),
          dateTo = LocalDateTime.of(2026, 3, 3, 0, 0),
          userId = 100L,
          pageable = PageRequest.of(0, 10),
        )
      page.totalElements shouldBe 1L
      page.content.map { it.id } shouldContainExactly listOf(hit)
    }

    test("findReviewsWithFilters:null 过滤返回全部活跃行;count 与 content 同口径") {
      val a = seedReview(orderId = 2001L, userId = 100L, workerId = 5L, rating = 1)
      seedReview(orderId = 2002L, userId = 100L, workerId = 5L, rating = 3, deleted = true)
      val c = seedReview(orderId = 2003L, userId = 101L, workerId = 6L, rating = 5)

      val page = repo.findReviewsWithFilters(null, null, null, null, null, null, PageRequest.of(0, 10))
      page.totalElements shouldBe 2L
      // a/c createdAt 相同 → id 降序破平
      page.content.map { it.id } shouldContainExactly listOf(c, a)

      // 仅 rating 下界
      repo.findReviewsWithFilters(minRating = 4, pageable = PageRequest.of(0, 10)).totalElements shouldBe 1L
      // 仅 userId
      repo.findReviewsWithFilters(userId = 100L, pageable = PageRequest.of(0, 10)).totalElements shouldBe 1L
      // 无命中
      repo.findReviewsWithFilters(deliveryWorkerId = 777L, pageable = PageRequest.of(0, 10)).totalElements shouldBe 0L
    }

    test("findReviewsWithFilters 分页:createdAt 降序 + id 降序破平") {
      val base = LocalDateTime.of(2026, 3, 1, 10, 0)
      val a = seedReview(orderId = 2001L, userId = 100L, workerId = 5L, createdAt = base)
      val b = seedReview(orderId = 2002L, userId = 100L, workerId = 5L, createdAt = base)
      val c = seedReview(orderId = 2003L, userId = 100L, workerId = 5L, createdAt = base.minusHours(1))

      val page0 = repo.findReviewsWithFilters(null, null, null, null, null, null, PageRequest.of(0, 2))
      page0.content.map { it.id } shouldContainExactly listOf(b, a)
      val page1 = repo.findReviewsWithFilters(null, null, null, null, null, null, PageRequest.of(1, 2))
      page1.content.map { it.id } shouldContainExactly listOf(c)
    }
  })

private fun ReviewRepositoryImpl.findAllCount(): Long =
  transaction(TradePgTestSupport.database) {
    ReviewsTable.selectAllIncludingDeleted().count()
  }

/** 直接插行的种子构造(含软删开关)。 */
private fun seedReview(
  orderId: Long,
  userId: Long,
  workerId: Long,
  rating: Int = 5,
  comment: String? = null,
  createdAt: LocalDateTime = LocalDateTime.of(2026, 3, 1, 10, 0),
  deleted: Boolean = false,
): Long {
  val id = TradePgTestSupport.id()
  transaction(TradePgTestSupport.database) {
    ReviewsTable.insert {
      it[ReviewsTable.id] = id
      it[ReviewsTable.orderId] = orderId
      it[ReviewsTable.userId] = userId
      it[ReviewsTable.deliveryWorkerId] = workerId
      it[ReviewsTable.rating] = rating
      it[ReviewsTable.comment] = comment
      it[ReviewsTable.isAnonymous] = false
      it[ReviewsTable.createdAt] = createdAt
      it[ReviewsTable.updatedAt] = createdAt
      if (deleted) {
        it[ReviewsTable.isDeleted] = true
        it[ReviewsTable.deletedAt] = createdAt.plusHours(1)
      }
    }
  }
  return id
}
