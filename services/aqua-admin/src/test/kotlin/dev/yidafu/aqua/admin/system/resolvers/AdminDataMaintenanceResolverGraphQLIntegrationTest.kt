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
import dev.yidafu.aqua.api.service.ops.LogPartitionMaintenanceService
import dev.yidafu.aqua.api.service.ops.LogPartitionMutationService
import dev.yidafu.aqua.api.service.ops.LogPartitionQueryService
import dev.yidafu.aqua.common.domain.model.LogBackupRecordModel
import dev.yidafu.aqua.common.domain.model.SystemSettingKeys
import dev.yidafu.aqua.common.domain.model.SystemSettingsModel
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingGroup
import dev.yidafu.aqua.common.domain.model.enums.SystemSettingType
import dev.yidafu.aqua.logging.partition.domain.model.LogPartitionInfo
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogBackupStatus
import dev.yidafu.aqua.logging.partition.domain.model.enums.LogTableType
import dev.yidafu.aqua.ops.systemconfig.domain.repository.SystemSettingsRepository
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
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
import java.time.LocalDateTime

/**
 * Admin 数据维护 Resolver GraphQL 集成测试(F-SYS-006, AC-SYS-006-01~08)
 * 权限:查询 PERMISSION_SYSTEM_CONFIG_READ,备份 PERMISSION_SYSTEM_CONFIG_WRITE(仅超级管理员,R-SYS-03)。
 * 日志分区/备份服务为 PostgreSQL 专属,测试环境(H2)经 @TestConfiguration @Primary mock 替换;
 * 保留策略参数经仓库播种(生产由 Liquibase v1.2.4 提供)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/data-maintenance/"])
@Import(DataMaintenanceTestConfiguration::class)
@ApplyExtension(SpringExtension::class)
class AdminDataMaintenanceResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  systemSettingsRepository: SystemSettingsRepository,
  logPartitionQueryService: LogPartitionQueryService,
  logPartitionMutationService: LogPartitionMutationService,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    val partition =
      LogPartitionInfo(
        logTable = LogTableType.API_LOG,
        partitionName = "api_logs_2026_01",
        partitionMonth = "2026-01",
        rowCount = 5,
        sizeBytes = 1024,
        isDefault = false,
        isCurrentMonth = false,
        fromBound = LocalDateTime.of(2026, 1, 1, 0, 0),
        toBound = LocalDateTime.of(2026, 2, 1, 0, 0),
      )

    val record =
      LogBackupRecordModel(
        id = 1L,
        logTable = LogTableType.API_LOG,
        partitionName = "api_logs_2026_01",
        partitionMonth = "2026-01",
        coldTableName = "log_archive.api_logs_2026",
        rowCount = 5,
        filePath = "/data/log-backup/api_logs_2026_01.csv.gz",
        fileChecksum = "abc123",
        status = LogBackupStatus.SUCCEEDED,
        operatorName = "admin",
        startedAt = LocalDateTime.of(2026, 8, 16, 3, 30),
        finishedAt = LocalDateTime.of(2026, 8, 16, 3, 30, 30),
      )

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
      type: SystemSettingType = SystemSettingType.NUMBER,
      group: SystemSettingGroup = SystemSettingGroup.DATA_MAINTENANCE,
    ) {
      val existing = systemSettingsRepository.findBySettingKey(key)
      if (existing != null) {
        existing.settingValue = value
        systemSettingsRepository.save(existing)
      } else {
        systemSettingsRepository.save(
          SystemSettingsModel(
            settingKey = key,
            settingValue = value,
            settingType = type,
            settingGroup = group,
            defaultValue = value,
          ),
        )
      }
    }

    fun seedSettings() {
      seedSetting(SystemSettingKeys.LOG_RETENTION_API_DAYS, "90")
      seedSetting(SystemSettingKeys.LOG_RETENTION_USER_ACTION_DAYS, "90")
      seedSetting(SystemSettingKeys.LOG_RETENTION_BUSINESS_DAYS, "90")
      seedSetting(SystemSettingKeys.LOG_CLEANUP_ENABLED, "true", SystemSettingType.BOOLEAN)
    }

    beforeTest {
      // 清理跨测试累积的调用记录与桩(mock 为 @Primary 单例 Bean,跨测试复用)
      clearMocks(logPartitionQueryService, logPartitionMutationService)
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
      seedSettings()
      every { logPartitionQueryService.listPartitions(any()) } returns listOf(partition)
      every { logPartitionQueryService.isBackedUp(any()) } returns false
      every { logPartitionQueryService.listBackupRecords(any(), any(), any()) } returns
        PageImpl(listOf(record), PageRequest.of(0, 10), 1)
      every { logPartitionMutationService.backup(any(), any(), any(), any(), any()) } returns record
    }

    test("dataMaintenanceOverview returns settings, partitions and backup records") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_READ"))
      val json =
        postGraphql(
          """
          { dataMaintenanceOverview { settings { settingKey } partitions { partitionName backupable } backupRecords { list { id status coldTableName } pageInfo { total } } } }
          """.trimIndent(),
        )
      withClue("response: $json") {
        json.contains("\"settingKey\" : \"${SystemSettingKeys.LOG_RETENTION_API_DAYS}\"") &&
          json.contains("\"partitionName\" : \"api_logs_2026_01\"") &&
          json.contains("\"backupable\" : true") &&
          json.contains("\"coldTableName\" : \"log_archive.api_logs_2026\"") &&
          json.contains("\"total\" : 1") &&
          !json.contains("\"errors\"") shouldBe true
      }
      verify { logPartitionQueryService.listPartitions(LogTableType.API_LOG) }
    }

    test("backupLogPartition calls service with operator and seeded retention days") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_READ", "PERMISSION_SYSTEM_CONFIG_WRITE"))
      val json =
        postGraphql(
          """
          mutation { backupLogPartition(input: { logTable: API_LOG, partitionName: "api_logs_2026_01" }) { id status coldTableName } }
          """.trimIndent(),
        )
      withClue("response: $json") {
        json.contains("\"status\" : \"SUCCEEDED\"") &&
          json.contains("\"coldTableName\" : \"log_archive.api_logs_2026\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
      verify {
        logPartitionMutationService.backup(
          logTable = LogTableType.API_LOG,
          partitionName = "api_logs_2026_01",
          operatorId = 1L,
          operatorName = "admin",
          retentionDays = 90,
        )
      }
    }

    test("admin without system config read permission denied for overview") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_READ"))
      val json = postGraphql("{ dataMaintenanceOverview { partitions { partitionName } } }")
      json.contains("\"errors\"") shouldBe true
    }

    test("admin without system config write permission denied for backup") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_SYSTEM_CONFIG_READ"))
      val json =
        postGraphql(
          """mutation { backupLogPartition(input: { logTable: API_LOG, partitionName: "api_logs_2026_01" }) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
      verify(exactly = 0) { logPartitionMutationService.backup(any(), any(), any(), any(), any()) }
    }

    test("anonymous access is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ dataMaintenanceOverview { partitions { partitionName } } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })

/**
 * 测试配置:以 mock 替换 PostgreSQL 专属的日志分区服务(测试环境 H2 无法执行分区 SQL)
 */
@TestConfiguration
class DataMaintenanceTestConfiguration {
  @Bean
  @Primary
  fun logPartitionQueryService(): LogPartitionQueryService = mockk(relaxed = true)

  @Bean
  @Primary
  fun logPartitionMutationService(): LogPartitionMutationService = mockk(relaxed = true)

  @Bean
  @Primary
  fun logPartitionMaintenanceService(): LogPartitionMaintenanceService = mockk(relaxed = true)
}
