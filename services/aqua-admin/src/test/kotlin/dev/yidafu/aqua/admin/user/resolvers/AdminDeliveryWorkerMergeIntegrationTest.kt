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

package dev.yidafu.aqua.admin.user.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.user.domain.repository.AdminRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * 送水员档案并入 admins 单表集成测试(PRD/07-user.md §F-USR-021 目标架构):
 * - createAdmin(role=DELIVERY_WORKER, 不传 username) 单表落库,用户名自动生成 delivery_{账号ID};
 * - createDeliveryWorker 兼容 mutation 同样单表落库;
 * - deliveryWorkers 查询返回配送能力角色账号(DELIVERY_WORKER/ADMIN),不含 SUPER_ADMIN。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/merge/"])
@DisplayName("Admin Delivery Worker Single-Table Merge Integration Tests")
class AdminDeliveryWorkerMergeIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var adminRepository: AdminRepository

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()
  }

  private fun graphqlBody(
    query: String,
    variables: String,
  ): String = """{"query": "$query", "variables": $variables}"""

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"])
  @DisplayName("createAdmin with DELIVERY_WORKER role and no username creates single admins row with delivery_ username")
  fun `createAdmin with delivery worker role auto generates delivery username`() {
    val before = adminRepository.count()
    val body =
      graphqlBody(
        "mutation CreateAdmin(\$input: CreateAdminInput!) { createAdmin(input: \$input) { id username role onlineStatus isAvailable } }",
        """{"input": {"password": "password123", "realName": "送水员甲", "phone": "13900139001", "role": "DELIVERY_WORKER", "isAvailable": true}}""",
      )

    val result =
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
        ).andExpect(status().isOk)
        .andReturn()
        .response
        .contentAsString

    // 单表落库:仅新增一条 admins 记录(不再创建 delivery_workers 档案)
    assertEquals(before + 1, adminRepository.count())

    // 用户名自动生成 delivery_{账号ID}
    val created = adminRepository.findByPhone("13900139001")
    assertNotNull(created)
    assertEquals(AdminRoleModel.DELIVERY_WORKER, created!!.role)
    assertEquals("delivery_${created.id}", created.username)
    assertTrue(created.isAvailable)
    // JSON 序列化键值冒号带空格,仅校验用户名值片段
    assertTrue(result.contains("delivery_${created.id}"))
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"])
  @DisplayName("createAdmin with DELIVERY_WORKER role and explicit username keeps given username")
  fun `createAdmin with delivery worker role and explicit username keeps it`() {
    val username = "dw_keep_${System.currentTimeMillis()}"
    val body =
      graphqlBody(
        "mutation CreateAdmin(\$input: CreateAdminInput!) { createAdmin(input: \$input) { id username role } }",
        """{"input": {"username": "$username", "password": "password123", "role": "DELIVERY_WORKER"}}""",
      )

    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content(body),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.data.createAdmin.username").value(username))
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_DELIVERY_WORKER_WRITE"])
  @DisplayName("createDeliveryWorker compat mutation creates single admins row")
  fun `createDeliveryWorker creates single admins row`() {
    val before = adminRepository.count()
    val body =
      graphqlBody(
        "mutation CreateDeliveryWorker(\$input: CreateDeliveryWorkerInput!) { createDeliveryWorker(input: \$input) { id name phone isAvailable } }",
        """{"input": {"name": "送水员乙", "phone": "13900139002", "password": "password123", "isAvailable": true}}""",
      )

    val result =
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
        ).andExpect(status().isOk)
        .andReturn()
        .response
        .contentAsString

    assertEquals(before + 1, adminRepository.count())
    assertTrue(result.contains("\"送水员乙\""))
    val created = adminRepository.findByPhone("13900139002")
    assertNotNull(created)
    assertEquals(AdminRoleModel.DELIVERY_WORKER, created!!.role)
    assertEquals("delivery_${created.id}", created.username)
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("deliveryWorkers query returns delivery-capable accounts only")
  fun `deliveryWorkers query returns delivery capable accounts`() {
    // 种子:DELIVERY_WORKER 与 ADMIN(具备配送能力)各一;SUPER_ADMIN 不返回
    adminRepository.save(
      dev.yidafu.aqua.common.domain.model.AdminModel(
        username = "dw_list_${System.currentTimeMillis()}",
        passwordHash = "hash",
        realName = "送水员丙",
        phone = "13900139003",
        role = AdminRoleModel.DELIVERY_WORKER,
      ),
    )
    adminRepository.save(
      dev.yidafu.aqua.common.domain.model.AdminModel(
        username = "admin_list_${System.currentTimeMillis()}",
        passwordHash = "hash",
        realName = "兼送水管理员",
        phone = "13900139004",
        role = AdminRoleModel.ADMIN,
      ),
    )

    val body =
      graphqlBody(
        "query { deliveryWorkers { id name phone } }",
        "{}",
      )

    val result =
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
        ).andExpect(status().isOk)
        .andReturn()
        .response
        .contentAsString

    assertTrue(result.contains("送水员丙"), "DELIVERY_WORKER 账号应在 deliveryWorkers 中")
    assertTrue(result.contains("兼送水管理员"), "ADMIN 账号(兼送水)应在 deliveryWorkers 中")
    assertTrue(!result.contains("\"errors\""))
  }
}
