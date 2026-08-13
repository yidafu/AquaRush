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
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeRepository
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
 * AdminNoticeResolver GraphQL 集成测试(F-OPS-002, AC-OPS-002-01~06 主路径)
 * 权限:查询 PERMISSION_OPS_CONFIG_READ,变更 PERMISSION_OPS_CONFIG_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/ops/"])
@DisplayName("AdminNoticeResolver GraphQL Integration Tests")
class AdminNoticeResolverGraphQLIntegrationTest {
  @Autowired
  private lateinit var context: WebApplicationContext

  @Autowired
  private lateinit var noticeRepository: NoticeRepository

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

    // 幂等种子公告(按标题去重,避免共享 H2 遗留数据重复)
    if (noticeRepository.findAll().none { it.title == "种子公告A" }) {
      noticeRepository.saveAll(
        listOf(
          seedNotice("种子公告A", NoticeType.PLATFORM_NOTICE, NoticeStatus.PUBLISHED, pinned = true),
          seedNotice("种子公告B", NoticeType.WATER_OUTAGE, NoticeStatus.PUBLISHED, pinned = false),
          seedNotice("种子公告C", NoticeType.ACTIVITY_PREVIEW, NoticeStatus.DRAFT, pinned = false),
        ),
      )
    }
  }

  private fun seedNotice(
    title: String,
    type: NoticeType,
    status: NoticeStatus,
    pinned: Boolean,
  ): NoticeModel =
    NoticeModel(
      title = title,
      content = "种子公告内容",
      type = type,
      pinned = pinned,
      status = status,
      publishedAt = if (status == NoticeStatus.PUBLISHED) java.time.LocalDateTime.now() else null,
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
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_READ"])
  @DisplayName("notices query should return notice list with pinned first")
  fun `notices query returns list with pinned first`() {
    val json = postGraphql("{ notices { content { title pinned status } totalElements } }")
    assertTrue(json.contains("\"种子公告A\"") && !json.contains("\"errors\""))
    // 置顶公告排最前(AC-OPS-002-02)
    assertTrue(json.indexOf("\"种子公告A\"") < json.indexOf("\"种子公告B\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("createNotice then publish then stop lifecycle")
  fun `notice lifecycle create publish stop`() {
    val title = "生命周期公告${System.currentTimeMillis()}"
    var json =
      postGraphql(
        """mutation { createNotice(input: {title: "$title", content: "内容", type: PLATFORM_NOTICE}) { id status pinned } }""",
      )
    assertTrue(json.contains("\"DRAFT\"") && !json.contains("\"errors\""))
    val noticeId = Regex("\"id\"\\s*:\\s*(\\d+)").find(json)!!.groupValues[1]

    json = postGraphql("mutation { publishNotice(id: $noticeId) { id status publishedAt } }")
    assertTrue(json.contains("\"PUBLISHED\"") && !json.contains("\"errors\""))
    assertTrue(json.contains("publishedAt"))

    json = postGraphql("mutation { stopNotice(id: $noticeId) { id status pinned } }")
    assertTrue(json.contains("\"STOPPED\"") && !json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("publishNotice on STOPPED notice should fail")
  fun `publish stopped notice fails`() {
    val stopped =
      noticeRepository.save(seedNotice("停用公告${System.currentTimeMillis()}", NoticeType.PLATFORM_NOTICE, NoticeStatus.STOPPED, pinned = false))
    val json = postGraphql("mutation { publishNotice(id: ${stopped.id}) { id } }")
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("setNoticePinned on DRAFT notice should fail")
  fun `pin draft notice fails`() {
    val draft = noticeRepository.save(seedNotice("草稿公告${System.currentTimeMillis()}", NoticeType.PLATFORM_NOTICE, NoticeStatus.DRAFT, pinned = false))
    val json = postGraphql("mutation { setNoticePinned(id: ${draft.id}, pinned: true) { id } }")
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("createNotice with invalid time window should fail")
  fun `createNotice rejects invalid time window`() {
    val json =
      postGraphql(
        """mutation { createNotice(input: {title: "非法时间窗", content: "内容", type: PLATFORM_NOTICE, startAt: "2026-08-12T12:00:00", endAt: "2026-08-12T10:00:00"}) { id } }""",
      )
    assertTrue(json.contains("\"errors\""))
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("createNotice should write audit log")
  fun `createNotice writes audit`() {
    val title = "审计公告${System.currentTimeMillis()}"
    val json =
      postGraphql(
        """mutation { createNotice(input: {title: "$title", content: "内容", type: PLATFORM_NOTICE}) { id } }""",
      )
    assertTrue(!json.contains("\"errors\""))

    val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_NOTICE_CREATE)
    assertTrue(audits.isNotEmpty())
    assertTrue(audits.any { it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true })
  }

  @Test
  @WithMockUserPrincipal(authorities = ["PERMISSION_OPS_CONFIG_WRITE"])
  @DisplayName("deleteNotice should remove notice from list")
  fun `deleteNotice removes notice`() {
    val notice = noticeRepository.save(seedNotice("待删公告${System.currentTimeMillis()}", NoticeType.PLATFORM_NOTICE, NoticeStatus.PUBLISHED, pinned = false))
    val json = postGraphql("mutation { deleteNotice(id: ${notice.id}) }")
    assertTrue(json.contains("\"deleteNotice\" : true") && !json.contains("\"errors\""))
    val listJson = postGraphql("{ notices { content { title } } }")
    assertTrue(!listJson.contains(notice.title))
  }

  @Test
  @DisplayName("anonymous should be denied")
  fun `anonymous access is denied`() {
    mockMvc
      .perform(
        post("/graphql")
          .contentType(MediaType.APPLICATION_JSON)
          .content("""{"query": "{ notices { content { id } } }"}"""),
      ).andExpect(status().isUnauthorized)
  }
}
