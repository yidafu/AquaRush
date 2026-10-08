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

package dev.yidafu.aqua.ops.banner.service.impl

import dev.yidafu.aqua.api.dto.BannerListRequest
import dev.yidafu.aqua.api.service.ops.BannerQueryService
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository
import dev.yidafu.aqua.ops.banner.domain.repository.BannerSpecifications
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

/**
 * 轮播图查询服务实现(管理后台)
 */
@Service
class BannerQueryServiceImpl(
  private val bannerRepository: BannerRepository,
) : BannerQueryService {
  override fun listBanners(request: BannerListRequest): Page<BannerModel> {
    var spec: Specification<BannerModel> = Specification { _, _, cb -> cb.conjunction() }
    request.keyword?.takeIf { it.isNotBlank() }?.let { spec = spec.and(BannerSpecifications.keywordContains(it)) }
    request.enabled?.let { spec = spec.and(BannerSpecifications.enabledEquals(it)) }
    val pageable = PageRequest.of(request.page, request.size, Sort.by(Sort.Direction.ASC, "sortOrder"))
    return bannerRepository.findAll(spec, pageable)
  }

  override fun getBanner(id: Long): BannerModel? = bannerRepository.findById(id).orElse(null)
}
