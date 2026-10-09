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
import dev.yidafu.aqua.common.graphql.generated.CreateCouponTemplateInput
import dev.yidafu.aqua.common.graphql.generated.IssueCouponsInput
import dev.yidafu.aqua.common.graphql.generated.UpdateCouponTemplateInput

/**
 * 优惠券模板变更服务(管理后台,PRD 10-marketing.md §F-MKT-003)
 * 「建券 → 发放/领取 → 核销 → 退回/过期」闭环的管理侧。
 */
interface CouponMutationService {
  /**
   * 新建模板并上架
   */
  fun createTemplate(
    adminId: Long,
    adminName: String,
    input: CreateCouponTemplateInput,
  ): CouponTemplateModel

  /**
   * 编辑模板(全量更新;发放方式不可改)
   */
  fun updateTemplate(
    adminId: Long,
    adminName: String,
    id: Long,
    input: UpdateCouponTemplateInput,
  ): CouponTemplateModel

  /**
   * 停用模板(不可再领取/发放,已领券按快照继续有效)
   */
  fun disableTemplate(
    adminId: Long,
    adminName: String,
    id: Long,
  ): CouponTemplateModel

  /**
   * 定向发放(USER 按用户列表/GROUP 按分组全体成员;每人一张,达到限领数跳过)
   *
   * @return 实际发放张数
   */
  fun issueCoupons(
    adminId: Long,
    adminName: String,
    input: IssueCouponsInput,
  ): Int
}
