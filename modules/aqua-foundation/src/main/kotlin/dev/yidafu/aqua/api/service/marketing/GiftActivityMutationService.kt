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

package dev.yidafu.aqua.api.service.marketing

import dev.yidafu.aqua.api.dto.GiftActivityCreateRequest
import dev.yidafu.aqua.api.dto.GiftActivityUpdateRequest
import dev.yidafu.aqua.common.domain.model.GiftActivityModel

/**
 * 买赠/满赠活动变更服务(管理后台,PRD 10-marketing.md §F-MKT-008/R-MKT-09)
 * 订单达标自动赠指定商品(赠品为附加 0 价行随单配送,不参与现金计价与叠加);
 * 配置变更不影响已生成订单;停用立即失效。
 */
interface GiftActivityMutationService {
  /**
   * 新建活动并上架(校验门槛/范围/赠品)
   */
  fun createActivity(
    adminId: Long,
    adminName: String,
    request: GiftActivityCreateRequest,
  ): GiftActivityModel

  /**
   * 编辑活动(全量覆盖)
   */
  fun updateActivity(
    adminId: Long,
    adminName: String,
    id: Long,
    request: GiftActivityUpdateRequest,
  ): GiftActivityModel

  /**
   * 停用活动(立即失效,不再触发赠品)
   */
  fun disableActivity(
    adminId: Long,
    adminName: String,
    id: Long,
  ): GiftActivityModel
}
