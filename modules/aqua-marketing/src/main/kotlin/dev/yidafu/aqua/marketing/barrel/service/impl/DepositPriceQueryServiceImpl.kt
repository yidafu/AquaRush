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

import dev.yidafu.aqua.api.service.deposit.DepositPriceQueryService
import dev.yidafu.aqua.common.domain.model.DepositPriceModel
import dev.yidafu.aqua.common.domain.model.enums.DepositPriceStatus
import dev.yidafu.aqua.marketing.barrel.domain.repository.DepositPriceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * 水桶押金价目查询服务实现(后台押金管理价目分页)。
 */
@Service
class DepositPriceQueryServiceImpl(
  private val depositPriceRepository: DepositPriceRepository,
) : DepositPriceQueryService {
  override fun list(
    status: DepositPriceStatus?,
    keyword: String?,
    page: Int,
    size: Int,
  ): Page<DepositPriceModel> {
    // 排序由仓储固定(sortOrder ASC + id 破平),pageable 只带页码
    return depositPriceRepository.searchPrices(status, keyword, PageRequest.of(page, size))
  }
}
