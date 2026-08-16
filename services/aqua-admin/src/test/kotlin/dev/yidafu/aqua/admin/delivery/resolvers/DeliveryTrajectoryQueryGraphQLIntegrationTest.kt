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

package dev.yidafu.aqua.admin.delivery.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.AddressModel
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.delivery.domain.repository.DeliveryTrajectoryRepository
import dev.yidafu.aqua.order.domain.repository.OrderRepository
import dev.yidafu.aqua.user.domain.repository.AddressRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
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
 * 配送轨迹查询 GraphQL 回归集成测试(后台轨迹管理页,F-ADR-012)。
 *
 * 回归目标:deliveryTrajectories 不带 orderId/startTime/endTime(默认全部轨迹)时,
 * 服务层此前用 LocalDateTime.MIN.plusDays(1)/LocalDateTime.MAX.minusNanos(1) 作为
 * 兜底时间边界 —— 极端年份(±999999999)超出 PostgreSQL timestamp 范围(4713 BC ~
 * 294276 AD),JDBC 绑定参数抛异常 → 整棵 data 为 null + INTERNAL_ERROR 掩码。
 * 修复后:无时间参数查询应正常返回该送水工全部轨迹,无 errors。
 *
 * 仅做只读查询(不调用 reportTrajectory),避免既有同类集成测试的数据串扰问题
 * (见 aqua-admin/build.gradle.kts 中被排除的 DeliveryTrajectoryResolverGraphQLIntegrationTest)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/trajectory/"])
@ApplyExtension(SpringExtension::class)
class DeliveryTrajectoryQueryGraphQLIntegrationTest(
  context: WebApplicationContext,
  adminRepository: AdminRepository,
  orderRepository: OrderRepository,
  userRepository: UserRepository,
  addressRepository: AddressRepository,
  trajectoryRepository: DeliveryTrajectoryRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()
    var workerId: Long = 0L
    var orderId: Long = 0L

    fun postGraphql(query: String): String {
      // GraphQL 查询需为单行 JSON 字符串:折叠空白,并转义反斜杠与双引号(内联字符串参数)
      val oneLineQuery = query.replace(Regex("\\s+"), " ").trim()
      val escapedQuery = oneLineQuery.replace("\\", "\\\\").replace("\"", "\\\"")
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

    beforeTest {
      setMockUserPrincipal(authorities = listOf("PERMISSION_DELIVERY_WORKER_READ"))
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()

      // 幂等种子化一个送水员账号 + 用户/地址/订单 + 两个轨迹点
      val worker =
        adminRepository.findByPhone("13900139011")
          ?: adminRepository.save(
            AdminModel(
              username = "traj_query_seed",
              passwordHash = "hash",
              realName = "轨迹查询测试送水员",
              phone = "13900139011",
              role = AdminRoleModel.DELIVERY_WORKER,
              onlineStatus = DeliverWorkerModelStatus.ONLINE,
              isAvailable = true,
              earningCents = 1000L,
            ),
          )
      workerId = worker.id!!

      val user =
        userRepository.findByPhone("13900139012")
          ?: userRepository.save(
            UserModel(
              wechatOpenId = "traj_query_openid",
              nickname = "轨迹查询测试客户",
              phone = "13900139012",
            ),
          )
      val address =
        addressRepository.findByUserId(user.id!!).firstOrNull()
          ?: addressRepository.save(
            AddressModel(
              userId = user.id!!,
              receiverName = "轨迹查询测试客户",
              phone = "13900139012",
              province = "北京市",
              city = "北京市",
              district = "朝阳区",
              detailAddress = "查询测试街道 1 号",
            ),
          )
      val order =
        orderRepository.findByOrderNo("TRAJ-QUERY-ORDER")
          ?: orderRepository.save(
            OrderModel(
              orderNo = "TRAJ-QUERY-ORDER",
              userId = user.id!!,
              addressId = address.id!!,
              status = OrderModelStatus.COMPLETED,
              deliveryWorkerId = workerId,
            ),
          )
      orderId = order.id!!

      if (trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(workerId, orderId).isEmpty()) {
        trajectoryRepository.saveAll(
          listOf(
            DeliveryTrajectoryModel(
              deliveryWorkerId = workerId,
              orderId = orderId,
              longitude = 116.39,
              latitude = 39.90,
              reportedAt = LocalDateTime.of(2026, 8, 16, 10, 0, 0),
            ),
            DeliveryTrajectoryModel(
              deliveryWorkerId = workerId,
              orderId = orderId,
              longitude = 116.40,
              latitude = 39.91,
              reportedAt = LocalDateTime.of(2026, 8, 16, 10, 0, 5),
            ),
          ),
        )
      }
    }

    test("deliveryTrajectories without orderId/time range returns all trajectories, no INTERNAL_ERROR") {
      // 回归:不带任何时间参数(前端轨迹管理页默认 range=[null,null] 的形态)
      val body =
        postGraphql(
          """
          query {
            deliveryTrajectories(workerId: $workerId) {
              id
              longitude
              latitude
              reportedAt
            }
          }
          """.trimIndent(),
        )

      withClue("response body: $body") {
        body shouldContain "\"deliveryTrajectories\""
        body shouldContain "116.4"
        body shouldNotContain "INTERNAL_ERROR"
        body shouldNotContain "\"errors\""
      }
    }

    test("deliveryTrajectories with only startTime returns trajectories since bound") {
      val body =
        postGraphql(
          """
          query {
            deliveryTrajectories(workerId: $workerId, startTime: "2026-08-16T09:00:00") {
              id
              longitude
            }
          }
          """.trimIndent(),
        )

      withClue("response body: $body") {
        body shouldContain "\"deliveryTrajectories\""
        body shouldNotContain "INTERNAL_ERROR"
        body shouldNotContain "\"errors\""
      }
    }

    test("deliveryTrajectories by orderId still resolves") {
      val body =
        postGraphql(
          """
          query {
            deliveryTrajectories(workerId: $workerId, orderId: $orderId) {
              id
              longitude
            }
          }
          """.trimIndent(),
        )

      withClue("response body: $body") {
        body shouldContain "\"deliveryTrajectories\""
        body shouldContain "116.4"
        body shouldNotContain "\"errors\""
      }
    }
  })
