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

package dev.yidafu.aqua.marketing.gift.service.impl

import dev.yidafu.aqua.api.service.marketing.GiftActivityQueryService
import dev.yidafu.aqua.common.domain.model.GiftActivityModel
import dev.yidafu.aqua.marketing.gift.domain.repository.GiftActivityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

/**
 * 买赠/满赠活动查询服务实现(管理后台,F-MKT-008)
 */
@Service
class GiftActivityQueryServiceImpl(
  private val giftActivityRepository: GiftActivityRepository,
) : GiftActivityQueryService {
  override fun listActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<GiftActivityModel> = giftActivityRepository.searchActivities(keyword, pageable)

  override fun getActivity(id: Long): GiftActivityModel? = giftActivityRepository.findById(id).orElse(null)
}
