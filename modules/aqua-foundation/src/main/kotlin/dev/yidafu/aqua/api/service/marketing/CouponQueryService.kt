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

package dev.yidafu.aqua.api.service.marketing

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 优惠券模板查询服务(管理后台,PRD 10-marketing.md §F-MKT-003)
 */
interface CouponQueryService {
  /**
   * 模板列表(分页,关键字按名称模糊;含已领/已核销统计)
   */
  fun listTemplates(
    keyword: String?,
    status: dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus?,
    pageable: Pageable,
  ): Page<CouponTemplateModel>

  fun getTemplate(id: Long): CouponTemplateModel?

  /**
   * 模板已领取数(不含作废/过期)
   */
  fun receivedCount(templateId: Long): Long

  /**
   * 模板已核销数
   */
  fun usedCount(templateId: Long): Long
}
