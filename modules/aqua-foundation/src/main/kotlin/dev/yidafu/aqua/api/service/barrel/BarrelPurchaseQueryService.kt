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

package dev.yidafu.aqua.api.service.barrel

import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import org.springframework.data.domain.Page
import java.time.LocalDateTime

/**
 * 水桶购买单查询服务(G06 水桶费模块,实现位于 aqua-marketing;CLAUDE.md 规则 4)
 */
interface BarrelPurchaseQueryService {
  fun getById(purchaseId: Long): BarrelPurchaseModel?

  fun findByOrderNo(orderNo: String): BarrelPurchaseModel?

  /** 购桶单明细(规格/单价/数量快照) */
  fun findItemsByPurchaseId(purchaseId: Long): List<BarrelPurchaseItemModel>

  /** 当前用户最近购桶单(我的水桶;未购买返回 null) */
  fun findLatestByUserId(userId: Long): BarrelPurchaseModel?

  /** 是否存在已支付(PAID)购桶单(每用户总限购一次判定) */
  fun hasPaidPurchase(userId: Long): Boolean

  /** 后台购桶单分页(状态/支付时间范围筛选) */
  fun list(
    status: BarrelPaymentStatus?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    page: Int,
    size: Int,
  ): Page<BarrelPurchaseModel>
}
