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
import dev.yidafu.aqua.common.domain.model.DailyReconciliationModel
import dev.yidafu.aqua.common.domain.model.enums.DailyReconciliationStatus
import dev.yidafu.aqua.delivery.domain.repository.DailyReconciliationRepository
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
 * 每日对账查询 GraphQL 集成测试(收银对账页,PRD 08-statistics.md)
 *
 * 回归用例:PRD 规则要求页面单请求取数,而 delivery-app / admin-client 均以无参形式
 * 调用 `dailyReconciliations`。此前 resolver 声明了 schema 中不存在的
 * `@Argument page: Int = 0, size: Int = 20`,Spring GraphQL 会为未声明参数传入 null,
 * 反装箱抛 NPE → INTERNAL_ERROR。本测试以无参查询锁定该回归。
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

    test("dailyReconciliations without args returns records (regression: INTERNAL_ERROR)") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_RECONCILIATION_READ"))
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
          "{ dailyReconciliations { id reconciliationDate totalOrderCount status reportData } }",
        )
      withClue("dailyReconciliations should return seeded record without error: $json") {
        json.contains("\"reconciliationDate\" : \"2026-08-15\"") &&
          json.contains("\"totalOrderCount\" : 3") &&
          json.contains("\"status\" : \"MATCHED\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("dailyReconciliations requires RECONCILIATION_READ permission") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_ORDER_READ"))
      val json = postGraphql("{ dailyReconciliations { id } }")
      withClue("dailyReconciliations should be denied without permission: $json") {
        json.contains("\"errors\"") || !json.contains("\"dailyReconciliations\"") shouldBe true
      }
    }

    test("dailyCollections returns records") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_RECONCILIATION_READ"))
      val json = postGraphql("{ dailyCollections { id collectionDate } }")
      withClue("dailyCollections should not error: $json") {
        !json.contains("\"errors\"") shouldBe true
      }
    }
  })
