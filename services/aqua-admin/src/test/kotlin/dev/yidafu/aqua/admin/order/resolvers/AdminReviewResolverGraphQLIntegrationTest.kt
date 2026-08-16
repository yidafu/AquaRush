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

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.review.domain.repository.DeliveryWorkerStatisticsRepository
import dev.yidafu.aqua.review.domain.repository.ReviewRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal

/**
 * 用户评价记录 Resolver GraphQL 集成测试(用户详情页「评价记录」Tab,PRD 07-user.md §F-USR-022)
 * 权限:PERMISSION_USER_READ;配送员名经 AdminService.findById 联查。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/order/"])
@ApplyExtension(SpringExtension::class)
class AdminReviewResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  reviewRepository: ReviewRepository,
  adminRepository: AdminRepository,
  deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()
    var orderIdSeq = 0L

    fun postGraphql(query: String): String {
      val escaped = query.replace("\"", "\\\"")
      val result =
        mockMvc
          .perform(
            post("/graphql")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""{"query": "$escaped"}"""),
          ).andExpect(status().isOk)
          .andReturn()
      return result.response.contentAsString
    }

    fun seedWorker(realName: String): Long {
      val username = "review-worker-${System.nanoTime()}"
      return adminRepository
        .save(
          AdminModel(
            username = username,
            passwordHash = "x",
            realName = realName,
            role = AdminRoleModel.DELIVERY_WORKER,
          ),
        ).id!!
    }

    fun seedReview(
      userId: Long,
      deliveryWorkerId: Long,
      rating: Int = 5,
      comment: String = "送水很快",
    ): Long =
      reviewRepository
        .save(
          ReviewModel(
            // nanoTime 保证跨测试方法唯一(一单一评,reviews.order_id 唯一约束)
            orderId = System.nanoTime() + orderIdSeq++,
            userId = userId,
            deliveryWorkerId = deliveryWorkerId,
            rating = rating,
            comment = comment,
            isAnonymous = false,
          ),
        ).id!!

    fun seedWorkerStatistics(
      deliveryWorkerId: Long,
      totalReviews: Int = 3,
      averageRating: BigDecimal = BigDecimal("4.33"),
      oneStarReviews: Int = 1,
      fiveStarReviews: Int = 2,
    ) {
      deliveryWorkerStatisticsRepository.save(
        DeliveryWorkerStatisticsModel(
          deliveryWorkerId = deliveryWorkerId,
          averageRating = averageRating,
          totalReviews = totalReviews,
          oneStarReviews = oneStarReviews,
          twoStarReviews = 0,
          threeStarReviews = 0,
          fourStarReviews = 0,
          fiveStarReviews = fiveStarReviews,
        ),
      )
    }

    // 种子配送员账号(用户名唯一,nanoTime 保证跨调用/跨测试方法不冲突),返回账号 id

    // 种子一条用户评价

    // 种子配送员评分统计(delivery_worker_statistics 表,delivery_worker_id 唯一)

    // ============ reviewsByDeliveryWorker(账号详情页「评价记录」Tab,F-USR-020/021)============

    // ============ deliveryWorkerRatingStatistics(账号详情页「评价记录」Tab 评分分布)============

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
    }
    test("reviewsByUser returns user reviews with worker name") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      val workerId = seedWorker("王师傅")
      val reviewId = seedReview(userId = 1L, deliveryWorkerId = workerId)
      val json =
        postGraphql(
          "{ reviewsByUser(userId: 1, page: 0, size: 20) { content { reviewId rating comment deliveryWorkerName isAnonymous } totalElements } }",
        )
      withClue("reviewsByUser should return seeded review with worker name: $json") {
        json.contains("\"reviewId\" : \"$reviewId\"") &&
          json.contains("\"rating\" : 5") &&
          json.contains("\"comment\" : \"送水很快\"") &&
          json.contains("\"deliveryWorkerName\" : \"王师傅\"") &&
          json.contains("\"isAnonymous\" : false") &&
          json.contains("\"totalElements\" : 1") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("reviewsByUser is isolated per user") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      val workerId = seedWorker("李师傅")
      seedReview(userId = 1L, deliveryWorkerId = workerId)
      val json = postGraphql("{ reviewsByUser(userId: 2, page: 0, size: 20) { content { reviewId } totalElements } }")
      withClue("reviewsByUser for user 2 should be empty: $json") {
        json.contains("\"totalElements\" : 0") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("reviewsByUser paginates") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_USER_READ"))
      val workerId = seedWorker("赵师傅")
      seedReview(userId = 3L, deliveryWorkerId = workerId, comment = "第一条")
      seedReview(userId = 3L, deliveryWorkerId = workerId, comment = "第二条")
      val json = postGraphql("{ reviewsByUser(userId: 3, page: 0, size: 1) { content { comment } totalElements size } }")
      withClue("reviewsByUser should paginate: $json") {
        json.contains("\"totalElements\" : 2") &&
          json.contains("\"size\" : 1") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("reviewsByUser anonymous is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ reviewsByUser(userId: 1, page: 0, size: 20) { content { reviewId } } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
    test("reviewsByDeliveryWorker returns worker reviews across users") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_DELIVERY_WORKER_READ"))
      val workerId = seedWorker("周师傅")
      seedReview(userId = 1L, deliveryWorkerId = workerId, rating = 5, comment = "很快")
      seedReview(userId = 2L, deliveryWorkerId = workerId, rating = 4, comment = "不错")
      val json =
        postGraphql(
          "{ reviewsByDeliveryWorker(deliveryWorkerId: $workerId, page: 0, size: 20) { content { reviewId rating comment deliveryWorkerName } totalElements } }",
        )
      withClue("reviewsByDeliveryWorker should return both reviews with worker name: $json") {
        json.contains("\"totalElements\" : 2") &&
          json.contains("\"deliveryWorkerName\" : \"周师傅\"") &&
          json.contains("\"rating\" : 5") &&
          json.contains("\"rating\" : 4") &&
          json.contains("\"comment\" : \"很快\"") &&
          json.contains("\"comment\" : \"不错\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("reviewsByDeliveryWorker is isolated per worker") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_DELIVERY_WORKER_READ"))
      val workerA = seedWorker("吴师傅")
      val workerB = seedWorker("郑师傅")
      seedReview(userId = 1L, deliveryWorkerId = workerA)
      val json =
        postGraphql(
          "{ reviewsByDeliveryWorker(deliveryWorkerId: $workerB, page: 0, size: 20) { content { reviewId } totalElements } }",
        )
      withClue("reviewsByDeliveryWorker for worker B should be empty: $json") {
        json.contains("\"totalElements\" : 0") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("reviewsByDeliveryWorker paginates") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_DELIVERY_WORKER_READ"))
      val workerId = seedWorker("孙师傅")
      seedReview(userId = 3L, deliveryWorkerId = workerId, comment = "第一条")
      seedReview(userId = 3L, deliveryWorkerId = workerId, comment = "第二条")
      val json =
        postGraphql(
          "{ reviewsByDeliveryWorker(deliveryWorkerId: $workerId, page: 0, size: 1) { content { comment } totalElements size } }",
        )
      withClue("reviewsByDeliveryWorker should paginate: $json") {
        json.contains("\"totalElements\" : 2") &&
          json.contains("\"size\" : 1") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("reviewsByDeliveryWorker anonymous is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ reviewsByDeliveryWorker(deliveryWorkerId: 1, page: 0, size: 20) { content { reviewId } } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
    test("deliveryWorkerRatingStatistics returns distribution") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_DELIVERY_WORKER_READ"))
      val workerId = seedWorker("钱师傅")
      seedWorkerStatistics(deliveryWorkerId = workerId)
      val json =
        postGraphql(
          "{ deliveryWorkerRatingStatistics(deliveryWorkerId: $workerId) { totalReviews averageRating oneStarReviews fiveStarReviews ratingDistribution workerName } }",
        )
      withClue("deliveryWorkerRatingStatistics should return distribution: $json") {
        json.contains("\"totalReviews\" : \"3\"") &&
          json.contains("\"averageRating\" : \"4.33\"") &&
          json.contains("\"oneStarReviews\" : \"1\"") &&
          json.contains("\"fiveStarReviews\" : \"2\"") &&
          json.contains("\"ratingDistribution\"") &&
          json.contains("\"1\" : 1") &&
          json.contains("\"5\" : 2") &&
          json.contains("\"workerName\" : \"钱师傅\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("deliveryWorkerRatingStatistics empty returns zeros") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_DELIVERY_WORKER_READ"))
      val workerId = seedWorker("冯师傅")
      val json =
        postGraphql(
          "{ deliveryWorkerRatingStatistics(deliveryWorkerId: $workerId) { totalReviews averageRating oneStarReviews fiveStarReviews } }",
        )
      withClue("deliveryWorkerRatingStatistics without record should return zeros: $json") {
        json.contains("\"totalReviews\" : \"0\"") &&
          json.contains("\"averageRating\" : \"0\"") &&
          json.contains("\"oneStarReviews\" : \"0\"") &&
          json.contains("\"fiveStarReviews\" : \"0\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }
    test("deliveryWorkerRatingStatistics anonymous is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ deliveryWorkerRatingStatistics(deliveryWorkerId: 1) { totalReviews } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
