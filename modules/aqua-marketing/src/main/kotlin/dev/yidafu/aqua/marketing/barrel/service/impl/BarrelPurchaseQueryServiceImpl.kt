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

package dev.yidafu.aqua.marketing.barrel.service.impl

import dev.yidafu.aqua.api.service.barrel.BarrelPurchaseQueryService
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseItemModel
import dev.yidafu.aqua.common.domain.model.BarrelPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.BarrelPaymentStatus
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelPurchaseItemRepository
import dev.yidafu.aqua.marketing.barrel.domain.repository.BarrelPurchaseRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 水桶购买单查询服务实现(G06:我的水桶 / 支付回调按单号查询 / 后台购桶单分页)
 */
@Service
class BarrelPurchaseQueryServiceImpl(
  private val barrelPurchaseRepository: BarrelPurchaseRepository,
  private val barrelPurchaseItemRepository: BarrelPurchaseItemRepository,
) : BarrelPurchaseQueryService {
  override fun getById(purchaseId: Long): BarrelPurchaseModel? = barrelPurchaseRepository.findById(purchaseId).orElse(null)

  override fun findByOrderNo(orderNo: String): BarrelPurchaseModel? = barrelPurchaseRepository.findByOrderNo(orderNo)

  override fun findItemsByPurchaseId(purchaseId: Long): List<BarrelPurchaseItemModel> =
    barrelPurchaseItemRepository.findAllByPurchaseIdOrderByIdAsc(purchaseId)

  override fun findLatestByUserId(userId: Long): BarrelPurchaseModel? =
    barrelPurchaseRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)

  override fun hasPaidPurchase(userId: Long): Boolean =
    barrelPurchaseRepository.existsByUserIdAndPaymentStatus(userId, BarrelPaymentStatus.PAID)

  override fun list(
    status: BarrelPaymentStatus?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    page: Int,
    size: Int,
  ): Page<BarrelPurchaseModel> {
    // 排序由仓储固定(createdAt 降序 + id 破平),pageable 只带页码
    return barrelPurchaseRepository.searchPurchases(status, dateFrom, dateTo, PageRequest.of(page, size))
  }
}
