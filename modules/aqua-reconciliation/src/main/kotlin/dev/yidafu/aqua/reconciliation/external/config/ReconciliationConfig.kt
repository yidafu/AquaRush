/*
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

package dev.yidafu.aqua.reconciliation.external.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 对账配置属性
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
  val weChatAppId: String = "",
  val weChatMchId: String = "",
  val weChatApiKey: String = "",
) {
  data class WeChatConfig(
    val apiUrl: String = "https://api.mch.weixin.qq.com",
    val timeout: Int = 30000,
    val retryDelay: Int = 5000,
  )
}
