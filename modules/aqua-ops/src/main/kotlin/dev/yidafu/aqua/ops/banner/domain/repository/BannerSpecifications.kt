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

package dev.yidafu.aqua.ops.banner.domain.repository

import dev.yidafu.aqua.common.domain.model.BannerModel
import org.springframework.data.jpa.domain.Specification

/**
 * 轮播图列表筛选条件(JPA Criteria,参照 PaymentSpecifications 先例)
 */
class BannerSpecifications {
  companion object {
    /**
     * 关键字模糊匹配(标题)
     */
    fun keywordContains(keyword: String): Specification<BannerModel> =
      Specification { root, _, cb ->
        cb.like(cb.lower(root.get<String>("title")), "%${keyword.lowercase()}%")
      }

    /**
     * 启用状态等值匹配
     */
    fun enabledEquals(enabled: Boolean): Specification<BannerModel> =
      Specification { root, _, cb ->
        cb.equal(root.get<Boolean>("enabled"), enabled)
      }
  }
}
