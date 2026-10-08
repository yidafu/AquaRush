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

import dev.yidafu.aqua.api.dto.FirstOrderActivityCreateRequest
import dev.yidafu.aqua.api.dto.FirstOrderActivityUpdateRequest
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel

/**
 * 首单立减/新人专享价活动变更服务(管理后台,PRD 10-marketing.md §F-MKT-009/R-MKT-10)
 * 新用户首次下单自动享受,一单一享(新用户=无已完成订单,首单完成即失效);
 * 配置变更不影响已生成订单;停用立即失效。
 */
interface FirstOrderActivityMutationService {
  /**
   * 新建活动并上架(校验类型字段/新人价低于目录价/同商品新人价重叠)
   */
  fun createActivity(
    adminId: Long,
    adminName: String,
    request: FirstOrderActivityCreateRequest,
  ): FirstOrderActivityModel

  /**
   * 编辑活动(全量覆盖)
   */
  fun updateActivity(
    adminId: Long,
    adminName: String,
    id: Long,
    request: FirstOrderActivityUpdateRequest,
  ): FirstOrderActivityModel

  /**
   * 停用活动(立即失效,不再应用)
   */
  fun disableActivity(
    adminId: Long,
    adminName: String,
    id: Long,
  ): FirstOrderActivityModel
}
