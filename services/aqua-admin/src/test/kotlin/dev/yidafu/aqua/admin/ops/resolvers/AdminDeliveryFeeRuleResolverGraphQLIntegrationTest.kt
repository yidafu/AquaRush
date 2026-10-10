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
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.deliveryfeerule.domain.repository.DeliveryFeeRuleRepository
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
 * AdminDeliveryFeeRuleResolver GraphQL 集成测试(F-OPS-005, AC-OPS-005-01/05/06)
 * 权限:查询 PERMISSION_OPS_CONFIG_READ,变更 PERMISSION_OPS_CONFIG_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/ops/"])
@ApplyExtension(SpringExtension::class)
class AdminDeliveryFeeRuleResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  userActionLogRepository: UserActionLogRepository,
  deliveryFeeRuleRepository: DeliveryFeeRuleRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun postGraphql(query: String): String {
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

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
    }
    test("deliveryFeeRule returns default when unconfigured") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_READ"))
      // 清空已保存规则(共享测试库可能被其他用例/历史运行写入),回到未配置态
      deliveryFeeRuleRepository.hardDeleteAll()
      val json = postGraphql("{ deliveryFeeRule { freeFloorLimit surchargeCents } }")
      json.contains("\"freeFloorLimit\" : 4") && json.contains("\"surchargeCents\" : \"200\"") && !json.contains("\"errors\"") shouldBe true
    }
    test("save and query delivery fee rule round trip") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE", "PERMISSION_OPS_CONFIG_READ"))
      val json =
        postGraphql(
          """mutation { saveDeliveryFeeRule(input: {freeFloorLimit: 6, surchargeCents: 500}) { freeFloorLimit surchargeCents } }""",
        )
      json.contains("\"freeFloorLimit\" : 6") && json.contains("\"surchargeCents\" : \"500\"") && !json.contains("\"errors\"") shouldBe true

      val queryJson = postGraphql("{ deliveryFeeRule { freeFloorLimit surchargeCents } }")
      queryJson.contains("\"freeFloorLimit\" : 6") && queryJson.contains("\"surchargeCents\" : \"500\"") shouldBe true
    }
    test("saveDeliveryFeeRule rejects invalid amount") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val json =
        postGraphql(
          "mutation { saveDeliveryFeeRule(input: {freeFloorLimit: 4, surchargeCents: 100000, " +
            "tiers: [{fromFloor: 5, toFloor: 6, feeCents: 200}, {fromFloor: 7, toFloor: null, feeCents: 800}]}) { freeFloorLimit } }",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("saveDeliveryFeeRule writes audit") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { saveDeliveryFeeRule(input: {freeFloorLimit: 4, surchargeCents: 200, tiers: [{fromFloor: 5, toFloor: 6, feeCents: 200}, {fromFloor: 7, toFloor: 9, feeCents: 500}, {fromFloor: 10, toFloor: null, feeCents: 800}]}) { freeFloorLimit } }""",
        )
      val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_DELIVERY_FEE_RULE_SAVE)
      !json.contains("\"errors\"") shouldBe true
      audits.isNotEmpty() shouldBe true
      audits.any { it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true } shouldBe true
    }
    test("anonymous access is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ deliveryFeeRule { freeFloorLimit } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
