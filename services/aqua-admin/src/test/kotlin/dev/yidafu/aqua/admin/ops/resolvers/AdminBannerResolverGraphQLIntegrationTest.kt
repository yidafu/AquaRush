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
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.common.domain.model.enums.BannerLinkType
import dev.yidafu.aqua.logging.repository.BusinessLogRepository
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import dev.yidafu.aqua.ops.audit.OpsConfigAuditService
import dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository
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
 * AdminBannerResolver GraphQL 集成测试(F-OPS-001, AC-OPS-001-01~06 主路径)
 * 权限:查询 PERMISSION_OPS_CONFIG_READ,变更 PERMISSION_OPS_CONFIG_WRITE。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/ops/"])
@ApplyExtension(SpringExtension::class)
class AdminBannerResolverGraphQLIntegrationTest(
  context: WebApplicationContext,
  bannerRepository: BannerRepository,
  userActionLogRepository: UserActionLogRepository,
  businessLogRepository: BusinessLogRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun seedBanner(
      title: String,
      sortOrder: Int,
    ): BannerModel =
      BannerModel(
        imageUrl = "http://localhost:9090/api/v1/storage/files/test-$sortOrder.png",
        title = title,
        linkType = BannerLinkType.CUSTOM_PAGE,
        linkTarget = "/pages/home/index",
        sortOrder = sortOrder,
        enabled = true,
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

      // 幂等种子 3 条轮播(按排序号去重,避免共享测试库遗留数据重复)
      if (bannerRepository.findBySortOrder(1) == null) {
        bannerRepository.saveAll(
          listOf(
            seedBanner("种子轮播A", 1),
            seedBanner("种子轮播B", 2),
            seedBanner("种子轮播C", 3),
          ),
        )
      }
    }
    test("banners query returns ordered list") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_READ"))
      val json = postGraphql("{ banners { content { id title sortOrder enabled } totalElements } }")
      json.contains("\"种子轮播A\"") && json.contains("\"banners\"") && !json.contains("\"errors\"") shouldBe true
    }
    test("banners query filters by enabled") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_READ"))
      val json = postGraphql("{ banners(input: {enabled: true}) { content { title enabled } } }")
      json.contains("\"enabled\" : true") && !json.contains("\"enabled\" : false") && !json.contains("\"errors\"") shouldBe true
    }
    test("createBanner creates banner and writes audit") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val title = "新建轮播${System.currentTimeMillis()}"
      val json =
        postGraphql(
          """mutation { createBanner(input: {imageUrl: "http://localhost:9090/api/v1/storage/files/new.png", title: "$title", linkType: CUSTOM_PAGE, linkTarget: "/pages/home/index", sortOrder: 10, enabled: true}) { id title sortOrder enabled } }""",
        )
      json.contains("\"$title\"") && !json.contains("\"errors\"") shouldBe true

      // R-OPS-01 审计:操作人/时间/前后值写入 user_action_logs
      val audits = userActionLogRepository.findByActionType(OpsConfigAuditService.ACTION_BANNER_CREATE)
      audits.isNotEmpty() shouldBe true
      audits.any { it.userId == "1" && it.username == "admin" && it.properties?.contains("\"after\"") == true } shouldBe true

      // F-LOG-003 业务日志:同操作同步写入 business_logs(OPS 域,BizLogger 异步落库需轮询等待)
      var businessLogs = businessLogRepository.findByOperation(OpsConfigAuditService.ACTION_BANNER_CREATE)
      var waited = 0
      while (businessLogs.isEmpty() && waited < 50) {
        Thread.sleep(100)
        waited++
        businessLogs = businessLogRepository.findByOperation(OpsConfigAuditService.ACTION_BANNER_CREATE)
      }
      businessLogs.isNotEmpty() shouldBe true
      businessLogs.any { it.loggerName == "OPS" && it.userId == 1L && it.username == "admin" } shouldBe true
    }
    test("createBanner rejects invalid time window") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { createBanner(input: {imageUrl: "http://x/img.png", title: "非法时间窗", linkType: CUSTOM_PAGE, linkTarget: "/pages/home/index", startAt: "2026-08-12T12:00:00", endAt: "2026-08-12T10:00:00"}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("createBanner rejects duplicate sort order") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { createBanner(input: {imageUrl: "http://x/img.png", title: "重复排序${System.currentTimeMillis()}", linkType: CUSTOM_PAGE, linkTarget: "/pages/home/index", sortOrder: 1}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("createBanner rejects nonexistent product link") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val json =
        postGraphql(
          """mutation { createBanner(input: {imageUrl: "http://x/img.png", title: "无效商品跳转", linkType: PRODUCT, linkTarget: "999999999"}) { id } }""",
        )
      json.contains("\"errors\"") shouldBe true
    }
    test("reorderBanner swaps sort order") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val bannerB = bannerRepository.findBySortOrder(2)!!
      val bannerA = bannerRepository.findBySortOrder(1)!!
      val json = postGraphql("mutation { reorderBanner(id: ${bannerB.id}, direction: UP) { id title sortOrder } }")
      json.contains("\"${bannerB.title}\"") && !json.contains("\"errors\"") shouldBe true
      (bannerRepository.findBySortOrder(2)?.id == bannerA.id) shouldBe true
      (bannerRepository.findBySortOrder(1)?.id == bannerB.id) shouldBe true
    }
    test("deleteBanner removes banner") {
      setMockUserPrincipal(authorities = listOf("PERMISSION_OPS_CONFIG_WRITE"))
      val banner = bannerRepository.findBySortOrder(3)!!
      val json = postGraphql("mutation { deleteBanner(id: ${banner.id}) }")
      json.contains("\"deleteBanner\" : true") && !json.contains("\"errors\"") shouldBe true
      val listJson = postGraphql("{ banners { content { title } } }")
      !listJson.contains(banner.title) shouldBe true
    }
    test("anonymous access is denied") {
      mockMvc
        .perform(
          post("/graphql")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"query": "{ banners { content { id } } }"}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
