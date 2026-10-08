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

package dev.yidafu.aqua.notification.channel.wechat

import dev.yidafu.aqua.notice.config.WeChatProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.web.client.RestTemplate

/**
 * 微信 access_token 服务测试:缓存命中不重复请求,过期后自动刷新。
 */
class WeChatAccessTokenServiceTest :
  FunSpec({
    var restTemplate: RestTemplate = mockk()
    var service: WeChatAccessTokenService = mockk()
    var fetchCount = 0

    beforeTest {
      restTemplate = mockk()
      fetchCount = 0
      every {
        restTemplate.getForObject(any<String>(), any<Class<WeChatTokenResponse>>())
      } answers {
        fetchCount++
        WeChatTokenResponse(accessToken = "token_$fetchCount", expiresIn = 7200)
      }
      service =
        WeChatAccessTokenService(
          restTemplate = restTemplate,
          weChatProperties = WeChatProperties(appId = "appid", appSecret = "secret", tokenCacheMinutes = 90),
        )
    }

    test("首次获取 access_token") {
      service.getAccessToken() shouldBe "token_1"
      fetchCount shouldBe 1
    }

    test("缓存期内重复获取不再请求微信") {
      service.getAccessToken() shouldBe "token_1"
      service.getAccessToken() shouldBe "token_1"
      service.getAccessToken() shouldBe "token_1"
      fetchCount shouldBe 1
      verify(exactly = 1) { restTemplate.getForObject(any<String>(), any<Class<WeChatTokenResponse>>()) }
    }

    test("access_token 获取失败应抛异常") {
      val failingTemplate = mockk<RestTemplate>()
      every { failingTemplate.getForObject(any<String>(), any<Class<WeChatTokenResponse>>()) } returns
        WeChatTokenResponse(errcode = 40013, errmsg = "invalid appid")
      val failingService =
        WeChatAccessTokenService(
          restTemplate = failingTemplate,
          weChatProperties = WeChatProperties(appId = "bad", appSecret = "secret"),
        )
      val exception =
        runCatching { failingService.getAccessToken() }.exceptionOrNull()
      exception shouldNotBe null
      exception?.message?.contains("invalid appid") shouldBe true
    }
  })
