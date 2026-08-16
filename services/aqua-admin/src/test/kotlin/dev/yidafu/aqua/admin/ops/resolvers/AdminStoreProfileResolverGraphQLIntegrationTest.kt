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
import dev.yidafu.aqua.ops.storeprofile.domain.repository.StoreProfileRepository
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
 * AdminStoreProfileResolver GraphQL 集成测试(F-OPS-004, AC-OPS-004-01~04)
 * 权限:查询 PERMISSION_OPS_CONFIG_READ,变更 PERMISSION_OPS_CONFIG_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/ops/"])
@ApplyExtension(SpringExtension::class)
class AdminStoreProfileResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  userActionLogRepository: UserActionLogRepository,
  storeProfileRepository: StoreProfileRepository,
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
    test("storeProfile returns null when unconfigured") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_READ"))
      // 清空已保存店铺信息(共享 H2 可能被其他用例/历史运行写入)
      storeProfileRepository.hardDeleteAll()
      val json = postGraphql("{ storeProfile { shopName serviceHotline } }")
      json.contains("\"storeProfile\"") && json.contains("null") && !json.contains("\"errors\"") shouldBe true
    }
    test("save and query store profile round trip") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE", "PERMISSION_OPS_CONFIG_READ"))
      val title = "好喝山泉${System.currentTimeMillis() % 10000}"
      val json =
        postGraphql(
          """mutation { saveStoreProfile(input: {shopName: "$title", logoUrl: "http://localhost:9090/api/v1/storage/files/logo.png", serviceHotline: "400-800-8888", businessHoursStart: "08:00", businessHoursEnd: "21:00", shopIntro: "天然山泉水", aboutUs: "关于我们文案"}) { shopName logoUrl serviceHotline businessHoursStart businessHoursEnd shopIntro aboutUs } }""",
        )
      json.contains("\"$title\"") && json.contains("\"08:00\"") && !json.contains("\"errors\"") shouldBe true

      val queryJson = postGraphql("{ storeProfile { shopName serviceHotline } }")
      queryJson.contains("\"$title\"") && queryJson.contains("\"400-800-8888\"") shouldBe true
    }
    test("saveStoreProfile rejects invalid hotline") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { saveStoreProfile(input: {shopName: "测试店", serviceHotline: "not-a-phone"}) { shopName } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("saveStoreProfile writes audit") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { saveStoreProfile(input: {shopName: "审计店铺", serviceHotline: "400-800-8888"}) { shopName } }""",
        )
      !json.contains("\"errors\"") shouldBe true

      val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_STORE_PROFILE_SAVE)
      audits.isNotEmpty() shouldBe true
      audits.any { it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true } shouldBe true
    }
    test("anonymous access is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ storeProfile { shopName } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
