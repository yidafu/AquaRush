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
import dev.yidafu.aqua.common.domain.model.NoticeModel
import dev.yidafu.aqua.common.domain.model.enums.NoticeStatus
import dev.yidafu.aqua.common.domain.model.enums.NoticeType
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.notice.domain.repository.NoticeRepository
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
 * AdminNoticeResolver GraphQL 集成测试(F-OPS-002, AC-OPS-002-01~06 主路径)
 * 权限:查询 PERMISSION_OPS_CONFIG_READ,变更 PERMISSION_OPS_CONFIG_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/ops/"])
@ApplyExtension(SpringExtension::class)
class AdminNoticeResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  noticeRepository: NoticeRepository,
  userActionLogRepository: UserActionLogRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun seedNotice(
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
    test("notices query returns list with pinned first") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_READ"))
      val json = postGraphql("{ notices { content { title pinned status } totalElements } }")
      json.contains("\"种子公告A\"") && !json.contains("\"errors\"") shouldBe true
      // 置顶公告排最前(AC-OPS-002-02)
      (json.indexOf("\"种子公告A\"") < json.indexOf("\"种子公告B\"")) shouldBe true
    }
    test("notice lifecycle create publish stop") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val title = "生命周期公告${System.currentTimeMillis()}"
      var json =
        postGraphql(
          """mutation { createNotice(input: {title: "$title", content: "内容", type: PLATFORM_NOTICE}) { id status pinned } }""",
        )
      json.contains("\"DRAFT\"") && !json.contains("\"errors\"") shouldBe true
      // PrimaryId(Long) 在线上序列化为字符串,如 "id" : "7434..."
      val noticeId = Regex("\"id\"\\s*:\\s*\"(\\d+)\"").find(json)!!.groupValues[1]

      json = postGraphql("mutation { publishNotice(id: $noticeId) { id status publishedAt } }")
      json.contains("\"PUBLISHED\"") && !json.contains("\"errors\"") shouldBe true
      json.contains("publishedAt") shouldBe true

      json = postGraphql("mutation { stopNotice(id: $noticeId) { id status pinned } }")
      json.contains("\"STOPPED\"") && !json.contains("\"errors\"") shouldBe true
    }
    test("publish stopped notice fails") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val stopped =
        noticeRepository.save(
          seedNotice("停用公告${System.currentTimeMillis()}", NoticeType.PLATFORM_NOTICE, NoticeStatus.STOPPED, pinned = false),
        )
      val json = postGraphql("mutation { publishNotice(id: ${stopped.id}) { id } }")
      json.contains("\"errors\"") shouldBe true
    }
    test("pin draft notice fails") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val draft =
        noticeRepository.save(
          seedNotice("草稿公告${System.currentTimeMillis()}", NoticeType.PLATFORM_NOTICE, NoticeStatus.DRAFT, pinned = false),
        )
      val json = postGraphql("mutation { setNoticePinned(id: ${draft.id}, pinned: true) { id } }")
      json.contains("\"errors\"") shouldBe true
    }
    test("createNotice rejects invalid time window") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { createNotice(input: {title: "非法时间窗", content: "内容", type: PLATFORM_NOTICE, startAt: "2026-08-12T12:00:00", endAt: "2026-08-12T10:00:00"}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("createNotice writes audit") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val title = "审计公告${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { createNotice(input: {title: "$title", content: "内容", type: PLATFORM_NOTICE}) { id } }""",
        )
      !json.contains("\"errors\"") shouldBe true

      val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_NOTICE_CREATE)
      audits.isNotEmpty() shouldBe true
      audits.any { it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true } shouldBe true
    }
    test("deleteNotice removes notice") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val notice =
        noticeRepository.save(
          seedNotice("待删公告${System.currentTimeMillis()}", NoticeType.PLATFORM_NOTICE, NoticeStatus.PUBLISHED, pinned = false),
        )
      val json = postGraphql("mutation { deleteNotice(id: ${notice.id}) }")
      json.contains("\"deleteNotice\" : true") && !json.contains("\"errors\"") shouldBe true
      val listJson = postGraphql("{ notices { content { title } } }")
      !listJson.contains(notice.title) shouldBe true
    }
    test("anonymous access is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ notices { content { id } } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
