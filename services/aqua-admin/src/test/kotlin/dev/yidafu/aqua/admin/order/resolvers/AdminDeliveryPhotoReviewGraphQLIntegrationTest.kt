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
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.UserRepository
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
import java.time.LocalDateTime

/**
 * 配送照片核验 mutation GraphQL 集成测试(G23/F-ORD-021,PERMISSION_ORDER_REVIEW)。
 *
 * 回归目标:此前 schema 声明 `reviewDeliveryPhoto(...): Order!` 但后台缺少对应
 * @MutationMapping resolver,数据取回器返回 null → 非空字段 null 冒泡,
 * GraphQL 报 `non-nullable type is 'Order' within parent type 'Mutation'`,
 * 前端「照片核验失败」。本测试验证通过/驳回核验流端到端可用。
 *
 * 自包含 schema:classpath:graphql/photo-review/(仿 order/ 测试目录模式)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/photo-review/"])
@ApplyExtension(SpringExtension::class)
class AdminDeliveryPhotoReviewGraphQLIntegrationTest(
  context: WebApplicationContext,
  orderRepository: OrderRepository,
  userRepository: UserRepository,
  addressRepository: AddressRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun postGraphql(query: String): String {
      val escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"")
      val result =
        mockMvc
          .perform(
            post("/graphql")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""{"query": "$escapedQuery"}"""),
          ).andReturn()
      if (result.response.status != 200) {
        println("=== GRAPHQL STATUS ${result.response.status} BODY: ${result.response.contentAsString}")
      }
      result.response.status shouldBe 200
      return result.response.contentAsString
    }

    fun seedUser(): Long =
      userRepository
        .save(
          UserModel(
            wechatOpenId = "photo-review-openid-${System.nanoTime()}",
            nickname = "照片核验测试用户",
          ),
        ).id!!

    fun seedAddress(userId: Long): Long =
      addressRepository
        .save(
          AddressModel(
            userId = userId,
            receiverName = "测试收件人",
            phone = "13800138000",
            province = "广东省",
            city = "深圳市",
            district = "南山区",
            detailAddress = "测试路 1 号",
          ),
        ).id!!

    fun seedOrder(
      userId: Long,
      addressId: Long,
      photos: String? = """["https://example.com/photo-1.jpg"]""",
    ): Long =
      orderRepository
        .save(
          OrderModel(
            // nanoTime 保证跨测试方法唯一(orders.order_no 唯一)
            orderNo = "PHOTO-REVIEW-${System.nanoTime()}",
            userId = userId,
            amountCents = 1500,
            addressId = addressId,
            status = OrderModelStatus.COMPLETED,
            deliveryPhotos = photos,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
          ),
        ).id!!

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
    }

    test("reviewDeliveryPhoto approve returns APPROVED order without null-bubbling error") {
      setMockUserPrincipal(id = 7L, username = "admin1", authorities = listOf("PERMISSION_ORDER_REVIEW"))
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val orderId = seedOrder(userId, addressId)

      val json =
        postGraphql(
          "mutation { reviewDeliveryPhoto(orderId: $orderId, action: APPROVE) { id deliveryPhotoReviewStatus deliveryPhotoReviewedBy deliveryPhotoReviewRemark } }",
        )
      withClue("approve should return APPROVED order without errors: $json") {
        json.contains("\"deliveryPhotoReviewStatus\" : \"APPROVED\"") &&
          json.contains("\"deliveryPhotoReviewedBy\" : 7") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("reviewDeliveryPhoto reject with remark returns REJECTED and keeps remark") {
      setMockUserPrincipal(id = 8L, username = "admin2", authorities = listOf("PERMISSION_ORDER_REVIEW"))
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val orderId = seedOrder(userId, addressId)

      val json =
        postGraphql(
          "mutation { reviewDeliveryPhoto(orderId: $orderId, action: REJECT, remark: \"照片模糊无法确认\") { id deliveryPhotoReviewStatus deliveryPhotoReviewRemark } }",
        )
      withClue("reject should return REJECTED order with remark: $json") {
        json.contains("\"deliveryPhotoReviewStatus\" : \"REJECTED\"") &&
          json.contains("照片模糊无法确认") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("reviewDeliveryPhoto reject without remark returns readable error") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_ORDER_REVIEW"))
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val orderId = seedOrder(userId, addressId)

      val json =
        postGraphql(
          "mutation { reviewDeliveryPhoto(orderId: $orderId, action: REJECT) { id } }",
        )
      withClue("reject without remark should fail with readable message: $json") {
        json.contains("驳回必须填写原因") &&
          json.contains("\"errors\"") shouldBe true
      }
    }

    test("reviewDeliveryPhoto order without photos returns readable error") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_ORDER_REVIEW"))
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val orderId = seedOrder(userId, addressId, photos = null)

      val json =
        postGraphql(
          "mutation { reviewDeliveryPhoto(orderId: $orderId, action: APPROVE) { id } }",
        )
      withClue("order without photos should fail with readable message: $json") {
        json.contains("该订单无配送照片,无法核验") &&
          json.contains("\"errors\"") shouldBe true
      }
    }

    test("orderOperations timeline serializes DELIVERY_PHOTO_APPROVED after review") {
      setMockUserPrincipal(
        id = 7L,
        username = "admin1",
        authorities = listOf("PERMISSION_ORDER_REVIEW", "PERMISSION_ORDER_READ"),
      )
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val orderId = seedOrder(userId, addressId)

      postGraphql(
        "mutation { reviewDeliveryPhoto(orderId: $orderId, action: APPROVE) { id } }",
      )

      val json =
        postGraphql(
          "{ orderOperations(orderId: $orderId) { id operationType description } }",
        )
      withClue("timeline should serialize photo review operation without enum errors: $json") {
        json.contains("\"operationType\" : \"DELIVERY_PHOTO_APPROVED\"") &&
          json.contains("配送照片核验通过") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("reviewDeliveryPhoto can be re-reviewed to correct a previous result") {
      setMockUserPrincipal(id = 7L, username = "admin1", authorities = listOf("PERMISSION_ORDER_REVIEW"))
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val orderId = seedOrder(userId, addressId)

      val approveJson =
        postGraphql(
          "mutation { reviewDeliveryPhoto(orderId: $orderId, action: APPROVE) { id deliveryPhotoReviewStatus } }",
        )
      withClue("first approve: $approveJson") {
        approveJson.contains("\"deliveryPhotoReviewStatus\" : \"APPROVED\"") shouldBe true
      }

      val rejectJson =
        postGraphql(
          "mutation { reviewDeliveryPhoto(orderId: $orderId, action: REJECT, remark: \"先驳回复核\") { id deliveryPhotoReviewStatus } }",
        )
      withClue("reject after approve (纠错): $rejectJson") {
        rejectJson.contains("\"deliveryPhotoReviewStatus\" : \"REJECTED\"") shouldBe true
      }

      val reApproveJson =
        postGraphql(
          "mutation { reviewDeliveryPhoto(orderId: $orderId, action: APPROVE) { id deliveryPhotoReviewStatus } }",
        )
      withClue("re-approve after reject (可重复核验): $reApproveJson") {
        reApproveJson.contains("\"deliveryPhotoReviewStatus\" : \"APPROVED\"") &&
          !reApproveJson.contains("\"errors\"") shouldBe true
      }
    }

    test("reviewDeliveryPhoto without PERMISSION_ORDER_REVIEW is denied") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_ORDER_READ"))
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val orderId = seedOrder(userId, addressId)

      val json =
        postGraphql(
          "mutation { reviewDeliveryPhoto(orderId: $orderId, action: APPROVE) { id } }",
        )
      withClue("missing permission should be rejected: $json") {
        json.contains("\"errors\"") &&
          !json.contains("\"deliveryPhotoReviewStatus\" : \"APPROVED\"") shouldBe true
      }
    }

    test("reviewDeliveryPhoto anonymous is unauthorized") {
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val orderId = seedOrder(userId, addressId)

      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "mutation { reviewDeliveryPhoto(orderId: $orderId, action: APPROVE) { id } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
