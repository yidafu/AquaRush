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

package dev.yidafu.aqua.analytics.reconciliation.external.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 对账配置属性(wechat 凭据字段为 var:JavaBean 绑定需要 setter,测试环境经 properties 注入)
 */
@Component
@ConfigurationProperties(prefix = "aqua.reconciliation")
data class ReconciliationConfig(
  val enabled: Boolean = true,
  val scheduleCron: String = "0 0 2 * * ?",
  val batchSize: Int = 1000,
  val retryAttempts: Int = 3,
  val alertThreshold: Double = 0.01,
  val wechat: WeChatConfig = WeChatConfig(),
  var weChatAppId: String = "",
  var weChatMchId: String = "",
  var weChatApiKey: String = "",
) {
  data class WeChatConfig(
    val apiUrl: String = "https://api.mch.weixin.qq.com",
    val timeout: Int = 30000,
    val retryDelay: Int = 5000,
  )

  /**
   * 真实商户参数是否已配置(R-PAY-04:未配置 = 假支付模式,对账任务置 SKIPPED 并说明原因;
   * 口径对齐 aqua-trade SdkWechatPayGateway.isConfigured —— appid/mchId/apiKey 任一缺失即视为未配置)。
   */
  val weChatConfigured: Boolean
    get() = weChatAppId.isNotBlank() && weChatMchId.isNotBlank() && weChatApiKey.isNotBlank()
}
