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

package dev.yidafu.aqua.marketing.coupon.domain.repository

import dev.yidafu.aqua.common.domain.model.CouponTemplateModel
import dev.yidafu.aqua.common.domain.model.enums.CouponTemplateStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

/**
 * 优惠券模板仓库(F-MKT-003)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted。
 */
interface CouponTemplateRepository {
  fun findByName(name: String): CouponTemplateModel?

  fun findByStatus(status: CouponTemplateStatus): List<CouponTemplateModel>

  fun findById(id: Long): Optional<CouponTemplateModel>

  fun findAllById(ids: List<Long>): List<CouponTemplateModel>

  /** 后台模板分页:name 关键词(大小写不敏感) + status 精确;固定 id 升序破平。 */
  fun searchTemplates(
    keyword: String?,
    status: CouponTemplateStatus?,
    pageable: Pageable,
  ): Page<CouponTemplateModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: CouponTemplateModel): CouponTemplateModel
}
