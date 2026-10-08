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

package dev.yidafu.aqua.admin.statistics.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DailyCollectionRecordModel
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.domain.model.enums.DailyCollectionStatus
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.delivery.domain.repository.DailyCollectionRecordRepository
import dev.yidafu.aqua.delivery.domain.repository.DailyReconciliationRepository
import dev.yidafu.aqua.user.domain.repository.AdminRepository
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
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 每日对账查询 GraphQL 集成测试(收银对账页,PRD 06-collection.md)
 *
 * 回归用例:PRD 规则要求页面单请求取数,而 delivery-app / admin-client 均以无参形式
 * 调用 `dailyReconciliations`。分页改造后 schema 显式声明 `input: DailyReconciliationListInput`
 * (含默认 page/size),无参调用必须正常返回第一页(此前 resolver 声明了 schema 中不存在的
 * `@Argument page: Int = 0, size: Int = 20`,Spring GraphQL 会为未声明参数传入 null,
 * 反装箱抛 NPE → INTERNAL_ERROR)。本测试以无参查询锁定该回归。
 *
 * 权限:PERMISSION_RECONCILIATION_READ。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/reconciliation/"])
@ApplyExtension(SpringExtension::class)
class AdminDailyCollectionGraphQLIntegrationTest(
  context: WebApplicationContext,
  reconciliationRepository: DailyReconciliationRepository,
  collectionRecordRepository: DailyCollectionRecordRepository,
  adminRepository: AdminRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun postGraphql(query: String): String {
      // 转义为合法 JSON 字符串:反斜杠/引号/换行等控制字符(支持多行 GraphQL 查询体)
      val escaped =
        query
          .replace("\\", "\\\\")
          .replace("\"", "\\\"")
          .replace("\r", "\\r")
          .replace("\n", "\\n")
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

    test("dailyReconciliations without args returns first page (regression: INTERNAL_ERROR)") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_RECONCILIATION_READ"))
      reconciliationRepository.deleteAll()
      val date = LocalDate.of(2026, 8, 15)
      val model =
        DailyReconciliationModel(
          reconciliationDate = date,
          totalCashAmountCents = 1000L,
          totalWaterTicketCount = 2,
          totalQrCodeAmountCents = 500L,
          totalOrderCount = 3,
          totalOrderAmountCents = 1500L,
          totalCollectionAmountCents = 1500L,
          discrepancyAmountCents = 0L,
          status = DailyReconciliationStatus.MATCHED,
          reportData = mapOf("totalWorkers" to 1),
          createdAt = LocalDateTime.of(2026, 8, 15, 10, 0),
        )
      reconciliationRepository.save(model)

      val json =
        postGraphql(
          "{ dailyReconciliations { list { id reconciliationDate totalOrderCount status reportData } pageInfo { total pageNum pageSize } } }",
        )
      withClue("dailyReconciliations should return seeded record without error: $json") {
        json.contains("\"reconciliationDate\" : \"2026-08-15\"") &&
          json.contains("\"totalOrderCount\" : 3") &&
          json.contains("\"status\" : \"MATCHED\"") &&
          json.contains("\"total\" : 1") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("dailyReconciliations paginates with size 1") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_RECONCILIATION_READ"))
      reconciliationRepository.deleteAll()
      reconciliationRepository.save(
        DailyReconciliationModel(
          reconciliationDate = LocalDate.of(2026, 8, 14),
          status = DailyReconciliationStatus.PENDING,
          createdAt = LocalDateTime.of(2026, 8, 14, 10, 0),
        ),
      )
      reconciliationRepository.save(
        DailyReconciliationModel(
          reconciliationDate = LocalDate.of(2026, 8, 13),
          status = DailyReconciliationStatus.PENDING,
          createdAt = LocalDateTime.of(2026, 8, 13, 10, 0),
        ),
      )

      val json =
        postGraphql(
          """{ dailyReconciliations(input: { page: 0, size: 1 }) { list { reconciliationDate } pageInfo { total pageNum hasNext } } }""",
        )
      withClue("size 1 should return single row with hasNext and total 2: $json") {
        json.contains("\"pageNum\" : 0") &&
          json.contains("\"hasNext\" : true") &&
          json.contains("\"total\" : 2") shouldBe true
      }
    }

    test("dailyReconciliations requires RECONCILIATION_READ permission") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_ORDER_READ"))
      val json = postGraphql("{ dailyReconciliations { list { id } } }")
      withClue("dailyReconciliations should be denied without permission: $json") {
        json.contains("\"errors\"") || !json.contains("\"dailyReconciliations\"") shouldBe true
      }
    }

    test("dailyCollections returns paginated records") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_RECONCILIATION_READ"))
      val json = postGraphql("{ dailyCollections { list { id collectionDate } pageInfo { total } } }")
      withClue("dailyCollections should not error: $json") {
        !json.contains("\"errors\"") shouldBe true
      }
    }

    test("dailyCollectionSummary returns zero-filled summary") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_RECONCILIATION_READ"))
      val query =
        """
        { dailyCollectionSummary(collectionDate: "2026-08-15") {
            collectionDate totalCashAmountCents totalSubmittedCount
          }
        }
        """.trimIndent()
      val json = postGraphql(query)
      withClue("dailyCollectionSummary should not error: $json") {
        // Long 标量序列化为 JSON 字符串(如 "0"),Int 序列化为数字
        !json.contains("\"errors\"") &&
          json.contains("\"collectionDate\" : \"2026-08-15\"") &&
          json.contains("\"totalCashAmountCents\" : \"0\"") &&
          json.contains("\"totalSubmittedCount\" : 0") shouldBe true
      }
    }

    test("dailyReconciliationOrderDetails returns empty page for date without orders") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_RECONCILIATION_READ"))
      val json =
        postGraphql(
          """{ dailyReconciliationOrderDetails(reconciliationDate: "2026-08-15", page: 0, size: 20) { list { orderId orderNo } pageInfo { total } } }""",
        )
      withClue("dailyReconciliationOrderDetails should not error: $json") {
        !json.contains("\"errors\"") && json.contains("\"total\" : 0") shouldBe true
      }
    }

    test("rejectDailyCollection rejects submitted record to draft with trail") {
      setMockUserPrincipal(
        username = "admin1",
        authorities = listOf("PERMISSION_RECONCILIATION_CONFIRM"),
      )
      adminRepository.deleteAll()
      collectionRecordRepository.deleteAll()
      val admin =
        AdminModel(username = "admin1", passwordHash = "hash", role = AdminRoleModel.ADMIN)
      adminRepository.save(admin)
      val record =
        DailyCollectionRecordModel(
          deliveryWorkerId = admin.id ?: 1L,
          collectionDate = LocalDate.of(2026, 8, 15),
          cashAmountCents = 1000L,
          waterTicketCount = 1,
          qrCodeAmountCents = 500L,
          orderCount = 3,
          orderAmountCents = 1500L,
          differenceAmountCents = 200L,
          status = DailyCollectionStatus.SUBMITTED,
          createdAt = LocalDateTime.of(2026, 8, 15, 10, 0),
        )
      collectionRecordRepository.save(record)
      val recordId = record.id ?: 1L

      val json =
        postGraphql(
          """mutation { rejectDailyCollection(input: { recordId: $recordId, reason: "金额不符" }) { id status rejectedBy rejectReason } }""",
        )
      withClue("rejectDailyCollection should set DRAFT with trail: $json") {
        json.contains("\"status\" : \"DRAFT\"") &&
          json.contains("\"rejectReason\" : \"金额不符\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("rejectDailyCollection requires RECONCILIATION_CONFIRM permission") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_RECONCILIATION_READ"))
      val json =
        postGraphql(
          """mutation { rejectDailyCollection(input: { recordId: 1, reason: "金额不符" }) { id status } }""",
        )
      withClue("rejectDailyCollection should be denied without permission: $json") {
        json.contains("\"errors\"") || !json.contains("\"rejectDailyCollection\"") shouldBe true
      }
    }
  })
