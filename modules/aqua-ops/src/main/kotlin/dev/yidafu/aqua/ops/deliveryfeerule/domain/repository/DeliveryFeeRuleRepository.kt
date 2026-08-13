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

package dev.yidafu.aqua.ops.deliveryfeerule.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 配送费规则仓库(单行配置,固定 id = SINGLETON_ID)
 * 注意:@SoftDelete 自动过滤已删行。
 */
@Repository
interface DeliveryFeeRuleRepository : JpaRepository<DeliveryFeeRuleModel, Long> {
  companion object {
    /** 单行配置固定主键 */
    const val SINGLETON_ID = 1L
  }
}
