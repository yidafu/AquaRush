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
import dev.yidafu.aqua.admin.testutil.setMockUser
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
 * 配送轨迹 GraphQL 集成测试(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03)
 *
 * - reportTrajectory:送水工上报本人配送中订单 → true 且轨迹点落库;
 * - 非配送中订单上报被拦截(errors);
 * - 无 DELIVERY_TASK 权限上报被拦截(方法级 @PreAuthorize);
 * - deliveryTrajectories/deliveryWorkerLatestLocation:仅管理员
 *   (PERMISSION_DELIVERY_WORKER_READ)可查,返回轨迹点序列/最近点;
 * - 无 DELIVERY_WORKER_READ 权限查询被拦截(R-AUTH-02)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/trajectory/"])
@ApplyExtension(SpringExtension::class)
class DeliveryTrajectoryResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  adminRepository: AdminRepository,
  orderRepository: OrderRepository,
  userRepository: UserRepository,
  addressRepository: AddressRepository,
  trajectoryRepository: DeliveryTrajectoryRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()
    var workerId: Long = 0L
    var deliveringOrderId: Long = 0L

    fun postGraphql(query: String): String =
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "$query"}"""),
        ).andExpect(status().isOk)
        .andReturn()
        .response
        .contentAsString

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()

      // 幂等种子化一个在线送水员账号(findByPhone 去重,避免共享测试库遗留行导致的唯一键冲突)
      val worker =
        adminRepository.findByPhone("13900139001")
          ?: adminRepository.save(
            AdminModel(
              username = "delivery_trajectory_seed",
              passwordHash = "hash",
              realName = "轨迹测试送水员",
              phone = "13900139001",
              role = AdminRoleModel.DELIVERY_WORKER,
              onlineStatus = DeliverWorkerModelStatus.ONLINE,
              isAvailable = true,
              earningCents = 1000L,
            ),
          )
      workerId = worker.id!!

      // 种子化用户与地址(orders.user_id/address_id 外键约束,直接落 0 会违反 FK)
      if (userRepository.findByPhone("13900139002") == null) {
        userRepository.save(
          UserModel(
            wechatOpenId = "traj_test_openid",
            nickname = "轨迹测试客户",
            phone = "13900139002",
          ),
        )
      }
      val addressUserId = userRepository.findByPhone("13900139002")!!.id!!
      val address =
        addressRepository.findByUserId(addressUserId).firstOrNull()
          ?: addressRepository.save(
            AddressModel(
              userId = addressUserId,
              receiverName = "轨迹测试客户",
              phone = "13900139002",
              province = "北京市",
              city = "北京市",
              district = "朝阳区",
              detailAddress = "测试街道 1 号",
            ),
          )

      // 种子化订单:一条配送中(归属该送水员)、一条已完成(供非配送中上报拦截用例)
      val delivering =
        orderRepository.findByOrderNo("TRAJ-DELIVERING")
          ?: orderRepository.save(
            OrderModel(
              orderNo = "TRAJ-DELIVERING",
              userId = addressUserId,
              addressId = address.id!!,
              status = OrderModelStatus.DELIVERING,
              deliveryWorkerId = workerId,
            ),
          )
      deliveringOrderId = delivering.id!!

      if (orderRepository.findByOrderNo("TRAJ-COMPLETED") == null) {
        orderRepository.save(
          OrderModel(
            orderNo = "TRAJ-COMPLETED",
            userId = addressUserId,
            addressId = address.id!!,
            status = OrderModelStatus.COMPLETED,
            deliveryWorkerId = workerId,
          ),
        )
      }
    }
    test("reportTrajectory on own delivering order returns true") {
      setMockUser(username = "delivery_trajectory_seed", authorities = listOf("PERMISSION_DELIVERY_TASK"))
      val json =
        postGraphql(
          """
          mutation {
            reportTrajectory(input: {
              orderId: $deliveringOrderId
              longitude: 116.397428
              latitude: 39.90923
              accuracy: 5.0
            })
          }
          """.trimIndent().replace("\n", " "),
        )

      withClue("上报应成功无错误: $json") { json.contains("\"reportTrajectory\"") && !json.contains("\"errors\"") shouldBe true }

      val saved = trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(workerId, deliveringOrderId)
      withClue("轨迹点应落库") { saved.isNotEmpty() shouldBe true }
      // GraphQL Float(32 位)投影精度有限,落库 double 与上报值容差比较
      withClue("落库坐标应与上报一致(容差 0.001)") {
        saved.any {
          kotlin.math.abs(it.longitude - 116.397428) < 0.001 &&
            kotlin.math.abs(it.latitude - 39.90923) < 0.001
        } shouldBe
          true
      }
    }
    test("reportTrajectory on non delivering order is rejected") {
      setMockUser(username = "delivery_trajectory_seed", authorities = listOf("PERMISSION_DELIVERY_TASK"))
      val completedOrderId =
        orderRepository.findByOrderNo("TRAJ-COMPLETED")!!.id!!

      val json =
        postGraphql(
          """
          mutation {
            reportTrajectory(input: {
              orderId: $completedOrderId
              longitude: 116.397428
              latitude: 39.90923
            })
          }
          """.trimIndent().replace("\n", " "),
        )

      withClue("非配送中上报应报错: $json") { json.contains("\"errors\"") shouldBe true }
    }
    test("reportTrajectory without permission is rejected") {
      setMockUser(username = "delivery_trajectory_seed")
      // 无 DELIVERY_TASK 权限码(仅默认 ROLE_USER),方法级 @PreAuthorize 应拒绝
      val json =
        postGraphql(
          """
          mutation {
            reportTrajectory(input: {
              orderId: $deliveringOrderId
              longitude: 116.397428
              latitude: 39.90923
            })
          }
          """.trimIndent().replace("\n", " "),
        )

      withClue("无权限上报应报错: $json") { json.contains("\"errors\"") shouldBe true }
    }
    test("admin queries trajectories and latest location") {
      setMockUser(authorities = listOf("PERMISSION_DELIVERY_WORKER_READ"))
      // 预置两个轨迹点
      trajectoryRepository.save(
        DeliveryTrajectoryModel(
          deliveryWorkerId = workerId,
          orderId = deliveringOrderId,
          longitude = 116.39,
          latitude = 39.90,
          reportedAt = LocalDateTime.of(2026, 8, 16, 10, 0, 0),
        ),
      )
      trajectoryRepository.save(
        DeliveryTrajectoryModel(
          deliveryWorkerId = workerId,
          orderId = deliveringOrderId,
          longitude = 116.40,
          latitude = 39.91,
          reportedAt = LocalDateTime.of(2026, 8, 16, 10, 0, 5),
        ),
      )

      val listJson =
        postGraphql(
          """
          query {
            deliveryTrajectories(workerId: $workerId, orderId: $deliveringOrderId) {
              id
              deliveryWorkerId
              orderId
              longitude
              latitude
              reportedAt
            }
          }
          """.trimIndent().replace("\n", " "),
        )
      withClue("查询应返回轨迹列表: $listJson") { listJson.contains("\"deliveryTrajectories\"") shouldBe true }
      // GraphQL Float 序列化为数字(如 116.4),非带引号字符串
      withClue("轨迹应包含第二个点(116.40): $listJson") { listJson.contains("116.4") shouldBe true }
      withClue("不应有错误: $listJson") { !listJson.contains("\"errors\"") shouldBe true }

      val latestJson =
        postGraphql(
          """
          query {
            deliveryWorkerLatestLocation(workerId: $workerId) {
              longitude
              latitude
              reportedAt
            }
          }
          """.trimIndent().replace("\n", " "),
        )
      withClue("最近位置应为 116.40: $latestJson") { latestJson.contains("116.4") shouldBe true }
    }
    test("trajectory query without admin permission is rejected") {
      setMockUser(username = "delivery_trajectory_seed")
      val json =
        postGraphql(
          """
          query {
            deliveryTrajectories(workerId: $workerId) {
              id
            }
          }
          """.trimIndent().replace("\n", " "),
        )

      withClue("无权限查询轨迹应报错: $json") { json.contains("\"errors\"") shouldBe true }
    }
  })
