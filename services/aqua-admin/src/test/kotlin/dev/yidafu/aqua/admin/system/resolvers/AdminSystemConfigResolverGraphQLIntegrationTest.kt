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
import dev.yidafu.aqua.admin.ops.resolvers.WithMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.ProductModel
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.ProductModelStatus
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import dev.yidafu.aqua.product.domain.repository.ProductRepository
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
 * AdminSystemConfig Resolver GraphQL 集成测试(F-SYS-001/002/003, AC-SYS-001/002/003)
 * 权限:查询 PERMISSION_SYSTEM_CONFIG_READ,变更 PERMISSION_SYSTEM_CONFIG_WRITE(仅超级管理员,R-SYS-03)。
 * 测试环境 Liquibase 禁用(H2 + create-drop),@BeforeEach 经仓库播种 system_settings 行。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/system/"])
@DisplayName("AdminSystemConfigResolver GraphQL Integration Tests")
class AdminSystemConfigResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var systemSettingsRepository: SystemSettingsRepository

  @Autowired
  private lateinit var userActionLogRepository: UserActionLogRepository

  @Autowired
  private lateinit var productRepository: ProductRepository

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()
    seedSettings()
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

  /**
   * 播种系统参数(键唯一,重复执行时更新值,保持幂等)
   */
  private fun seedSettings() {
    seedSetting("low_stock_threshold", "10", SystemSettingType.NUMBER, SystemSettingGroup.INVENTORY, "10", "0", "99999")
    seedSetting("stock_warning_tier_mid", "50", SystemSettingType.NUMBER, SystemSettingGroup.INVENTORY, "50", "0", "99999")
    seedSetting("payment_timeout_minutes", "30", SystemSettingType.NUMBER, SystemSettingGroup.ORDER_AND_FULFILLMENT, "30", "1", null)
  }

  private fun seedSetting(
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

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_SYSTEM_CONFIG_READ"])
  @DisplayName("systemConfigs query returns seeded rows by group (AC-SYS-001-01)")
  fun `systemConfigs returns seeded rows by group`() {
    val json = postGraphql("{ systemConfigs(group: INVENTORY) { settingKey settingValue } }")
    assertTrue(
      json.contains("\"settingKey\" : \"low_stock_threshold\"") &&
        json.contains("\"settingKey\" : \"stock_warning_tier_mid\"") &&
        !json.contains("\"settingKey\" : \"payment_timeout_minutes\"") &&
        !json.contains("\"errors\""),
    )

    val emptyJson = postGraphql("{ systemConfigs(group: SECURITY) { settingKey } }")
    assertTrue(!emptyJson.contains("\"settingKey\"") && !emptyJson.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_SYSTEM_CONFIG_READ", "PERMISSION_SYSTEM_CONFIG_WRITE"])
  @DisplayName("saveSystemConfigs round-trip and writes audit (AC-SYS-001-02)")
  fun `save round trips and writes audit`() {
    val json =
      postGraphql(
        """mutation { saveSystemConfigs(input: [{settingKey: "payment_timeout_minutes", settingValue: "15"}]) { settingKey settingValue } }""",
      )
    assertTrue(json.contains("\"settingValue\" : \"15\"") && !json.contains("\"errors\""), "response: $json")

    val queryJson = postGraphql("{ systemConfigs(group: ORDER_AND_FULFILLMENT) { settingKey settingValue } }")
    assertTrue(
      queryJson.contains("\"settingKey\" : \"payment_timeout_minutes\"") && queryJson.contains("\"settingValue\" : \"15\""),
      "queryJson: $queryJson",
    )

    val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_SYS_CONFIG_SAVE)
    assertTrue(audits.isNotEmpty(), "no audit rows for SAVE")
    assertTrue(
      audits.any {
        it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true
      },
      "audit content: ${audits.map { it.properties }}",
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_SYSTEM_CONFIG_WRITE"])
  @DisplayName("out-of-range value rejected with 参数值超出范围 (AC-SYS-001-02)")
  fun `out of range value rejected`() {
    val json =
      postGraphql(
        """mutation { saveSystemConfigs(input: [{settingKey: "low_stock_threshold", settingValue: "100000"}]) { settingKey } }""",
      )
    assertTrue(json.contains("\"errors\""), "out-of-range json: $json")
    assertTrue(json.contains("参数值超出范围"), "out-of-range msg json: $json")
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_SYSTEM_CONFIG_WRITE"])
  @DisplayName("stock tiers not increasing rejected with 档位需递增 (AC-SYS-003-02)")
  fun `stock tiers must increase`() {
    val json =
      postGraphql(
        """mutation { saveSystemConfigs(input: [{settingKey: "low_stock_threshold", settingValue: "20"}, {settingKey: "stock_warning_tier_mid", settingValue: "10"}]) { settingKey } }""",
      )
    assertTrue(json.contains("\"errors\""), "tiers json: $json")
    assertTrue(json.contains("档位需递增"), "tiers msg json: $json")
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_SYSTEM_CONFIG_WRITE"])
  @DisplayName("restoreSystemConfig resets to default and writes audit (AC-SYS-001-04)")
  fun `restore resets to default with audit`() {
    postGraphql(
      """mutation { saveSystemConfigs(input: [{settingKey: "low_stock_threshold", settingValue: "30"}]) { settingKey } }""",
    )
    val json = postGraphql("""mutation { restoreSystemConfig(settingKey: "low_stock_threshold") { settingValue } }""")
    assertTrue(json.contains("\"settingValue\" : \"10\"") && !json.contains("\"errors\""))

    val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_SYS_CONFIG_RESTORE)
    assertTrue(audits.any { it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true })
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_SYSTEM_CONFIG_WRITE", "PERMISSION_STATISTICS_READ"])
  @DisplayName("inventory consumption: low stock list follows threshold (AC-SYS-003-01)")
  fun `product statistics follows low stock threshold`() {
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
      """mutation { saveSystemConfigs(input: [{settingKey: "low_stock_threshold", settingValue: "20"}]) { settingKey } }""",
    )
    val afterRaise = postGraphql("{ productStatistics { lowStockProducts } }")
    assertTrue(afterRaise.contains("\"lowStockProducts\" : 1") && !afterRaise.contains("\"errors\""), "afterRaise: $afterRaise")

    postGraphql(
      """mutation { saveSystemConfigs(input: [{settingKey: "low_stock_threshold", settingValue: "10"}]) { settingKey } }""",
    )
    val afterLower = postGraphql("{ productStatistics { lowStockProducts } }")
    assertTrue(afterLower.contains("\"lowStockProducts\" : 0") && !afterLower.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_READ"])
  @DisplayName("admin without SYSTEM_CONFIG_READ denied (AC-SYS-001-05, R-SYS-03)")
  fun `admin without system config permission denied`() {
    val json = postGraphql("{ systemConfigs(group: INVENTORY) { settingKey } }")
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @DisplayName("anonymous should be denied")
  fun `anonymous access is denied`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "{ systemConfigs(group: INVENTORY) { settingKey } }"}"""),
      ).andExpect(status().isUnauthorized)
  }
}
