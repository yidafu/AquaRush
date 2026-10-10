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

package dev.yidafu.aqua.user.domain.repository

import dev.yidafu.aqua.common.domain.model.AddressModel
import java.util.Optional

/** 收货地址仓储(JPA→Exposed 迁移 P3 收尾;Specification 已内联为显式条件)。 */
interface AddressRepository : AddressRepositoryCustom {
  fun findByUserId(userId: Long): List<AddressModel>

  fun findByUserIdOrderByIsDefaultDescCreatedAtDesc(userId: Long): List<AddressModel>

  fun findByUserIdOrderByIdDesc(userId: Long): List<AddressModel>

  fun findByUserIdAndIsDefault(
    userId: Long,
    isDefault: Boolean,
  ): AddressModel?

  fun findByUserIdAndIdNot(
    userId: Long,
    addressId: Long,
  ): List<AddressModel>

  fun findByUserIdAndIsDefaultTrue(userId: Long): AddressModel?

  fun countByUserId(userId: Long): Int

  // ===== 基础操作(JpaRepository 显式化;默认方法逻辑移入实现)=====
  fun findById(id: Long): Optional<AddressModel>

  fun existsById(id: Long): Boolean

  /** 语义对齐 @SoftDelete 下的 deleteById:软删。 */
  fun deleteById(id: Long)

  /** 语义对齐 @SoftDelete 下的 delete:软删。 */
  fun delete(entity: AddressModel)

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: AddressModel): AddressModel

  fun saveAll(entities: List<AddressModel>): List<AddressModel>
}
