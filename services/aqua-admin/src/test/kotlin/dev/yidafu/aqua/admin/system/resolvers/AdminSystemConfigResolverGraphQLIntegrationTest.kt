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

package dev.yidafu.aqua.admin.system.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
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

/**
 * AdminSystemConfig Resolver GraphQL 集成测试(F-SYS-001~006, AC-SYS-001/002/003)
 * 权限:查询 PERMISSION_SYSTEM_CONFIG_READ,变更 PERMISSION_SYSTEM_CONFIG_WRITE(仅超级管理员,R-SYS-03)。
 * 测试环境 Liquibase 禁用(Testcontainers PG + create-drop),@BeforeEach 经仓库播种 system_settings 行;
 * 生产种子参数(含 F-SYS-004/005/006)由 Liquibase v1.2.4 提供,不在测试环境自动生效。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/system/"])
@ApplyExtension(SpringExtension::class)
class AdminSystemConfigResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  systemSettingsRepository: SystemSettingsRepository,
  userActionLogRepository: UserActionLogRepository,
  productRepository: ProductRepository,
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

    fun seedSetting(
      key: String,
      value: String,
      type: SystemSettingType,
      group: SystemSettingGroup,
      defaultValue: String?,
      min: String?,
      max: String?,
    ) {
      val existing = systemSettingsRepository.findBySettingKey(key)
      if (existing != null) {
        existing.settingValue = value
        existing.updatedBy = null
        systemSettingsRepository.save(existing)
      } else {
        systemSettingsRepository.save(
          SystemSettingsModel(
            settingKey = key,
            settingValue = value,
            settingType = type,
            settingGroup = group,
            defaultValue = defaultValue,
            minValue = min,
            maxValue = max,
          ),
        )
      }
    }

    fun seedSettings() {
      seedSetting(SystemSettingKeys.LOW_STOCK_THRESHOLD, "10", SystemSettingType.NUMBER, SystemSettingGroup.INVENTORY, "10", "0", "99999")
      seedSetting(
        SystemSettingKeys.STOCK_WARNING_TIER_MID,
        "50",
        SystemSettingType.NUMBER,
        SystemSettingGroup.INVENTORY,
        "50",
        "0",
        "99999",
      )
      seedSetting(
        SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES,
        "30",
        SystemSettingType.NUMBER,
        SystemSettingGroup.ORDER_AND_FULFILLMENT,
        "30",
        "1",
        null,
      )
      seedSetting(SystemSettingKeys.PASSWORD_MIN_LENGTH, "8", SystemSettingType.NUMBER, SystemSettingGroup.SECURITY, "8", "6", "32")
    }

    /**
     * 播种系统参数(键唯一,重复执行时更新值,保持幂等)
     */

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
      seedSettings()
    }
    test("systemConfigs returns seeded rows by group") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_READ"))
      val json = postGraphql("{ systemConfigs(group: INVENTORY) { settingKey settingValue } }")
      json.contains("\"settingKey\" : \"${SystemSettingKeys.LOW_STOCK_THRESHOLD}\"") &&
        json.contains("\"settingKey\" : \"${SystemSettingKeys.STOCK_WARNING_TIER_MID}\"") &&
        !json.contains("\"settingKey\" : \"${SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES}\"") &&
        !json.contains("\"errors\"") shouldBe true

      val securityJson = postGraphql("{ systemConfigs(group: SECURITY) { settingKey } }")
      securityJson.contains("\"settingKey\" : \"${SystemSettingKeys.PASSWORD_MIN_LENGTH}\"") &&
        !securityJson.contains("\"errors\"") shouldBe true

      // 未播种分组(生产由 Liquibase v1.2.4 提供,测试环境不自动生效)返回空列表
      val emptyJson = postGraphql("{ systemConfigs(group: DATA_MAINTENANCE) { settingKey } }")
      !emptyJson.contains("\"settingKey\"") && !emptyJson.contains("\"errors\"") shouldBe true
    }
    test("save round trips and writes audit") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_READ", "PERMISSION_SYSTEM_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { saveSystemConfigs(input: [{settingKey: "${SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES}", settingValue: "15"}]) { settingKey settingValue } }""",
        )
      withClue("response: $json") { json.contains("\"settingValue\" : \"15\"") && !json.contains("\"errors\"") shouldBe true }

      val queryJson = postGraphql("{ systemConfigs(group: ORDER_AND_FULFILLMENT) { settingKey settingValue } }")
      withClue("queryJson: $queryJson") {
        queryJson.contains("\"settingKey\" : \"${SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES}\"") &&
          queryJson.contains("\"settingValue\" : \"15\"") shouldBe true
      }

      val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_SYS_CONFIG_SAVE)
      withClue("no audit rows for SAVE") { audits.isNotEmpty() shouldBe true }
      withClue("audit content: ${audits.map { it.properties }}") {
        audits.any {
          it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true
        } shouldBe true
      }
    }
    test("out of range value rejected") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { saveSystemConfigs(input: [{settingKey: "${SystemSettingKeys.LOW_STOCK_THRESHOLD}", settingValue: "100000"}]) { settingKey } }""",
        )
      withClue("out-of-range json: $json") { json.contains("\"errors\"") shouldBe true }
      withClue("out-of-range msg json: $json") { json.contains("参数值超出范围") shouldBe true }
    }
    test("stock tiers must increase") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { saveSystemConfigs(input: [{settingKey: "${SystemSettingKeys.LOW_STOCK_THRESHOLD}", settingValue: "20"}, {settingKey: "${SystemSettingKeys.STOCK_WARNING_TIER_MID}", settingValue: "10"}]) { settingKey } }""",
        )
      withClue("tiers json: $json") { json.contains("\"errors\"") shouldBe true }
      withClue("tiers msg json: $json") { json.contains("档位需递增") shouldBe true }
    }
    test("restore resets to default with audit") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_WRITE"))
      postGraphql(
        """mutation { saveSystemConfigs(input: [{settingKey: "${SystemSettingKeys.LOW_STOCK_THRESHOLD}", settingValue: "30"}]) { settingKey } }""",
      )
      val json =
        postGraphql("""mutation { restoreSystemConfig(settingKey: "${SystemSettingKeys.LOW_STOCK_THRESHOLD}") { settingValue } }""")
      json.contains("\"settingValue\" : \"10\"") && !json.contains("\"errors\"") shouldBe true

      val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_SYS_CONFIG_RESTORE)
      audits.any { it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true } shouldBe true
    }
    test("product statistics follows low stock threshold") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_WRITE", "PERMISSION_STATISTICS_READ"))
      productRepository.save(
        ProductModel(
          name = "库存消费测试水",
          price = 1000,
          coverImageUrl = "http://test.local/cover.png",
          specification = "5L",
          stock = 15,
          status = ProductModelStatus.ONLINE,
        ),
      )

      postGraphql(
        """mutation { saveSystemConfigs(input: [{settingKey: "${SystemSettingKeys.LOW_STOCK_THRESHOLD}", settingValue: "20"}]) { settingKey } }""",
      )
      val afterRaise = postGraphql("{ productStatistics { lowStockProducts } }")
      withClue("afterRaise: $afterRaise") {
        afterRaise.contains("\"lowStockProducts\" : 1") &&
          !afterRaise.contains("\"errors\"") shouldBe true
      }

      postGraphql(
        """mutation { saveSystemConfigs(input: [{settingKey: "${SystemSettingKeys.LOW_STOCK_THRESHOLD}", settingValue: "10"}]) { settingKey } }""",
      )
      val afterLower = postGraphql("{ productStatistics { lowStockProducts } }")
      afterLower.contains("\"lowStockProducts\" : 0") && !afterLower.contains("\"errors\"") shouldBe true
    }
    test("admin without system config permission denied") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_READ"))
      val json = postGraphql("{ systemConfigs(group: INVENTORY) { settingKey } }")
      json.contains("\"errors\"") shouldBe true
    }
    test("anonymous access is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ systemConfigs(group: INVENTORY) { settingKey } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
