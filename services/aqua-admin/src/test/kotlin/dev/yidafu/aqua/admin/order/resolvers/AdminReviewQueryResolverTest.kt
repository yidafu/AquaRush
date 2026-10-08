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

package dev.yidafu.aqua.admin.order.resolvers

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.review.ReviewQueryService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDateTime

class AdminReviewQueryResolverTest :
  FunSpec({
    var reviewQueryService: ReviewQueryService = mockk()
    var adminService: AdminService = mockk()
    var resolver: AdminReviewQueryResolver = mockk()

    fun createSampleReview(): ReviewModel =
      ReviewModel(
        id = 1L,
        orderId = 100L,
        userId = 1L,
        deliveryWorkerId = 2L,
        rating = 5,
        comment = "送得快",
        isAnonymous = false,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    fun createSampleWorker(): AdminModel =
      AdminModel(
        id = 2L,
        username = "worker1",
        passwordHash = "hashed_password",
        realName = "送水员甲",
        phone = "13900139000",
        role = AdminRoleModel.DELIVERY_WORKER,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
      )

    beforeTest {
      reviewQueryService = mockk(relaxed = true)
      adminService = mockk(relaxed = true)
      resolver = AdminReviewQueryResolver(reviewQueryService, adminService)
    }
    test("reviewsByUser should return paginated reviews") {
      // Given
      every { reviewQueryService.findByUserIdPaginated(1L, PageRequest.of(0, 10)) } returns
        PageImpl(listOf(createSampleReview()))
      every { adminService.findById(2L) } returns createSampleWorker()

      // When
      val result = resolver.reviewsByUser(1L, 0, 10)

      // Then
      result.content.size shouldBe 1
      result.content[0].deliveryWorkerName shouldBe "送水员甲"
      result.content[0].rating shouldBe 5
      verify { reviewQueryService.findByUserIdPaginated(1L, PageRequest.of(0, 10)) }
    }
    test("reviewsByUser should resolve null worker name when admin missing") {
      // Given
      every { reviewQueryService.findByUserIdPaginated(1L, PageRequest.of(0, 10)) } returns
        PageImpl(listOf(createSampleReview()))
      every { adminService.findById(2L) } returns null

      // When
      val result = resolver.reviewsByUser(1L, 0, 10)

      // Then
      result.content[0].deliveryWorkerName.shouldBeNull()
    }
    test("reviewsByDeliveryWorker should return paginated reviews") {
      // Given
      every { reviewQueryService.findByDeliveryWorkerIdPaginated(2L, PageRequest.of(1, 5)) } returns
        PageImpl(listOf(createSampleReview()), PageRequest.of(1, 5), 1)
      every { adminService.findById(2L) } returns createSampleWorker()

      // When
      val result = resolver.reviewsByDeliveryWorker(2L, 1, 5)

      // Then
      result.content.size shouldBe 1
      result.number shouldBe 1
      verify { reviewQueryService.findByDeliveryWorkerIdPaginated(2L, PageRequest.of(1, 5)) }
    }
  })
