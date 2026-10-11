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

package dev.yidafu.aqua.api.service.order

import dev.yidafu.aqua.api.dto.SubscriptionListQuery
import dev.yidafu.aqua.common.domain.model.SubscriptionModel
import org.springframework.data.domain.Page

/**
 * 周期订购计划查询服务(F-ORD-026,后台入口;G77 新方向:仅后台查看/管理)。
 * 实现在 aqua-trade `subscription` 包;调用方为 admin resolvers。
 */
interface SubscriptionQueryService {
  /** 分页筛选查询计划(keyword 匹配用户昵称/手机号,状态与下次配送时间区间筛选) */
  fun search(query: SubscriptionListQuery): Page<SubscriptionModel>

  /** 按 id 查询计划,不存在抛 NotFoundException */
  fun getById(id: Long): SubscriptionModel
}
