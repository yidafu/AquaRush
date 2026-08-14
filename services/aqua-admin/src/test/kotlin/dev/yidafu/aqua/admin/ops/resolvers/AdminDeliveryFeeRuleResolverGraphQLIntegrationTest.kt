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
import dev.yidafu.aqua.ops.deliveryfeerule.domain.repository.DeliveryFeeRuleRepository
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
 * AdminDeliveryFeeRuleResolver GraphQL 集成测试(F-OPS-005, AC-OPS-005-01/05/06)
 * 权限:查询 PERMISSION_OPS_CONFIG_READ,变更 PERMISSION_OPS_CONFIG_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/ops/"])
@DisplayName("AdminDeliveryFeeRuleResolver GraphQL Integration Tests")
class AdminDeliveryFeeRuleResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var userActionLogRepository: UserActionLogRepository

  @Autowired
  private lateinit var deliveryFeeRuleRepository: DeliveryFeeRuleRepository

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
  @DisplayName("deliveryFeeRule query should return default rule 4/200 when unconfigured (AC-OPS-005-01)")
  fun `deliveryFeeRule returns default when unconfigured`() {
    // 清空已保存规则(共享 H2 可能被其他用例/历史运行写入),回到未配置态
    deliveryFeeRuleRepository.hardDeleteAll()
    val json = postGraphql("{ deliveryFeeRule { freeFloorLimit surchargeCents } }")
    assertTrue(json.contains("\"freeFloorLimit\" : 4") && json.contains("\"surchargeCents\" : \"200\"") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE", "PERMISSION_OPS_CONFIG_READ"])
  @DisplayName("saveDeliveryFeeRule then query should round-trip (AC-OPS-005-02)")
  fun `save and query delivery fee rule round trip`() {
    val json =
      postGraphql(
        """mutation { saveDeliveryFeeRule(input: {freeFloorLimit: 6, surchargeCents: 500}) { freeFloorLimit surchargeCents } }""",
      )
    assertTrue(json.contains("\"freeFloorLimit\" : 6") && json.contains("\"surchargeCents\" : \"500\"") && !json.contains("\"errors\""))

    val queryJson = postGraphql("{ deliveryFeeRule { freeFloorLimit surchargeCents } }")
    assertTrue(queryJson.contains("\"freeFloorLimit\" : 6") && queryJson.contains("\"surchargeCents\" : \"500\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("saveDeliveryFeeRule with invalid amount should fail (AC-OPS-005-05)")
  fun `saveDeliveryFeeRule rejects invalid amount`() {
    val json = postGraphql("mutation { saveDeliveryFeeRule(input: {freeFloorLimit: 4, surchargeCents: 100000}) { freeFloorLimit } }")
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("saveDeliveryFeeRule should write audit log (AC-OPS-005-06)")
  fun `saveDeliveryFeeRule writes audit`() {
    val json =
      postGraphql(
        """mutation { saveDeliveryFeeRule(input: {freeFloorLimit: 4, surchargeCents: 200}) { freeFloorLimit } }""",
      )
    assertTrue(!json.contains("\"errors\""))

    val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_DELIVERY_FEE_RULE_SAVE)
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
          .content("""{"query": "{ deliveryFeeRule { freeFloorLimit } }"}"""),
      ).andExpect(status().isUnauthorized)
  }
}
