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

import dev.yidafu.aqua.api.service.ops.BannerQueryApiService
import dev.yidafu.aqua.common.domain.model.BannerModel
import dev.yidafu.aqua.ops.banner.domain.repository.BannerRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 轮播图查询服务实现(用户端)
 * 返回启用且处于时间窗内的轮播图,按排序号升序(R-OPS-01:配置即展示、过期自动下架)。
 */
@Service
class BannerQueryApiServiceImpl(
  private val bannerRepository: BannerRepository,
) : BannerQueryApiService {
  override fun getActiveBanners(): List<BannerModel> = bannerRepository.findActiveBanners(LocalDateTime.now())
}
