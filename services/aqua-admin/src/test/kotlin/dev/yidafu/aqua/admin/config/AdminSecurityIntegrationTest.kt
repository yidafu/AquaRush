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

package dev.yidafu.aqua.admin.config

import dev.yidafu.aqua.admin.AquaAdminApplication
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
 * 安全集成测试：验证方法级安全（@EnableMethodSecurity + @PreAuthorize）真正生效。
 *
 * 关键点：
 * - 无 token 请求 /graphql → URL 层拒绝 401；
 * - 普通 ADMIN（无 PERMISSION_ADMIN_USER_WRITE）调 createAdmin → 方法级拒绝，
 *   GraphQL 内表现为 HTTP 200 + errors[0]（data.createAdmin 不存在）；
 * - SUPER_ADMIN（含 PERMISSION_ADMIN_USER_WRITE）调 createAdmin → HTTP 200 + data。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/security/"])
@DisplayName("Admin Security Integration Tests")
class AdminSecurityIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  private lateinit var mockMvc: MockMvc

  private val createAdminQuery =
    "mutation CreateAdmin(\$input: CreateAdminInput!) { createAdmin(input: \$input) { id username } }"

  @BeforeEach
  fun setUp() {
    // Boot 4.0 不再提供 @AutoConfigureMockMvc，手动装配并挂载 springSecurity 过滤器链
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
  @DisplayName("unauthenticated POST /graphql returns 401")
  fun `unauthenticated graphql returns 401`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query":"{ _dummy }"}"""),
      ).andExpect(status().isUnauthorized)
  }

  @Test
  @WithMockUser(authorities = ["ROLE_ADMIN", "PERMISSION_ORDER_READ", "PERMISSION_PRODUCT_WRITE"])
  @DisplayName("ADMIN without ADMIN_USER_WRITE is denied createAdmin")
  fun `admin without admin_user_write cannot create admin`() {
    val body =
      graphqlBody(
        createAdminQuery,
        """{"input": {"username": "deniedadmin", "password": "password123", "role": "ADMIN"}}""",
      )

    val result =
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
        ).andReturn()
    println("=== DENIED RESPONSE STATUS: ${result.response.status}")
    println("=== DENIED RESPONSE BODY: ${result.response.contentAsString}")
    org.junit.jupiter.api.Assertions
      .assertEquals(200, result.response.status)
    org.junit.jupiter.api.Assertions.assertTrue(
      result.response.contentAsString.contains("\"errors\"") || !result.response.contentAsString.contains("\"data\":{\"createAdmin\""),
    )
  }

  @Test
  @WithMockUser(authorities = ["ROLE_SUPER_ADMIN", "ROLE_ADMIN", "PERMISSION_ADMIN_USER_WRITE"])
  @DisplayName("SUPER_ADMIN with ADMIN_USER_WRITE can createAdmin")
  fun `super admin can create admin`() {
    // 用户名带时间戳后缀，避免共享 H2 在多次运行间重复创建导致唯一键冲突
    val username = "allowedadmin${System.currentTimeMillis()}"
    val body =
      graphqlBody(
        createAdminQuery,
        """{"input": {"username": "$username", "password": "password123", "role": "SUPER_ADMIN"}}""",
      )

    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content(body),
      ).andExpect(status().isOk)
      .andExpect(jsonPath("$.data.createAdmin.username").value(username))
  }
}
