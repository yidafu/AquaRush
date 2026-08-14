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

package dev.yidafu.aqua.marketing.pricing.service.impl

import dev.yidafu.aqua.api.service.marketing.GroupProductPriceQueryService
import dev.yidafu.aqua.common.domain.model.GroupProductPriceModel
import dev.yidafu.aqua.marketing.pricing.domain.repository.GroupProductPriceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * 分组商品专属价查询服务实现(管理后台,F-MKT-002)
 */
@Service
class GroupProductPriceQueryServiceImpl(
  private val groupProductPriceRepository: GroupProductPriceRepository,
) : GroupProductPriceQueryService {
  override fun listByGroup(
    groupId: Long,
    pageable: Pageable,
  ): Page<GroupProductPriceModel> = groupProductPriceRepository.findByCustomerGroupId(groupId, pageable)

  override fun getByGroupAndProduct(
    groupId: Long,
    productId: Long,
  ): GroupProductPriceModel? = groupProductPriceRepository.findByCustomerGroupIdAndProductId(groupId, productId)
}
