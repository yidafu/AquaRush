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
import dev.yidafu.aqua.common.domain.model.FeedbackModel
import dev.yidafu.aqua.common.domain.model.enums.FeedbackStatus
import dev.yidafu.aqua.common.domain.model.enums.FeedbackType
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.feedback.domain.repository.FeedbackRepository
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
 * AdminFeedbackResolver GraphQL 集成测试(F-OPS-003, AC-OPS-003-01~06 主路径)
 * 权限:查询 PERMISSION_FEEDBACK_READ,处理 PERMISSION_FEEDBACK_REVIEW。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/ops/"])
@DisplayName("AdminFeedbackResolver GraphQL Integration Tests")
class AdminFeedbackResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var feedbackRepository: FeedbackRepository

  @Autowired
  private lateinit var userActionLogRepository: UserActionLogRepository

  private lateinit var mockMvc: MockMvc

  @BeforeEach
  fun setUp() {
    mockMvc =
      MockMvcBuilders
        .webAppContextSetup(context)
        .apply<DefaultMockMvcBuilder>(springSecurity())
        .build()

    // 幂等种子 4 条反馈(不同类型/状态/联系方式)
    // 注意:reviewFeedback 测试会把「水质发黄」(PENDING)流转为 RESOLVED,故 PENDING 状态断言依赖「订单重复扣款」种子
    if (feedbackRepository.count() == 0L) {
      feedbackRepository.saveAll(
        listOf(
          seedFeedback("水质发黄,请尽快处理", FeedbackType.PRODUCT_QUALITY, FeedbackStatus.PENDING, "13800138001"),
          seedFeedback("订单重复扣款问题", FeedbackType.COMPLAINT, FeedbackStatus.PENDING, "13800138004"),
          seedFeedback("配送迟到半小时", FeedbackType.DELIVERY_SERVICE, FeedbackStatus.PROCESSING, "13800138002"),
          seedFeedback("希望增加小程序夜间模式", FeedbackType.SUGGESTION, FeedbackStatus.RESOLVED, "13800138003"),
        ),
      )
    }
  }

  private fun seedFeedback(
    content: String,
    feedbackType: FeedbackType,
    status: FeedbackStatus,
    contactInfo: String,
  ): FeedbackModel =
    FeedbackModel(
      userId = 100L,
      feedbackType = feedbackType,
      content = content,
      contactInfo = contactInfo,
      status = status,
    )

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
  @WithMockUserPrincipal(authorities = ["PERMISSION_FEEDBACK_READ"])
  @DisplayName("feedbacks query should return feedback list")
  fun `feedbacks query returns list`() {
    val json = postGraphql("{ feedbacks { content { id feedbackType status content contactInfo } totalElements } }")
    assertTrue(json.contains("\"feedbacks\"") && json.contains("水质发黄") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_FEEDBACK_READ"])
  @DisplayName("feedbacks query should filter by status")
  fun `feedbacks query filters by status`() {
    val json = postGraphql("{ feedbacks(input: {status: PENDING}) { content { status } } }")
    assertTrue(json.contains("\"status\" : \"PENDING\"") && !json.contains("\"RESOLVED\"") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_FEEDBACK_READ"])
  @DisplayName("feedbacks query should filter by contact keyword")
  fun `feedbacks query filters by contact keyword`() {
    val json = postGraphql("{ feedbacks(input: {contactKeyword: \"13800138002\"}) { content { contactInfo } } }")
    assertTrue(json.contains("13800138002") && !json.contains("13800138001") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_FEEDBACK_READ"])
  @DisplayName("feedback(id) query should return detail")
  fun `feedback query returns detail`() {
    val feedback = feedbackRepository.findFirstByContentContaining("水质发黄")!!
    val json = postGraphql("{ feedback(id: ${feedback.id}) { id content status contactInfo } }")
    assertTrue(json.contains("水质发黄") && json.contains("\"feedback\"") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_FEEDBACK_REVIEW"])
  @DisplayName("reviewFeedback should transition status and record handler")
  fun `reviewFeedback transitions to resolved and records handler`() {
    val feedback = feedbackRepository.findFirstByContentContaining("水质发黄")!!
    val json =
      postGraphql(
        """mutation { reviewFeedback(id: ${feedback.id}, input: {handleResult: "已联系用户上门检测", status: RESOLVED}) { id status handleResult handledByName handledAt } }""",
      )
    assertTrue(json.contains("\"status\" : \"RESOLVED\"") && json.contains("\"handledByName\" : \"admin\"") && !json.contains("\"errors\""))

    // R-OPS-01 审计:处理操作写入 user_action_logs,properties 含前后值
    val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_FEEDBACK_PROCESS)
    assertTrue(audits.isNotEmpty())
    assertTrue(
      audits.any {
        it.target == "feedback:${feedback.id}" &&
          it.properties?.contains("\"before\"") == true &&
          it.properties?.contains("\"after\"") == true
      },
    )
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_FEEDBACK_REVIEW"])
  @DisplayName("reviewFeedback should reject backward status transition")
  fun `reviewFeedback rejects backward transition`() {
    val feedback = feedbackRepository.findFirstByContentContaining("夜间模式")!!
    val json =
      postGraphql(
        """mutation { reviewFeedback(id: ${feedback.id}, input: {status: PENDING}) { id status } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @DisplayName("anonymous should be denied")
  fun `anonymous access is denied`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "{ feedbacks { content { id } } }"}"""),
      ).andExpect(status().isUnauthorized)
  }
}
