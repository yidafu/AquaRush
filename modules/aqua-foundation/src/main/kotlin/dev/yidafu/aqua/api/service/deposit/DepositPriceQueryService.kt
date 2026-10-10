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

package dev.yidafu.aqua.api.service.deposit

import dev.yidafu.aqua.common.domain.model.DepositPriceModel
import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus
import org.springframework.data.domain.Page

/**
 * 水桶押金价目查询服务(R-MON-03,实现位于 aqua-marketing;AGENTS.md 规则 3)
 * 后台押金管理价目分页;下单押金价目匹配由 DepositMutationApiService 内部经仓储读取。
 */
interface DepositPriceQueryService {
  /** 后台价目管理分页(状态/关键词筛选,强制分页 R-ORD 禁全表) */
  fun list(
    status: DepositPriceStatus?,
    keyword: String?,
    page: Int,
    size: Int,
  ): Page<DepositPriceModel>
}
