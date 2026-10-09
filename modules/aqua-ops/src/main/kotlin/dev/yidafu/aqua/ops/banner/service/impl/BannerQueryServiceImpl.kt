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
import dev.yidafu.aqua.ops.banner.domain.repository.BannerFilter
import dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

/**
 * 轮播图查询服务实现(管理后台)
 * 列表排序固定为 sort_order 升序，由仓储层保证(原 `Sort.by(ASC, "sortOrder")` 已随
 * JPA Specification 一并迁移进 [BannerRepository.findPage])。
 */
@Service
class BannerQueryServiceImpl(
  private val bannerRepository: BannerRepository,
) : BannerQueryService {
  override fun listBanners(request: BannerListRequest): Page<BannerModel> {
    val filter =
      BannerFilter(
        keyword = request.keyword,
        enabled = request.enabled,
      )
    val pageable = PageRequest.of(request.page, request.size)
    return bannerRepository.findPage(filter, pageable)
  }

  override fun getBanner(id: Long): BannerModel? = bannerRepository.findById(id).orElse(null)
}
