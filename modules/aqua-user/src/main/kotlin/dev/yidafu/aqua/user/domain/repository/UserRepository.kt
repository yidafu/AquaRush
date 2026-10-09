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

import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

/** 用户仓储(JPA→Exposed 迁移 P3 收尾,JpaRepository 派生查询显式化;跨模块读走 api/service)。 */
interface UserRepository : UserRepositoryCustom {
  fun findByWechatOpenId(wechatOpenId: String): UserModel?

  fun findByPhone(phone: String): UserModel?

  fun existsByWechatOpenId(wechatOpenId: String): Boolean

  fun findByNicknameContainingIgnoreCaseOrPhoneContainingIgnoreCase(
    nickname: String,
    phone: String,
    pageable: Pageable,
  ): Page<UserModel>

  fun findByStatus(
    status: UserStatus,
    pageable: Pageable,
  ): Page<UserModel>

  // ===== 客户分组(F-MKT-001,营销受众维度)=====
  fun findByCustomerGroupId(groupId: Long): List<UserModel>

  fun findByCustomerGroupId(
    groupId: Long,
    pageable: Pageable,
  ): Page<UserModel>

  fun countByCustomerGroupId(groupId: Long): Long

  // ===== 创建时间窗口查询(统计口径:createdAt 严格开区间,与原内存 isAfter/isBefore 等价)=====
  fun findByCreatedAtGreaterThan(startDate: LocalDateTime): List<UserModel>

  fun findByCreatedAtGreaterThanAndCreatedAtLessThan(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<UserModel>

  fun countByCreatedAtGreaterThanAndCreatedAtLessThan(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long

  // ===== 基础操作(JpaRepository 显式化)=====
  fun findById(id: Long): Optional<UserModel>

  /** 存量接口遗留的无界查询(违反规则 10,仅 DebugJwtController 存量使用;不新增调用)。 */
  fun findAll(): List<UserModel>

  fun findAll(pageable: Pageable): Page<UserModel>

  fun findAllById(ids: List<Long>): List<UserModel>

  fun count(): Long

  fun existsById(id: Long): Boolean

  /** 语义对齐 @SoftDelete 下的 delete:软删。 */
  fun delete(entity: UserModel)

  /** 语义对齐 @SoftDelete 下的 deleteById:软删。 */
  fun deleteById(id: Long)

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: UserModel): UserModel
}

/** 用户域跨模块查询(F-MKT/统计口径;JPA→Exposed 迁移 P3 收尾)。 */
interface UserRepositoryCustom {
  fun getUserTotalSpent(userId: Long): Double

  fun getUserBalance(userId: Long): Double

  fun getUserAddressCount(userId: Long): Int

  fun canUserReview(
    userId: Long,
    orderId: Long,
  ): Boolean

  fun findByNicknameContainingIgnoreCaseAndStatusOrPhoneContainingIgnoreCaseAndStatus(
    keyword: String,
    status: UserStatus,
    pageable: Pageable,
  ): Page<UserModel>
}
