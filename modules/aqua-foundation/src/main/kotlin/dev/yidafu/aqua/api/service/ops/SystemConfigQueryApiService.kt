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

package dev.yidafu.aqua.api.service.ops

/**
 * 系统参数业务读取服务接口(跨模块消费)
 * 供业务域(如商品统计 F-SYS-003 库存预警、订单履约 F-SYS-002)读取平台参数,
 * 取值优先级:当前值 → 声明默认值 → 调用方兜底值(R-SYS-01 配置中心降级默认)。
 */
interface SystemConfigQueryApiService {
  /**
   * 读取数值型参数(分/件等长整型),失败时按 当前值 → 默认值 → fallback 兜底
   */
  fun getLongValue(
    key: String,
    fallback: Long,
  ): Long

  /**
   * 读取布尔型参数,失败时按 当前值 → 默认值 → fallback 兜底
   */
  fun getBooleanValue(
    key: String,
    fallback: Boolean,
  ): Boolean

  /**
   * 读取文本型参数,失败时按 当前值 → 默认值 → fallback 兜底
   */
  fun getStringValue(
    key: String,
    fallback: String?,
  ): String?
}
