/**
 * AquaRush Admin Subscription GraphQL Integration Test
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

package dev.yidafu.aqua.admin.subscription.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.product.domain.repository.ProductRepository
import dev.yidafu.aqua.subscription.domain.repository.SubscriptionRepository
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

/**
 * 周期订购计划 GraphQL 集成测试(G77/F-ORD-026,后台入口):
 * 创建(绑定用户+地址+在售商品)→ 列表(用户昵称/明细商品名/周期文案)→ 暂停/恢复/终止状态机 →
 * 权限门(缺 PERMISSION_SUBSCRIPTION_WRITE 拒绝创建)。
 *
 * 自包含 schema:classpath:graphql/subscription/(仿 photo-review/ 测试目录模式)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/subscription/"])
@ApplyExtension(SpringExtension::class)
class AdminSubscriptionGraphQLIntegrationTest(
  context: WebApplicationContext,
  userRepository: UserRepository,
  addressRepository: AddressRepository,
  productRepository: ProductRepository,
  subscriptionRepository: SubscriptionRepository,
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
            wechatOpenId = "sub-openid-${System.nanoTime()}",
            nickname = "周期订购测试客户",
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

    fun seedProduct(): Long =
      productRepository
        .save(
          ProductModel(
            name = "周期订购测试水${System.nanoTime()}",
            price = 2000,
            stock = 50,
            status = ProductModelStatus.ONLINE,
          ),
        ).id!!

    /** 创建计划,返回计划 id(createSubscription { id }) */
    fun createPlan(
      userId: Long,
      addressId: Long,
      productId: Long,
    ): Long {
      val json =
        postGraphql(
          """
          mutation {
            createSubscription(input: {
              userId: $userId,
              addressId: $addressId,
              items: [{ productId: $productId, quantity: 2 }],
              cycleType: WEEKLY,
              weekdays: [1, 3, 5],
              timeSlot: MORNING,
              startDate: "2026-03-02",
              remark: "公司前台"
            }) { id status nextRunDate }
          }
          """.trimIndent().replace("\n", " "),
        )
      withClue("createSubscription should succeed: $json") {
        json.contains("\"status\" : \"ACTIVE\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
      // id 可能序列化为裸数字或带引号字符串(自定义 Long 标量),两种都接受
      val marker = "\"id\" : "
      val start = json.indexOf(marker) + marker.length
      val rest = json.substring(start)
      val end = rest.indexOfFirst { it == ',' || it == '}' }
      return rest
        .substring(0, end)
        .trim()
        .trim('"')
        .toLong()
    }

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
      setMockUserPrincipal(
        id = 7L,
        username = "admin1",
        authorities = listOf("PERMISSION_SUBSCRIPTION_READ", "PERMISSION_SUBSCRIPTION_WRITE"),
      )
    }

    test("create subscription then list shows user, items and cycle rule") {
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val productId = seedProduct()
      val planId = createPlan(userId, addressId, productId)

      val json =
        postGraphql(
          "{ subscriptions(input: { page: 0, size: 20 }) { totalElements content { id status cycleType weekdays timeSlot nextRunDate remark user { nickname } items { productId productName quantity } } } }",
        )
      withClue("subscriptions list should include seeded plan: $json") {
        json.contains("\"totalElements\" : 1") &&
          json.contains("\"status\" : \"ACTIVE\"") &&
          json.contains("\"cycleType\" : \"WEEKLY\"") &&
          json.contains("\"timeSlot\" : \"MORNING\"") &&
          json.contains("\"nextRunDate\" : \"2026-03-02\"") &&
          json.contains("周期订购测试客户") &&
          json.contains("\"productName\"") &&
          json.contains("公司前台") &&
          !json.contains("\"errors\"") shouldBe true
      }

      // 计划详情单查
      val detail =
        postGraphql("{ subscription(subscriptionId: $planId) { id status executedPeriods } }")
      withClue("subscription detail: $detail") {
        detail.contains("\"executedPeriods\" : 0") &&
          !detail.contains("\"errors\"") shouldBe true
      }
    }

    test("pause → resume → terminate state machine over graphql") {
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val productId = seedProduct()
      val planId = createPlan(userId, addressId, productId)

      val paused =
        postGraphql("mutation { pauseSubscription(subscriptionId: $planId) { id status } }")
      withClue("pause: $paused") {
        paused.contains("\"status\" : \"PAUSED\"") && !paused.contains("\"errors\"") shouldBe true
      }

      val resumed =
        postGraphql("mutation { resumeSubscription(subscriptionId: $planId) { id status } }")
      withClue("resume: $resumed") {
        resumed.contains("\"status\" : \"ACTIVE\"") && !resumed.contains("\"errors\"") shouldBe true
      }

      val terminated =
        postGraphql("mutation { terminateSubscription(subscriptionId: $planId) { id status } }")
      withClue("terminate: $terminated") {
        terminated.contains("\"status\" : \"TERMINATED\"") && !terminated.contains("\"errors\"") shouldBe true
      }

      // 终止不可恢复:再次终止应报错
      val doubleTerminate =
        postGraphql("mutation { terminateSubscription(subscriptionId: $planId) { id status } }")
      withClue("double terminate rejected: $doubleTerminate") {
        doubleTerminate.contains("\"errors\"") shouldBe true
      }
    }

    test("create without SUBSCRIPTION_WRITE is rejected") {
      setMockUserPrincipal(id = 7L, username = "admin1", authorities = listOf("PERMISSION_SUBSCRIPTION_READ"))
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val productId = seedProduct()

      val json =
        postGraphql(
          """
          mutation {
            createSubscription(input: {
              userId: $userId,
              addressId: $addressId,
              items: [{ productId: $productId, quantity: 1 }],
              cycleType: WEEKLY,
              weekdays: [1],
              timeSlot: MORNING,
              startDate: "2026-03-02"
            }) { id }
          }
          """.trimIndent().replace("\n", " "),
        )
      withClue("missing WRITE permission should deny: $json") {
        json.contains("\"errors\"") shouldBe true
      }
    }

    test("create with mismatched address is a readable error") {
      val userId = seedUser()
      val otherUserId = seedUser()
      val addressId = seedAddress(otherUserId)
      val productId = seedProduct()

      val json =
        postGraphql(
          """
          mutation {
            createSubscription(input: {
              userId: $userId,
              addressId: $addressId,
              items: [{ productId: $productId, quantity: 1 }],
              cycleType: WEEKLY,
              weekdays: [1],
              timeSlot: MORNING,
              startDate: "2026-03-02"
            }) { id }
          }
          """.trimIndent().replace("\n", " "),
        )
      withClue("address not owned by user should fail: $json") {
        json.contains("收货地址与用户不匹配") &&
          json.contains("\"errors\"") shouldBe true
      }
    }

    test("subscriptionRepository sees rows created through graphql") {
      val userId = seedUser()
      val addressId = seedAddress(userId)
      val productId = seedProduct()
      val planId = createPlan(userId, addressId, productId)

      val plan = subscriptionRepository.findById(planId)
      (plan != null) shouldBe true
      plan!!.createdBy shouldBe 7L
      plan.remark shouldBe "公司前台"
    }
  })
