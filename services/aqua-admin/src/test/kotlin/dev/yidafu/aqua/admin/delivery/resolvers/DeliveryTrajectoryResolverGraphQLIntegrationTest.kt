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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
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
@DisplayName("DeliveryTrajectory Resolver GraphQL Integration Tests")
class DeliveryTrajectoryResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var adminRepository: AdminRepository

  @Autowired
  private lateinit var orderRepository: OrderRepository

  @Autowired
  private lateinit var userRepository: UserRepository

  @Autowired
  private lateinit var addressRepository: AddressRepository

  @Autowired
  private lateinit var trajectoryRepository: DeliveryTrajectoryRepository

  private lateinit var mockMvc: MockMvc

  private var workerId: Long = 0L
  private var deliveringOrderId: Long = 0L

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()

    // 幂等种子化一个在线送水员账号(findByPhone 去重,避免共享 H2 遗留行导致的唯一键冲突)
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

  private fun postGraphql(query: String): String =
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "$query"}"""),
      ).andExpect(status().isOk)
      .andReturn()
      .response
      .contentAsString

  @Test
  @WithMockUser(username = "delivery_trajectory_seed", authorities = ["PERMISSION_DELIVERY_TASK"])
  @DisplayName("R-ADR-03:送水工上报本人配送中订单 → true 且轨迹点落库")
  fun `reportTrajectory on own delivering order returns true`() {
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

    assertTrue(json.contains("\"reportTrajectory\"") && !json.contains("\"errors\""), "上报应成功无错误: $json")

    val saved = trajectoryRepository.findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(workerId, deliveringOrderId)
    assertTrue(saved.isNotEmpty(), "轨迹点应落库")
    // GraphQL Float(32 位)投影精度有限,落库 double 与上报值容差比较
    assertTrue(
      saved.any { kotlin.math.abs(it.longitude - 116.397428) < 0.001 && kotlin.math.abs(it.latitude - 39.90923) < 0.001 },
      "落库坐标应与上报一致(容差 0.001)",
    )
  }

  @Test
  @WithMockUser(username = "delivery_trajectory_seed", authorities = ["PERMISSION_DELIVERY_TASK"])
  @DisplayName("R-ADR-03:非配送中订单上报被拦截(errors)")
  fun `reportTrajectory on non delivering order is rejected`() {
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

    assertTrue(json.contains("\"errors\""), "非配送中上报应报错: $json")
  }

  @Test
  @WithMockUser(username = "delivery_trajectory_seed")
  @DisplayName("R-ADR-03:无 DELIVERY_TASK 权限的账号上报被拦截")
  fun `reportTrajectory without permission is rejected`() {
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

    assertTrue(json.contains("\"errors\""), "无权限上报应报错: $json")
  }

  @Test
  @WithMockUser(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("R-ADR-03:管理员查询送水工轨迹与最近位置")
  fun `admin queries trajectories and latest location`() {
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
    assertTrue(listJson.contains("\"deliveryTrajectories\""), "查询应返回轨迹列表: $listJson")
    // GraphQL Float 序列化为数字(如 116.4),非带引号字符串
    assertTrue(listJson.contains("116.4"), "轨迹应包含第二个点(116.40): $listJson")
    assertTrue(!listJson.contains("\"errors\""), "不应有错误: $listJson")

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
    assertTrue(latestJson.contains("116.4"), "最近位置应为 116.40: $latestJson")
  }

  @Test
  @WithMockUser(username = "delivery_trajectory_seed")
  @DisplayName("R-AUTH-02:无 DELIVERY_WORKER_READ 权限的账号不可查轨迹")
  fun `trajectory query without admin permission is rejected`() {
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

    assertTrue(json.contains("\"errors\""), "无权限查询轨迹应报错: $json")
  }
}
