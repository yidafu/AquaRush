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

package dev.yidafu.aqua.admin.logging.controller

import dev.yidafu.aqua.admin.AquaAdminApplication
import dev.yidafu.aqua.admin.testutil.setMockUser
import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import dev.yidafu.aqua.logging.repository.UserActionLogRepository
import io.kotest.assertions.fail
import io.kotest.assertions.withClue
import io.kotest.core.extensions.ApplyExtension
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 用户操作日志控制器集成测试
 *
 * 覆盖:
 * - 批量上报:显式字段落库、旧客户端 properties 回退、敏感键脱敏、properties pretty-print;
 * - 分页查询:actionType/username/target/时间范围筛选;
 * - 权限:查询需 PERMISSION_LOG_READ,上报需登录。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaAdminApplication::class],
)
@ActiveProfiles("test")
// 仅加载日志域自包含测试 schema,避免测试资源下多份 schema 与主 schema 重复定义
@TestPropertySource(properties = ["spring.graphql.schema.locations=classpath:graphql/logging/"])
@ApplyExtension(SpringExtension::class)
class UserActionLogControllerIntegrationTest(
  context: WebApplicationContext,
  userActionLogRepository: UserActionLogRepository,
) : FunSpec({
    var mockMvc: MockMvc = mockk()

    fun awaitCount(
      expected: Long,
      timeoutMs: Long = 5000,
    ) {
      val deadline = System.currentTimeMillis() + timeoutMs
      while (System.currentTimeMillis() < deadline) {
        if (userActionLogRepository.count() >= expected) return
        Thread.sleep(50)
      }
      fail("Timed out waiting for $expected user action logs, got ${userActionLogRepository.count()}")
    }

    /** 等待异步落库完成(超时则失败) */

    beforeTest {
      // Boot 4.0 不再提供 @AutoConfigureMockMvc,手动装配并挂载 springSecurity 过滤器链
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
      userActionLogRepository.deleteAll()
    }
    test("batch report persists fields and sanitizes") {
      setMockUser()
      val body =
        """
        {
          "actions": [
            {
              "userId": "u-1001",
              "username": "alice",
              "actionType": "CLICK",
              "target": "add-to-cart",
              "pageUrl": "/pages/product-detail/index",
              "elementId": "btn-add-cart",
              "elementType": "button",
              "elementText": "加入购物车",
              "properties": {"productId": 1001, "password": "secret123"},
              "timestamp": 1700000000000
            },
            {
              "userId": "u-1001",
              "username": "alice",
              "actionType": "FORM_SUBMIT",
              "target": "order-submit",
              "properties": {"pageUrl": "/pages/order-confirm/index", "fields": ["a", "b"]},
              "timestamp": 1700000001000
            }
          ]
        }
        """.trimIndent()

      mockMvc
        .perform(
          post("/api/logs/user-actions/batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
        ).andExpect(status().isOk)
        .andExpect(jsonPath("$.success").value(true))

      awaitCount(2)

      val click = userActionLogRepository.findAll().first { it.actionType == "CLICK" }
      click.userId shouldBe "u-1001"
      click.target shouldBe "add-to-cart"
      click.pageUrl shouldBe "/pages/product-detail/index"
      click.elementId shouldBe "btn-add-cart"
      click.elementType shouldBe "button"
      click.elementText shouldBe "加入购物车"

      // properties pretty-print(含缩进换行)且敏感键被剔除
      val props = click.properties
      props.shouldNotBeNull()
      withClue("properties should be pretty-printed, got: $props") { props!!.contains("\n  ") shouldBe true }
      withClue("properties should keep safe keys") { props.contains("productId") shouldBe true }
      withClue("sensitive key password should be stripped") { props.contains("password") shouldBe false }
      withClue("sensitive value should be stripped") { props.contains("secret123") shouldBe false }

      // 旧客户端兼容:pageUrl 从 properties 回退提取
      val form = userActionLogRepository.findAll().first { it.actionType == "FORM_SUBMIT" }
      form.pageUrl shouldBe "/pages/order-confirm/index"
    }
    test("query filters and paginates") {
      setMockUser(authorities = listOf("ROLE_ADMIN", "PERMISSION_LOG_READ"))
      userActionLogRepository.save(
        UserActionLogModel(
          userId = "u-1",
          username = "bob",
          actionType = "CLICK",
          target = "add-to-cart",
          pageUrl = "/pages/product-detail/index",
          elementId = "btn-add-cart",
          elementType = "button",
          elementText = "加入购物车",
          clientIp = "127.0.0.1",
          properties = """{"productId": 1001}""",
        ),
      )
      userActionLogRepository.save(
        UserActionLogModel(
          userId = "u-2",
          username = "carol",
          actionType = "SEARCH",
          target = "search",
          pageUrl = "/pages/home/index",
          properties = """{"searchQuery": "农夫山泉"}""",
        ),
      )

      // actionType + username + target 组合筛选
      mockMvc
        .perform(
          get("/api/logs/user-actions")
            .param("actionType", "CLICK")
            .param("username", "bob")
            .param("target", "add-to-cart")
            .param("page", "0")
            .param("size", "10"),
        ).andExpect(status().isOk)
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.content[0].actionType").value("CLICK"))
        .andExpect(jsonPath("$.data.content[0].pageUrl").value("/pages/product-detail/index"))
        .andExpect(jsonPath("$.data.content[0].elementId").value("btn-add-cart"))

      // 时间范围筛选
      val start = LocalDateTime.now().minusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      val end = LocalDateTime.now().plusMinutes(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      mockMvc
        .perform(
          get("/api/logs/user-actions")
            .param("startTime", start)
            .param("endTime", end)
            .param("page", "0")
            .param("size", "10"),
        ).andExpect(status().isOk)
        .andExpect(jsonPath("$.data.totalElements").value(2))
    }
    test("query requires permission") {
      setMockUser(authorities = listOf("ROLE_ADMIN"))
      mockMvc
        .perform(get("/api/logs/user-actions"))
        .andExpect(status().isForbidden)
    }
    test("batch report requires authentication") {
      mockMvc
        .perform(
          post("/api/logs/user-actions/batch")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""{"actions": []}"""),
        ).andExpect(status().isUnauthorized)
    }
  })
