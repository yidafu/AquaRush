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
import dev.yidafu.aqua.admin.ops.resolvers.WithMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel
import dev.yidafu.aqua.common.domain.model.ReviewModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.review.domain.repository.DeliveryWorkerStatisticsRepository
import dev.yidafu.aqua.review.domain.repository.ReviewRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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
@DisplayName("AdminReviewResolver GraphQL Integration Tests")
class AdminReviewResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var reviewRepository: ReviewRepository

  @Autowired
  private lateinit var adminRepository: AdminRepository

  @Autowired
  private lateinit var deliveryWorkerStatisticsRepository: DeliveryWorkerStatisticsRepository

  private lateinit var mockMvc: MockMvc

  private var orderIdSeq = 0L

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()
  }

  private fun postGraphql(query: String): String {
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

  /** 种子配送员账号(用户名唯一),返回账号 id */
  private fun seedWorker(realName: String): Long {
    val username = "review-worker-${System.currentTimeMillis()}"
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

  /** 种子一条用户评价 */
  private fun seedReview(
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

  /** 种子配送员评分统计(delivery_worker_statistics 表,delivery_worker_id 唯一) */
  private fun seedWorkerStatistics(
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

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_USER_READ"])
  @DisplayName("reviewsByUser 返回该用户评价(评分/评价内容/配送员名联查)")
  fun `reviewsByUser returns user reviews with worker name`() {
    val workerId = seedWorker("王师傅")
    val reviewId = seedReview(userId = 1L, deliveryWorkerId = workerId)
    val json =
      postGraphql(
        "{ reviewsByUser(userId: 1, page: 0, size: 20) { content { reviewId rating comment deliveryWorkerName isAnonymous } totalElements } }",
      )
    assertTrue(
      json.contains("\"reviewId\" : \"$reviewId\"") &&
        json.contains("\"rating\" : 5") &&
        json.contains("\"comment\" : \"送水很快\"") &&
        json.contains("\"deliveryWorkerName\" : \"王师傅\"") &&
        json.contains("\"isAnonymous\" : false") &&
        json.contains("\"totalElements\" : 1") &&
        !json.contains("\"errors\""),
      "reviewsByUser should return seeded review with worker name: $json",
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_USER_READ"])
  @DisplayName("reviewsByUser 用户隔离:其他用户评价不返回")
  fun `reviewsByUser is isolated per user`() {
    val workerId = seedWorker("李师傅")
    seedReview(userId = 1L, deliveryWorkerId = workerId)
    val json = postGraphql("{ reviewsByUser(userId: 2, page: 0, size: 20) { content { reviewId } totalElements } }")
    assertTrue(
      json.contains("\"totalElements\" : 0") && !json.contains("\"errors\""),
      "reviewsByUser for user 2 should be empty: $json",
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_USER_READ"])
  @DisplayName("reviewsByUser 分页生效(size=1,totalElements=2)")
  fun `reviewsByUser paginates`() {
    val workerId = seedWorker("赵师傅")
    seedReview(userId = 3L, deliveryWorkerId = workerId, comment = "第一条")
    seedReview(userId = 3L, deliveryWorkerId = workerId, comment = "第二条")
    val json = postGraphql("{ reviewsByUser(userId: 3, page: 0, size: 1) { content { comment } totalElements size } }")
    assertTrue(
      json.contains("\"totalElements\" : 2") &&
        json.contains("\"size\" : 1") &&
        !json.contains("\"errors\""),
      "reviewsByUser should paginate: $json",
    )
  }

  @Test
  @DisplayName("anonymous 访问 reviewsByUser 被拦截(401)")
  fun `reviewsByUser anonymous is denied`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "{ reviewsByUser(userId: 1, page: 0, size: 20) { content { reviewId } } }"}"""),
      ).andExpect(status().isUnauthorized)
  }

  // ============ reviewsByDeliveryWorker(账号详情页「评价记录」Tab,F-USR-020/021)============

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("reviewsByDeliveryWorker 返回该配送员收到的评价(跨用户聚合,配送员名联查)")
  fun `reviewsByDeliveryWorker returns worker reviews across users`() {
    val workerId = seedWorker("周师傅")
    seedReview(userId = 1L, deliveryWorkerId = workerId, rating = 5, comment = "很快")
    seedReview(userId = 2L, deliveryWorkerId = workerId, rating = 4, comment = "不错")
    val json =
      postGraphql(
        "{ reviewsByDeliveryWorker(deliveryWorkerId: $workerId, page: 0, size: 20) { content { reviewId rating comment deliveryWorkerName } totalElements } }",
      )
    assertTrue(
      json.contains("\"totalElements\" : 2") &&
        json.contains("\"deliveryWorkerName\" : \"周师傅\"") &&
        json.contains("\"rating\" : 5") &&
        json.contains("\"rating\" : 4") &&
        json.contains("\"comment\" : \"很快\"") &&
        json.contains("\"comment\" : \"不错\"") &&
        !json.contains("\"errors\""),
      "reviewsByDeliveryWorker should return both reviews with worker name: $json",
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("reviewsByDeliveryWorker 配送员隔离:其他配送员的评价不返回")
  fun `reviewsByDeliveryWorker is isolated per worker`() {
    val workerA = seedWorker("吴师傅")
    val workerB = seedWorker("郑师傅")
    seedReview(userId = 1L, deliveryWorkerId = workerA)
    val json =
      postGraphql(
        "{ reviewsByDeliveryWorker(deliveryWorkerId: $workerB, page: 0, size: 20) { content { reviewId } totalElements } }",
      )
    assertTrue(
      json.contains("\"totalElements\" : 0") && !json.contains("\"errors\""),
      "reviewsByDeliveryWorker for worker B should be empty: $json",
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("reviewsByDeliveryWorker 分页生效(size=1,totalElements=2)")
  fun `reviewsByDeliveryWorker paginates`() {
    val workerId = seedWorker("孙师傅")
    seedReview(userId = 3L, deliveryWorkerId = workerId, comment = "第一条")
    seedReview(userId = 3L, deliveryWorkerId = workerId, comment = "第二条")
    val json =
      postGraphql(
        "{ reviewsByDeliveryWorker(deliveryWorkerId: $workerId, page: 0, size: 1) { content { comment } totalElements size } }",
      )
    assertTrue(
      json.contains("\"totalElements\" : 2") &&
        json.contains("\"size\" : 1") &&
        !json.contains("\"errors\""),
      "reviewsByDeliveryWorker should paginate: $json",
    )
  }

  @Test
  @DisplayName("anonymous 访问 reviewsByDeliveryWorker 被拦截(401)")
  fun `reviewsByDeliveryWorker anonymous is denied`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "{ reviewsByDeliveryWorker(deliveryWorkerId: 1, page: 0, size: 20) { content { reviewId } } }"}"""),
      ).andExpect(status().isUnauthorized)
  }

  // ============ deliveryWorkerRatingStatistics(账号详情页「评价记录」Tab 评分分布)============

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("deliveryWorkerRatingStatistics 返回评分分布/总评价数/平均评分")
  fun `deliveryWorkerRatingStatistics returns distribution`() {
    val workerId = seedWorker("钱师傅")
    seedWorkerStatistics(deliveryWorkerId = workerId)
    val json =
      postGraphql(
        "{ deliveryWorkerRatingStatistics(deliveryWorkerId: $workerId) { totalReviews averageRating oneStarReviews fiveStarReviews ratingDistribution workerName } }",
      )
    assertTrue(
      json.contains("\"totalReviews\" : \"3\"") &&
        json.contains("\"averageRating\" : \"4.33\"") &&
        json.contains("\"oneStarReviews\" : \"1\"") &&
        json.contains("\"fiveStarReviews\" : \"2\"") &&
        json.contains("\"ratingDistribution\"") &&
        json.contains("\"1\" : 1") &&
        json.contains("\"5\" : 2") &&
        json.contains("\"workerName\" : \"钱师傅\"") &&
        !json.contains("\"errors\""),
      "deliveryWorkerRatingStatistics should return distribution: $json",
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("deliveryWorkerRatingStatistics 无统计记录返回全零分布")
  fun `deliveryWorkerRatingStatistics empty returns zeros`() {
    val workerId = seedWorker("冯师傅")
    val json =
      postGraphql(
        "{ deliveryWorkerRatingStatistics(deliveryWorkerId: $workerId) { totalReviews averageRating oneStarReviews fiveStarReviews } }",
      )
    assertTrue(
      json.contains("\"totalReviews\" : \"0\"") &&
        json.contains("\"averageRating\" : \"0\"") &&
        json.contains("\"oneStarReviews\" : \"0\"") &&
        json.contains("\"fiveStarReviews\" : \"0\"") &&
        !json.contains("\"errors\""),
      "deliveryWorkerRatingStatistics without record should return zeros: $json",
    )
  }

  @Test
  @DisplayName("anonymous 访问 deliveryWorkerRatingStatistics 被拦截(401)")
  fun `deliveryWorkerRatingStatistics anonymous is denied`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "{ deliveryWorkerRatingStatistics(deliveryWorkerId: 1) { totalReviews } }"}"""),
      ).andExpect(status().isUnauthorized)
  }
}
