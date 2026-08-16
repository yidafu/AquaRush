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

package dev.yidafu.aqua.admin.logging.resolvers

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUserPrincipal
import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import dev.yidafu.aqua.logging.repository.BusinessLogRepository
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
import java.time.LocalDateTime

/**
 * AdminBusinessLog Resolver GraphQL 集成测试(F-LOG-003,AC-LOG-003-xx)
 * 权限:PERMISSION_LOG_READ。恒 createdAt 倒序;支持 domain/operationType 维度筛选。
 * 测试环境 Liquibase 禁用(H2 + create-drop),@BeforeEach 经仓库播种 business_logs 行。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/logging/"])
@ApplyExtension(SpringExtension::class)
class AdminBusinessLogResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  businessLogRepository: BusinessLogRepository,
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

    fun seedLogs() {
      val now = LocalDateTime.now()
      val seeds =
        listOf(
          BusinessLogModel(
            correlationId = "seed-log-001",
            level = "INFO",
            loggerName = "ORDER",
            operation = "ORDER_CREATED",
            message = "Operation: ORDER_CREATED, Module: ORDER, Result: SUCCESS, Target: 1001",
            userId = 1,
            username = "u1",
            createdAt = now.minusHours(4),
          ),
          BusinessLogModel(
            correlationId = "seed-log-002",
            level = "ERROR",
            loggerName = "ORDER",
            operation = "ORDER_PAID",
            message = "Operation: ORDER_PAID, Module: ORDER, Result: FAILURE, Target: 1002",
            userId = 2,
            username = "u2",
            createdAt = now.minusHours(3),
          ),
          BusinessLogModel(
            correlationId = "seed-log-003",
            level = "INFO",
            loggerName = "PAYMENT",
            operation = "PAYMENT_CREATED",
            message = "Operation: PAYMENT_CREATED, Module: PAYMENT, Result: SUCCESS, Target: 2001",
            userId = 1,
            username = "u1",
            createdAt = now.minusHours(2),
          ),
          BusinessLogModel(
            correlationId = "seed-log-004",
            level = "ERROR",
            loggerName = "AUTH",
            operation = "USER_LOGIN",
            message = "Operation: USER_LOGIN, Module: AUTH, Result: FAILURE, Target: 3001",
            userId = 2,
            username = "u2",
            createdAt = now.minusHours(1),
          ),
          // 老数据:operation 为 NULL(未结构化落库,筛选 operationType 时应被排除)
          BusinessLogModel(
            correlationId = "seed-log-005",
            level = "INFO",
            loggerName = "DELIVERY",
            operation = null,
            message = "Operation: DELIVERY_ASSIGNED, Module: DELIVERY, Result: SUCCESS, Target: 4001",
            userId = 3,
            username = "u3",
            createdAt = now,
          ),
        )
      seeds.forEach { seed ->
        val correlationId = seed.correlationId ?: return@forEach
        if (businessLogRepository.findByCorrelationId(correlationId).isEmpty()) {
          businessLogRepository.save(seed)
        }
      }
    }

    /**
     * 播种业务日志(correlationId 固定,重复执行时跳过,保持幂等)。
     * 5 条:倒序基线(now/now-1h/now-2h/now-3h/now-4h)、三种域(ORDER/PAYMENT/AUTH)、
     * 两级(INFO/ERROR)、两种 operation(ORDER_CREATED/ORDER_PAID)、一条老数据 operation=NULL。
     */

    beforeTest {
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
      seedLogs()
    }
    test("businessLogs returns all rows desc by createdAt") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_LOG_READ"))
      val json =
        postGraphql(
          "{ businessLogs { list { id correlationId level operation loggerName userId username createdAt } pageInfo { total pageNum pageSize } } }",
        )
      withClue("response: $json") { !json.contains("\"errors\"") shouldBe true }
      withClue("total: $json") { json.contains("\"total\" : 5") shouldBe true }
      // 恒 createdAt 倒序:最新 seed-log-005 在首位
      val firstIndex = json.indexOf("seed-log-005")
      val lastIndex = json.indexOf("seed-log-001")
      withClue("order by createdAt desc: $json") { (firstIndex != -1 && lastIndex != -1 && firstIndex < lastIndex) shouldBe true }
    }
    test("domain filter maps to loggerName") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_LOG_READ"))
      val json = postGraphql("{ businessLogs(input: {domain: ORDER}) { list { loggerName operation } pageInfo { total } } }")
      withClue("response: $json") { !json.contains("\"errors\"") shouldBe true }
      withClue("domain=ORDER total: $json") { json.contains("\"total\" : 2") shouldBe true }
      withClue("only ORDER rows: $json") { !json.contains("PAYMENT") && !json.contains("AUTH") shouldBe true }
    }
    test("operationType filter excludes NULL legacy rows") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_LOG_READ"))
      val json = postGraphql("""{ businessLogs(input: {operationType: "ORDER_CREATED"}) { list { operation } pageInfo { total } } }""")
      withClue("response: $json") { !json.contains("\"errors\"") shouldBe true }
      withClue("operationType=ORDER_CREATED total: $json") { json.contains("\"total\" : 1") shouldBe true }
      withClue("contains ORDER_CREATED: $json") { json.contains("ORDER_CREATED") shouldBe true }
      withClue("legacy NULL operation excluded: $json") { !json.contains("DELIVERY_ASSIGNED") shouldBe true }
    }
    test("level and userId combined filter") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_LOG_READ"))
      val json = postGraphql("{ businessLogs(input: {level: ERROR, userId: 2}) { list { level userId } pageInfo { total } } }")
      withClue("response: $json") { !json.contains("\"errors\"") shouldBe true }
      withClue("ERROR+userId=2 total: $json") { json.contains("\"total\" : 2") shouldBe true }
      withClue("no INFO rows: $json") { !json.contains("\"INFO\"") shouldBe true }
    }
    test("time range filter respects boundaries") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_LOG_READ"))
      val now = LocalDateTime.now()
      val json =
        postGraphql(
          """{ businessLogs(input: {startTime: "${now.minusHours(
            3,
          ).withNano(0)}", endTime: "${now.minusHours(1).withNano(0)}"}) { list { correlationId } pageInfo { total } } }""",
        )
      withClue("response: $json") { !json.contains("\"errors\"") shouldBe true }
      // [-3h, -1h] 区间含 seed-log-002(-3h)与 seed-log-003(-2h),排除 seed-log-004(-1h)与 seed-log-005(now)
      withClue("time-range total: $json") { json.contains("\"total\" : 2") shouldBe true }
      withClue("latest excluded: $json") { !json.contains("seed-log-005") shouldBe true }
    }
    test("pagination with size and page") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_LOG_READ"))
      val json =
        postGraphql(
          "{ businessLogs(input: {size: 2, page: 1}) { list { correlationId } pageInfo { total pageNum pageSize hasNext hasPrevious totalPages } } }",
        )
      withClue("response: $json") { !json.contains("\"errors\"") shouldBe true }
      withClue("pageInfo: $json") {
        json.contains("\"pageNum\" : 1") &&
          json.contains("\"pageSize\" : 2") &&
          json.contains("\"totalPages\" : 3") shouldBe true
      }
      withClue("flags: $json") { json.contains("\"hasNext\" : true") && json.contains("\"hasPrevious\" : true") shouldBe true }
      // 倒序 [005(now), 004(-1h), 003(-2h), 002(-3h), 001(-4h)],第 2 页(0 基 page=1)为 seed-log-003 与 seed-log-002
      withClue("page 2 content: $json") { json.contains("seed-log-003") && json.contains("seed-log-002") shouldBe true }
      withClue("page 2 excludes edges: $json") { !json.contains("seed-log-005") && !json.contains("seed-log-001") shouldBe true }
    }
    test("default size is 10") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_LOG_READ"))
      val json = postGraphql("{ businessLogs { pageInfo { pageSize } } }")
      withClue("default size: $json") { json.contains("\"pageSize\" : 10") shouldBe true }
    }
    test("anonymous rejected") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ businessLogs { list { id } } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
    test("missing permission rejected") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_READ"))
      val json =
        postGraphql("{ businessLogs { list { id } } }")
      withClue("should contain errors: $json") { json.contains("\"errors\"") shouldBe true }
      withClue("access denied marker: $json") { json.contains("FORBIDDEN") || json.contains("Forbidden") shouldBe true }
    }
  })
