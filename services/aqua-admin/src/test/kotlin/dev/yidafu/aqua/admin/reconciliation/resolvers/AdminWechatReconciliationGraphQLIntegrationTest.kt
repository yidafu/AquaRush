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

package dev.yidafu.aqua.admin.reconciliation.resolvers

import com.ninjasquad.springmockk.MockkBean
import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.analytics.reconciliation.domain.repository.ReconciliationDiscrepancyRepository
import dev.yidafu.aqua.analytics.reconciliation.domain.repository.ReconciliationReportRepository
import dev.yidafu.aqua.analytics.reconciliation.domain.repository.ReconciliationTaskRepository
import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.ReconciliationDiscrepancyModel
import dev.yidafu.aqua.common.domain.model.ReconciliationReportModel
import dev.yidafu.aqua.common.domain.model.ReconciliationTaskModel
import dev.yidafu.aqua.common.domain.model.enums.DiscrepancyStatus
import dev.yidafu.aqua.common.domain.model.enums.ReconciliationTaskStatus
import dev.yidafu.aqua.common.domain.model.enums.SourceSystem
import dev.yidafu.aqua.common.messaging.service.SimplifiedEventPublishService
import dev.yidafu.aqua.payment.service.WechatBillGateway
import dev.yidafu.aqua.payment.service.WechatBillType
import dev.yidafu.aqua.payment.service.WechatFundFlowBill
import dev.yidafu.aqua.payment.service.WechatFundFlowSummary
import dev.yidafu.aqua.payment.service.WechatTradeBill
import dev.yidafu.aqua.payment.service.WechatTradeBillSummary
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
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

