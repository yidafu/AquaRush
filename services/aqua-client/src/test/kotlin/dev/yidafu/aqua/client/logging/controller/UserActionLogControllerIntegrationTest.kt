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

package dev.yidafu.aqua.client.logging.controller

import dev.yidafu.aqua.client.AquaClientApplication
import dev.yidafu.aqua.client.testutil.setMockUser
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * 用户操作日志上报控制器集成测试(client 端)
 *
 * 回归验证:小程序端经 `POST /api/logs/user-actions/batch` 上报时,
 * client 服务必须注册该端点并正常落库(此前缺失导致 NoResourceFoundException → 500)。
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.MOCK,
  classes = [AquaClientApplication::class],
)
@ActiveProfiles("test")
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

    beforeTest {
      // Boot 4.0 不再提供 @AutoConfigureMockMvc,手动装配并挂载 springSecurity 过滤器链
      mockMvc =
        MockMvcBuilders
          .webAppContextSetup(context)
          .apply<DefaultMockMvcBuilder>(springSecurity())
          .build()
      userActionLogRepository.deleteAll()
    }
    test("batch report persists and injects server-side userAgent/clientIp") {
      setMockUser()
      val body =
        """
        {
          "actions": [
            {
              "userId": "u-2001",
              "username": "alice",
              "actionType": "CLICK",
              "target": "add-to-cart",
              "pageUrl": "/pages/product-detail/index",
              "elementId": "btn-add-cart",
              "elementType": "button",
              "elementText": "加入购物车",
              "properties": {"productId": 2001},
              "timestamp": 1700000000000
            }
          ]
        }
        """.trimIndent()

      mockMvc
        .perform(
          post("/api/logs/user-actions/batch")
            .header("X-Forwarded-For", "203.0.113.7")
            .header("User-Agent", "WechatDevTools/1.0")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body),
        ).andExpect(status().isOk)
        .andExpect(jsonPath("$.success").value(true))

      awaitCount(1)

      val log = userActionLogRepository.findByUserId("u-2001").single()
      log.userId shouldBe "u-2001"
      log.target shouldBe "add-to-cart"
      log.pageUrl shouldBe "/pages/product-detail/index"
      log.elementId shouldBe "btn-add-cart"
      // 服务端注入字段:批次上报必须带上真实客户端 IP / UA
      log.clientIp shouldBe "203.0.113.7"
      log.userAgent shouldBe "WechatDevTools/1.0"
      // properties pretty-print 落库
      val props = log.properties
      props.shouldNotBeNull()
      withClue("properties should be pretty-printed, got: $props") { props!!.contains("\n  ") shouldBe true }
      withClue("properties should keep safe keys") { props.contains("productId") shouldBe true }
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
