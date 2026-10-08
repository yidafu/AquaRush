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

import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel

/**
 * 配送费规则查询服务接口(管理后台)
 * 与 PRD 12-operations-config.md §F-OPS-005 保持一致:未配置时回显默认规则(免费4楼/加价2元)。
 */
interface DeliveryFeeRuleQueryService {
  /**
   * 获取当前生效规则(未配置返回默认规则,永不返回 null)
   */
  fun getEffectiveDeliveryFeeRule(): DeliveryFeeRuleModel
}
