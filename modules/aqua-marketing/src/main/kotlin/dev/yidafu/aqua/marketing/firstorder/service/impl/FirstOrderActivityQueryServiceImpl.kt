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

package dev.yidafu.aqua.marketing.firstorder.service.impl

import dev.yidafu.aqua.api.service.marketing.FirstOrderActivityQueryService
import dev.yidafu.aqua.common.domain.model.FirstOrderActivityModel
import dev.yidafu.aqua.marketing.firstorder.domain.repository.FirstOrderActivityRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * 首单立减/新人专享价活动查询服务实现(管理后台,F-MKT-009)
 */
@Service
class FirstOrderActivityQueryServiceImpl(
  private val firstOrderActivityRepository: FirstOrderActivityRepository,
) : FirstOrderActivityQueryService {
  override fun listActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<FirstOrderActivityModel> {
    val spec =
      Specification<FirstOrderActivityModel> { root, _, cb ->
        if (keyword.isNullOrBlank()) {
          null
        } else {
          cb.like(cb.lower(root.get("name")), "%${keyword.lowercase()}%")
        }
      }
    return firstOrderActivityRepository.findAll(spec, pageable)
  }

  override fun getActivity(id: Long): FirstOrderActivityModel? = firstOrderActivityRepository.findById(id).orElse(null)
}
