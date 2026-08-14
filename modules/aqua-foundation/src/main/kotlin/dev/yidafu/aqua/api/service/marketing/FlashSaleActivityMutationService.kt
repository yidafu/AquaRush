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

import dev.yidafu.aqua.api.dto.FlashSaleActivityCreateRequest
import dev.yidafu.aqua.api.dto.FlashSaleActivityUpdateRequest
import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel

/**
 * 限时特价活动变更服务(管理后台,PRD 10-marketing.md §F-MKT-005/R-MKT-05)
 * 活动期自动按特价计价(单价链③);配置变更不影响已生成订单;停用/到期立即失效。
 */
interface FlashSaleActivityMutationService {
  /**
   * 新建特价活动并上架(校验特价低于目录价、时间窗、同商品时间窗重叠)
   */
  fun createActivity(
    adminId: Long,
    adminName: String,
    request: FlashSaleActivityCreateRequest,
  ): FlashSaleActivityModel

  /**
   * 编辑特价活动(全量覆盖)
   */
  fun updateActivity(
    adminId: Long,
    adminName: String,
    id: Long,
    request: FlashSaleActivityUpdateRequest,
  ): FlashSaleActivityModel

  /**
   * 停用活动(立即失效,商品回退分组价/目录价)
   */
  fun disableActivity(
    adminId: Long,
    adminName: String,
    id: Long,
  ): FlashSaleActivityModel
}
