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

package dev.yidafu.aqua.ops.storeprofile.service.impl

import dev.yidafu.aqua.api.service.ops.StoreProfileQueryApiService
import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import dev.yidafu.aqua.ops.storeprofile.domain.repository.StoreProfileRepository
import org.springframework.stereotype.Service

/**
 * 店铺信息查询服务实现(用户端)
 * 配置即展示(R-OPS-01):未配置返回 null,用户端按默认值兜底。
 */
@Service
class StoreProfileQueryApiServiceImpl(
  private val storeProfileRepository: StoreProfileRepository,
) : StoreProfileQueryApiService {
  override fun getStoreProfile(): StoreProfileModel? =
    storeProfileRepository.findById(StoreProfileRepository.SINGLETON_ID).orElse(null)
}
