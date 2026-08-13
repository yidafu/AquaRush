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

import dev.yidafu.aqua.api.dto.SaveDeliveryFeeRuleRequest
import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel

/**
 * 配送费规则变更服务接口(管理后台)
 * 单行配置(固定 id = 1),整体覆盖保存;操作人显式传参,由实现落审计日志(R-OPS-02)。
 */
interface DeliveryFeeRuleMutationService {
  /**
   * 保存配送费规则(首次保存创建单行配置,再次保存覆盖;变更即时生效,已生成订单不受影响)
   */
  fun saveDeliveryFeeRule(
    adminId: Long,
    adminName: String,
    request: SaveDeliveryFeeRuleRequest,
  ): DeliveryFeeRuleModel
}
