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

import com.fasterxml.jackson.annotation.JsonProperty
import dev.yidafu.aqua.notice.config.WeChatProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

/**
 * 微信 access_token 服务(PRD 13-notification.md F-NTF-001/G08)
 *
 * GET /cgi-bin/token(grant_type=client_credential),内存缓存 WeChatProperties.tokenCacheMinutes
 * (默认 90 分钟,提前 5 分钟刷新);多线程并发时单飞刷新。
 */
@Service
class WeChatAccessTokenService(
  private val restTemplate: RestTemplate,
  private val weChatProperties: WeChatProperties,
) {
  private val logger = LoggerFactory.getLogger(WeChatAccessTokenService::class.java)

  @Volatile
  private var cachedToken: String? = null

  @Volatile
  private var expiresAtMillis: Long = 0L

  /** 获取 access_token(缓存优先,过期自动刷新) */
  fun getAccessToken(): String {
    val now = System.currentTimeMillis()
    cachedToken?.let { cached ->
      if (now < expiresAtMillis) {
        return cached
      }
    }
    synchronized(this) {
      cachedToken?.let { cached ->
        if (System.currentTimeMillis() < expiresAtMillis) {
          return cached
        }
      }
      val token = fetchToken()
      cachedToken = token
      expiresAtMillis =
        System.currentTimeMillis() +
          (weChatProperties.tokenCacheMinutes - 5).coerceAtLeast(1) * 60_000L
      logger.info("WeChat access_token refreshed, cache minutes: {}", weChatProperties.tokenCacheMinutes)
      return token
    }
  }

  private fun fetchToken(): String {
    val url =
      "${weChatProperties.apiUrl}/cgi-bin/token?grant_type=client_credential" +
        "&appid=${weChatProperties.appId}&secret=${weChatProperties.appSecret}"
    val response = restTemplate.getForObject(url, WeChatTokenResponse::class.java)
    if (response == null || response.accessToken.isNullOrBlank() || response.errcode != null && response.errcode != 0) {
      throw IllegalStateException("WeChat access_token 获取失败: ${response?.errmsg ?: "空响应"}")
    }
    return response.accessToken
  }
}

/** 微信 access_token 响应 */
data class WeChatTokenResponse(
  @JsonProperty("access_token") val accessToken: String? = null,
  @JsonProperty("expires_in") val expiresIn: Int = 7200,
  @JsonProperty("errcode") val errcode: Int? = null,
  @JsonProperty("errmsg") val errmsg: String? = null,
)
