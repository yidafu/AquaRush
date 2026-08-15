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
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.user.domain.repository.AdminRepository
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

/**
 * Integration tests for AdminDeliveryWorkerQueryResolver (admin).
 * Mirrors AdminSecurityIntegrationTest's MockMvc pattern (WebEnvironment.MOCK).
 *
 * 背景：`deliveryWorkers`/`onlineDeliveryWorkers`/`deliveryWorker` 曾在 schema 声明但
 * resolver 方法缺少 @QueryMapping 而未接线（返回 non-null 错误），本测试守护修复。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/delivery-worker/"])
@DisplayName("AdminDeliveryWorkerResolver GraphQL Integration Tests")
class AdminDeliveryWorkerResolverGraphQLIntegrationTest {
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

    // 幂等种子化一个在线送水员账号（findByPhone 去重，避免共享 H2 遗留行导致的唯一键冲突）
    // 送水员档案并入 admins 单表后(PRD/07-user.md §F-USR-021)，送水员即 role=DELIVERY_WORKER 的账号行
    if (adminRepository.findByPhone("13900139000") == null) {
      adminRepository.save(
        AdminModel(
          username = "delivery_test_seed",
          passwordHash = "hash",
          realName = "张三",
          phone = "13900139000",
          role = AdminRoleModel.DELIVERY_WORKER,
          onlineStatus = DeliverWorkerModelStatus.ONLINE,
          isAvailable = true,
          earningCents = 1000L,
        ),
      )
    }
  }

  private fun postGraphql(query: String): String {
    val result =
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "$query"}"""),
        ).andExpect(status().isOk)
        .andReturn()
    return result.response.contentAsString
  }

  @Test
  @WithMockUser(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("deliveryWorkers query should return all delivery workers for admin")
  fun `deliveryWorkers query returns workers`() {
    val json = postGraphql("{ deliveryWorkers { id name phone onlineStatus isAvailable currentTaskCount earningCents } }")

    assertTrue(json.contains("\"deliveryWorkers\"") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUser(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("onlineDeliveryWorkers query should return online delivery workers for admin")
  fun `onlineDeliveryWorkers query returns online workers`() {
    val json = postGraphql("{ onlineDeliveryWorkers { id name onlineStatus isAvailable } }")

    assertTrue(json.contains("\"onlineDeliveryWorkers\"") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUser(authorities = ["PERMISSION_DELIVERY_WORKER_READ"])
  @DisplayName("deliveryWorker(id) query should return the delivery worker for admin")
  fun `deliveryWorker query returns worker by id`() {
    val workerId = adminRepository.findByPhone("13900139000")?.id

    val json =
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
              """
              {"query":"query worker(${'$'}id: Long!) { deliveryWorker(id: ${'$'}id) { id name phone } }","variables":{"id":$workerId}}
              """.trimIndent(),
            ),
        ).andExpect(status().isOk)
        .andReturn()
        .response
        .contentAsString

    assertTrue(json.contains("\"张三\"") && json.contains("\"deliveryWorker\"") && !json.contains("\"errors\""))
  }
}
