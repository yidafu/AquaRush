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

package dev.yidafu.aqua.marketing.flashsale.domain.repository

import dev.yidafu.aqua.common.domain.model.FlashSaleActivityModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

/**
 * 限时特价活动仓库(F-MKT-005)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository/JpaSpecificationExecutor,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删;当前生效特价/重叠校验见 [FlashSaleActivityRepositoryCustom]。
 */
interface FlashSaleActivityRepository : FlashSaleActivityRepositoryCustom {
  fun findById(id: Long): Optional<FlashSaleActivityModel>

  /** 后台活动分页:name 关键词(大小写不敏感);固定 id 升序破平。 */
  fun searchActivities(
    keyword: String?,
    pageable: Pageable,
  ): Page<FlashSaleActivityModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: FlashSaleActivityModel): FlashSaleActivityModel
}