/**
 * 微信资金对账 GraphQL 集成测试(后台「支付管理-支付对账」页):
 * 对账任务/差异/报表查询、手动执行对账(executeReconciliation)、差异处置(解决/忽略 resolveDiscrepancy)。
 *
 * 权限:查询 PERMISSION_PAYMENT_RECONCILIATION_READ、执行与差异处置 PERMISSION_PAYMENT_RECONCILIATION_EXECUTE
 * (F-ORD-031/G93 独立权限码,见 spec/prd/01-overview.md §权限码目录)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(
  properties = [
    "spring.graphql.schema.locations=classpath:graphql/reconciliation/",
    // R-PAY-04 SKIPPED 分支门槛由 WechatBillGateway.isConfigured 决定(wechat.pay.*),本测试经 @MockkBean stub
  ],
)
@ApplyExtension(SpringExtension::class)
class AdminWechatReconciliationGraphQLIntegrationTest(
  context: WebApplicationContext,
  private val taskRepository: ReconciliationTaskRepository,
  private val discrepancyRepository: ReconciliationDiscrepancyRepository,
  private val reportRepository: ReconciliationReportRepository,
) : FunSpec() {
  @MockkBean
  private lateinit var billGateway: WechatBillGateway

  @MockkBean
  private lateinit var eventPublishService: SimplifiedEventPublishService

  @MockkBean
  private lateinit var adminService: AdminService

  init {
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

    /** 等待异步对账任务到达终态(SUCCESS/FAILED) */
    fun awaitTaskTerminal(
      taskId: String,
      timeoutMillis: Long = 10000,
    ): ReconciliationTaskModel {
      val deadline = System.currentTimeMillis() + timeoutMillis
      while (System.currentTimeMillis() < deadline) {
        val task = taskRepository.findByTaskId(taskId)
        if (task != null && task.status in listOf(ReconciliationTaskStatus.SUCCESS, ReconciliationTaskStatus.FAILED)) {
          return task
        }
        Thread.sleep(100)
      }
      throw AssertionError("对账任务 $taskId 在 ${timeoutMillis}ms 内未到达终态")
    }

    beforeTest {
      // 测试间隔离:清空对账任务/差异/报表,避免上个用例残留的 PENDING 任务触发
      // hasRunningTask() 拦截(executeReconciliation 依赖无运行中任务的干净状态)
      reportRepository.deleteAll()
      discrepancyRepository.deleteAll()
      taskRepository.deleteAll()

      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
      // 已配置商户参数(否则任务置 SKIPPED);微信账单默认返回空(测试环境无真实商户数据)
      every { billGateway.isConfigured } returns true
      every { billGateway.downloadTradeBill(any(), any()) } returns
        WechatTradeBill(LocalDate.of(2025, 6, 1), WechatBillType.ALL, emptyList(), WechatTradeBillSummary(0, emptyMap()))
      every { billGateway.downloadFundFlowBill(any()) } returns
        WechatFundFlowBill(LocalDate.of(2025, 6, 1), emptyList(), WechatFundFlowSummary(0, 0, 0, 0, 0))
      // 异步对账流程会发布领域事件(RECONCILIATION_STARTED/COMPLETED/FAILED),mock 需有默认返回值
      every { eventPublishService.publishDomainEvent(any(), any(), any()) } returns true
      every { adminService.findByUsername(any()) } returns
        AdminModel(username = "admin", passwordHash = "x").apply { id = 1L }
    }

    test("reconciliationTasks returns paged tasks with filters") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_RECONCILIATION_READ"))
      val date = LocalDate.of(2026, 8, 20)
      taskRepository.save(ReconciliationTaskModel.createPaymentTask(date.atStartOfDay()))
      taskRepository.save(ReconciliationTaskModel.createRefundTask(date.atStartOfDay()))

      val json =
        postGraphql(
          "{ reconciliationTasks(taskType: PAYMENT, page: 0, size: 10) { content { taskId taskType status taskDate totalRecords } totalElements } }",
        )
      withClue("reconciliationTasks should return filtered tasks: $json") {
        json.contains("\"taskType\" : \"PAYMENT\"") &&
          json.contains("\"totalElements\" : 1") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("reconciliationTasks requires PAYMENT_RECONCILIATION_READ permission") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_ORDER_READ"))
      val json = postGraphql("{ reconciliationTasks { content { taskId } totalElements } }")
      withClue("reconciliationTasks should be denied without permission: $json") {
        json.contains("\"errors\"") || !json.contains("\"reconciliationTasks\"") shouldBe true
      }
    }

    test("reconciliationTask returns task detail by taskId") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_RECONCILIATION_READ"))
      val date = LocalDate.of(2026, 8, 20)
      val task = taskRepository.save(ReconciliationTaskModel.createPaymentTask(date.atStartOfDay()))

      val json =
        postGraphql(
          "{ reconciliationTask(taskId: \"${task.taskId}\") { taskId taskType status totalRecords } }",
        )
      withClue("reconciliationTask should return the task: $json") {
        json.contains("\"taskId\" : \"${task.taskId}\"") &&
          json.contains("\"taskType\" : \"PAYMENT\"") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("reconciliationDiscrepancies returns paged discrepancies with status filter") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_RECONCILIATION_READ"))
      val task = taskRepository.save(ReconciliationTaskModel.createPaymentTask(LocalDate.of(2026, 8, 20).atStartOfDay()))
      discrepancyRepository.save(
        ReconciliationDiscrepancyModel.createMissingRecord(
          taskId = task.taskId,
          sourceSystem = SourceSystem.WECHAT,
          recordId = "wx-rec-1",
          recordDetails = mapOf("amountCents" to 1000L),
        ),
      )

      val json =
        postGraphql(
          "{ reconciliationDiscrepancies(taskId: \"${task.taskId}\", status: UNRESOLVED) { content { id recordId discrepancyType sourceSystem status } totalElements } }",
        )
      withClue("reconciliationDiscrepancies should return the discrepancy: $json") {
        json.contains("\"recordId\" : \"wx-rec-1\"") &&
          json.contains("\"discrepancyType\" : \"MISSING\"") &&
          json.contains("\"totalElements\" : 1") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("reconciliationReports returns reports by task") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_RECONCILIATION_READ"))
      val task = taskRepository.save(ReconciliationTaskModel.createPaymentTask(LocalDate.of(2026, 8, 20).atStartOfDay()))
      reportRepository.save(
        ReconciliationReportModel.createSummaryReport(
          taskId = task.taskId,
          reportData = mapOf("taskType" to "PAYMENT", "totalRecords" to 3),
        ),
      )

      val json =
        postGraphql(
          "{ reconciliationReports(taskId: \"${task.taskId}\") { reportType reportData generatedAt } }",
        )
      withClue("reconciliationReports should return the summary report: $json") {
        json.contains("\"reportType\" : \"SUMMARY\"") &&
          json.contains("\"totalRecords\" : 3") &&
          !json.contains("\"errors\"") shouldBe true
      }
    }

    test("executeReconciliation creates a payment task and executes it to SUCCESS") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_RECONCILIATION_EXECUTE", "PERMISSION_PAYMENT_RECONCILIATION_READ"))
      // 账单日窗口 [T-30, T-1](R-PAY-04):固定历史日期会被窗口校验拒绝,取 T-1
      val billDate = LocalDate.now().minusDays(1)
      val json =
        postGraphql(
          "mutation { executeReconciliation(input: { taskType: PAYMENT, reconciliationDate: \"$billDate\" }) { taskId taskType status triggerType } }",
        )
      withClue("executeReconciliation should return the created task: $json") {
        json.contains("\"taskType\" : \"PAYMENT\"") && !json.contains("\"errors\"") shouldBe true
      }
      val taskId =
        Regex("\"taskId\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
          ?: throw AssertionError("无法从响应中解析 taskId: $json")

      val terminal = awaitTaskTerminal(taskId)
      withClue("支付对账(无差异)应执行成功, 实际状态=${terminal.status}, error=${terminal.errorMessage}") {
        terminal.status shouldBe ReconciliationTaskStatus.SUCCESS
      }
      terminal.totalRecords shouldBe 0
      terminal.unmatchedRecords shouldBe 0
    }

    test("resolveDiscrepancy marks the discrepancy as resolved") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_RECONCILIATION_EXECUTE", "PERMISSION_PAYMENT_RECONCILIATION_READ"))
      val task = taskRepository.save(ReconciliationTaskModel.createPaymentTask(LocalDate.of(2026, 8, 20).atStartOfDay()))
      val discrepancy =
        discrepancyRepository.save(
          ReconciliationDiscrepancyModel.createMismatchRecord(
            taskId = task.taskId,
            sourceSystem = SourceSystem.INTERNAL,
            recordId = "wx-rec-mismatch",
            recordDetails = mapOf("amountDifferenceCents" to 50L),
          ),
        )

      val json =
        postGraphql(
          "mutation { resolveDiscrepancy(input: { discrepancyId: ${discrepancy.id}, action: RESOLVE, resolutionNotes: \"人工核对无误\" }) }",
        )
      withClue("resolveDiscrepancy should return true: $json") {
        json.contains("\"resolveDiscrepancy\" : true") && !json.contains("\"errors\"") shouldBe true
      }

      val resolved =
        discrepancyRepository.findById(discrepancy.id!!).orElse(null)
          ?: throw AssertionError("差异记录不存在")
      resolved.status shouldBe DiscrepancyStatus.RESOLVED
      resolved.resolutionNotes shouldBe "人工核对无误"
      resolved.resolvedBy shouldBe "admin"
      resolved.resolvedAt.shouldNotBeNull()
    }

    test("resolveDiscrepancy requires PAYMENT_RECONCILIATION_EXECUTE permission") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_PAYMENT_RECONCILIATION_READ"))
      val task = taskRepository.save(ReconciliationTaskModel.createPaymentTask(LocalDate.of(2026, 8, 20).atStartOfDay()))
      val discrepancy =
        discrepancyRepository.save(
          ReconciliationDiscrepancyModel.createMissingRecord(
            taskId = task.taskId,
            sourceSystem = SourceSystem.WECHAT,
            recordId = "wx-rec-2",
            recordDetails = emptyMap(),
          ),
        )

      val json =
        postGraphql(
          "mutation { resolveDiscrepancy(input: { discrepancyId: ${discrepancy.id}, action: IGNORE, resolutionNotes: \"无权限\" }) }",
        )
      withClue("resolveDiscrepancy should be denied without EXECUTE permission: $json") {
        json.contains("\"errors\"") || !json.contains("\"resolveDiscrepancy\"") shouldBe true
      }
    }
  }
}
