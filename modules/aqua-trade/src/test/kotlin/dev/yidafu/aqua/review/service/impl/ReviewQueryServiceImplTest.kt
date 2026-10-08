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

package dev.yidafu.aqua.review.service.impl

import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.review.domain.repository.ReviewRepository
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

/**
 * 订单评价查询服务单元测试(F-ORD-007;用户详情页「评价记录」Tab,F-USR-022):
 * findByUserIdPaginated 按用户分页查询,创建时间倒序。
 */
class ReviewQueryServiceImplTest :
  FunSpec({
    var reviewRepository: ReviewRepository = mockk()
    var service: ReviewQueryServiceImpl = mockk()

    beforeTest {
      reviewRepository = mockk(relaxed = true)
      service = ReviewQueryServiceImpl(reviewRepository)
    }
    test("findByUserIdPaginated queries by userId with createdAt desc paging") {
      val review =
        ReviewModel(
          id = 1L,
          orderId = 10L,
          userId = 100L,
          deliveryWorkerId = 5L,
          rating = 5,
          comment = "送得快",
        )
      every { reviewRepository.findByUserIdOrderByCreatedAtDesc(any(), any()) } returns
        PageImpl(listOf(review))

      val result = service.findByUserIdPaginated(userId = 100L, pageable = PageRequest.of(1, 10))

      result.content.size shouldBe 1
      val pageableSlot = slot<PageRequest>()
      verify {
        reviewRepository.findByUserIdOrderByCreatedAtDesc(userId = 100L, pageable = capture(pageableSlot))
      }
      pageableSlot.captured.pageNumber shouldBe 1
      pageableSlot.captured.pageSize shouldBe 10
      // 倒序排序由 Spring Data 方法名约定 findByUserIdOrderByCreatedAtDesc 派生,分页参数不带 Sort
    }
    test("findByDeliveryWorkerIdPaginated queries by deliveryWorkerId") {
      val review =
        ReviewModel(
          id = 2L,
          orderId = 11L,
          userId = 100L,
          deliveryWorkerId = 5L,
          rating = 4,
          comment = "不错",
        )
      every { reviewRepository.findReviewsWithFilters(deliveryWorkerId = 5L, pageable = any()) } returns
        PageImpl(listOf(review))

      val result = service.findByDeliveryWorkerIdPaginated(deliveryWorkerId = 5L, pageable = PageRequest.of(0, 10))

      result.content.size shouldBe 1
      result.content.first().rating shouldBe 4
      val pageableSlot = slot<PageRequest>()
      verify {
        reviewRepository.findReviewsWithFilters(deliveryWorkerId = 5L, pageable = capture(pageableSlot))
      }
      pageableSlot.captured.pageNumber shouldBe 0
      pageableSlot.captured.pageSize shouldBe 10
    }
  })
