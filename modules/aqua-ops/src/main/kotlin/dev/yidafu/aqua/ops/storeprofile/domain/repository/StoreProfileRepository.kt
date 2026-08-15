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

package dev.yidafu.aqua.ops.storeprofile.domain.repository

import dev.yidafu.aqua.common.domain.model.StoreProfileModel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * 店铺信息仓库(单行配置,固定 id = SINGLETON_ID)
 * 注意:@SoftDelete 自动过滤已删行。
 */
@Repository
interface StoreProfileRepository : JpaRepository<StoreProfileModel, Long> {
  companion object {
    /** 单行配置固定主键 */
    const val SINGLETON_ID = 1L
  }

  /**
   * 原生硬删除(绕过 @SoftDelete 的软删除改写):
   * 软删行仍占用 SINGLETON_ID,单行配置重新保存会撞主键(测试复位用)。
   */
  @Modifying
  @Transactional
  @Query(value = "DELETE FROM store_profiles", nativeQuery = true)
  fun hardDeleteAll()
}
