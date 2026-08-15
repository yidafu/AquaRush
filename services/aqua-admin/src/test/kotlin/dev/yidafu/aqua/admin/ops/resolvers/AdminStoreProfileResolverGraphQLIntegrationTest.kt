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

package dev.yidafu.aqua.admin.ops.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.storeprofile.domain.repository.StoreProfileRepository
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

/**
 * AdminStoreProfileResolver GraphQL 集成测试(F-OPS-004, AC-OPS-004-01~04)
 * 权限:查询 PERMISSION_OPS_CONFIG_READ,变更 PERMISSION_OPS_CONFIG_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/ops/"])
@DisplayName("AdminStoreProfileResolver GraphQL Integration Tests")
class AdminStoreProfileResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var userActionLogRepository: UserActionLogRepository

  @Autowired
  private lateinit var storeProfileRepository: StoreProfileRepository

  private lateinit var mockMvc: MockMvc

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

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_READ"])
  @DisplayName("storeProfile query should return null when unconfigured")
  fun `storeProfile returns null when unconfigured`() {
    // 清空已保存店铺信息(共享 H2 可能被其他用例/历史运行写入)
    storeProfileRepository.hardDeleteAll()
    val json = postGraphql("{ storeProfile { shopName serviceHotline } }")
    assertTrue(json.contains("\"storeProfile\"") && json.contains("null") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE", "PERMISSION_OPS_CONFIG_READ"])
  @DisplayName("saveStoreProfile then query should round-trip (AC-OPS-004-01/02)")
  fun `save and query store profile round trip`() {
    val title = "好喝山泉${System.currentTimeMillis() % 10000}"
    val json =
      postGraphql(
        """mutation { saveStoreProfile(input: {shopName: "$title", logoUrl: "http://localhost:9090/api/v1/storage/files/logo.png", serviceHotline: "400-800-8888", businessHoursStart: "08:00", businessHoursEnd: "21:00", shopIntro: "天然山泉水", aboutUs: "关于我们文案"}) { shopName logoUrl serviceHotline businessHoursStart businessHoursEnd shopIntro aboutUs } }""",
      )
    assertTrue(json.contains("\"$title\"") && json.contains("\"08:00\"") && !json.contains("\"errors\""))

    val queryJson = postGraphql("{ storeProfile { shopName serviceHotline } }")
    assertTrue(queryJson.contains("\"$title\"") && queryJson.contains("\"400-800-8888\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("saveStoreProfile with invalid hotline should fail (AC-OPS-004-03)")
  fun `saveStoreProfile rejects invalid hotline`() {
    val json =
      postGraphql(
        """mutation { saveStoreProfile(input: {shopName: "测试店", serviceHotline: "not-a-phone"}) { shopName } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("saveStoreProfile should write audit log with before/after (AC-OPS-004-04)")
  fun `saveStoreProfile writes audit`() {
    val json =
      postGraphql(
        """mutation { saveStoreProfile(input: {shopName: "审计店铺", serviceHotline: "400-800-8888"}) { shopName } }""",
      )
    assertTrue(!json.contains("\"errors\""))

    val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_STORE_PROFILE_SAVE)
    assertTrue(audits.isNotEmpty())
    assertTrue(audits.any { it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true })
  }

  @Test
  @DisplayName("anonymous should be denied")
  fun `anonymous access is denied`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "{ storeProfile { shopName } }"}"""),
      ).andExpect(status().isUnauthorized)
  }
}
