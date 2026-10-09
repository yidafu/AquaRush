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

import dev.yidafu.aqua.common.domain.model.PromotionActivityModel
import dev.yidafu.aqua.common.graphql.generated.CreatePromotionActivityInput
import dev.yidafu.aqua.common.graphql.generated.UpdatePromotionActivityInput

/**
 * 满减/折扣活动变更服务(管理后台,PRD 10-marketing.md §F-MKT-004)
 * 配置变更不影响已生成订单(成交价按下单时快照);停用立即失效。
 */
interface PromotionActivityMutationService {
  /**
   * 新建活动并上架(档位递增互斥校验)
   */
  fun createActivity(
    adminId: Long,
    adminName: String,
    input: CreatePromotionActivityInput,
  ): PromotionActivityModel

  /**
   * 编辑活动(全量覆盖,含档位)
   */
  fun updateActivity(
    adminId: Long,
    adminName: String,
    id: Long,
    input: UpdatePromotionActivityInput,
  ): PromotionActivityModel

  /**
   * 停用活动(立即失效,不再作用于新订单)
   */
  fun disableActivity(
    adminId: Long,
    adminName: String,
    id: Long,
  ): PromotionActivityModel
}
