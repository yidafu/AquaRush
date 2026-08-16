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
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime

/**
 * 订单详情页 GraphQL 错误处理回归集成测试(订单详情页单请求 ORDER_DETAIL_PAGE_DATA_QUERY)。
 *
 * 回归目标(GraphQLErrorHandler 注册为 DataFetcherExceptionResolver 前失败):
 * - orderId 不存在时,order 查询抛 NotFoundException(BusinessException),此前无任何
 *   DataFetcherExceptionResolver 处理它 → Spring GraphQL 默认掩码为
 *   `INTERNAL_ERROR for <executionId>`(classification: INTERNAL_ERROR),前端无法得知
 *   「订单不存在」;同时同请求内 onlineDeliveryWorkers / orderOperations 仍正常返回。
 * - 修复后:order 应返回可读错误 message="订单不存在: X" + extensions.code=NOT_FOUND,
 *   且 data.order=null;兄弟字段不受影响。
 *
 * 权限:PERMISSION_ORDER_READ / PERMISSION_DELIVERY_WORKER_READ。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/order-error/"])
@ApplyExtension(SpringExtension::class)
class AdminOrderErrorHandlingGraphQLIntegrationTest(
  context: WebApplicationContext,
  orderRepository: OrderRepository,
  addressRepository: AddressRepository,
  userRepository: UserRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    val pageQuery =
      """
      query OrderDetailPageData(${'$'}orderId: PrimaryId!) {
        order(orderId: ${'$'}orderId) {
          id
          orderNo
        }
        onlineDeliveryWorkers {
          id
          name
          phone
        }
        orderOperations(orderId: ${'$'}orderId) {
          id
        }
      }
      """.trimIndent()

    fun postGraphql(
      query: String,
      variables: String,
    ): String {
      // GraphQL 查询需为单行 JSON 字符串:折叠换行/缩进,并转义反斜杠与双引号
      val oneLineQuery = query.replace(Regex("\\s+"), " ").trim()
      val escapedQuery = oneLineQuery.replace("\\", "\\\\").replace("\"", "\\\"")
      val result =
        mockMvc
          .perform(
            post("/graphql")
              .contentType(MediaType.APPLICATION_JSON)
              .content("""{"query": "$escapedQuery", "variables": $variables}"""),
          ).andReturn()
      if (result.response.status != 200) {
        println("=== GRAPHQL STATUS ${result.response.status} BODY: ${result.response.contentAsString}")
      }
      result.response.status shouldBe 200
      return result.response.contentAsString
    }

    beforeTest {
      setMockUserPrincipal(
        authorities = listOf("PERMISSION_ORDER_READ", "PERMISSION_DELIVERY_WORKER_READ"),
      )
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
    }

    test("nonexistent orderId returns readable NOT_FOUND error, not masked INTERNAL_ERROR") {
      val nonexistentId = 999999999999999999L

      val body =
        postGraphql(
          pageQuery,
          """{"orderId": $nonexistentId}""",
        )

      withClue("response body: $body") {
        // order 字段:可读业务错误 + NOT_FOUND code,而非 INTERNAL_ERROR 掩码
        body shouldContain "订单不存在: $nonexistentId"
        body shouldContain "NOT_FOUND"
        body shouldNotContain "INTERNAL_ERROR"
        // 兄弟字段不受影响(部分成功语义)
        body shouldContain "onlineDeliveryWorkers"
        body shouldContain "orderOperations"
      }
    }

    test("existing order resolves successfully alongside error-free siblings") {
      val user =
        userRepository.save(
          UserModel(
            wechatOpenId = "err-test-openid-${System.nanoTime()}",
            nickname = "错误处理测试用户",
          ),
        )
      val address =
        addressRepository.save(
          AddressModel(
            userId = requireNotNull(user.id),
            receiverName = "测试收件人",
            phone = "13800138000",
            province = "广东省",
            city = "深圳市",
            district = "南山区",
            detailAddress = "测试路 1 号",
          ),
        )
      val order =
        orderRepository.save(
          OrderModel(
            orderNo = "ERR-TEST-${System.nanoTime()}",
            userId = requireNotNull(user.id),
            amountCents = 1500,
            addressId = requireNotNull(address.id),
            status = OrderModelStatus.PENDING_DELIVERY,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
          ),
        )
      val orderId = requireNotNull(order.id)

      val body =
        postGraphql(
          pageQuery,
          """{"orderId": $orderId}""",
        )

      withClue("response body: $body") {
        body shouldContain "\"order\""
        body shouldContain order.orderNo
        body shouldNotContain "\"errors\""
      }
    }
  })
