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

package dev.yidafu.aqua.api.service.admin

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 管理员服务接口
 *
 * 送水员档案并入 admins 单表后(spec/prd/07-user.md §F-USR-021 目标架构),
 * 送水员查询/创建/更新统一走本接口:配送能力角色(DELIVERY_WORKER/ADMIN)
 * 的账号行即送水员身份。
 */
interface AdminService {
  /**
   * Find admin by id
   */
  fun findById(id: Long): AdminModel?

  fun findByUserId(userId: Long): AdminModel?

  /**
   * Find admin by username
   */
  fun findByUsername(username: String): AdminModel?

  /**
   * Create or update admin
   */
  fun save(admin: AdminModel): AdminModel

  /**
   * Delete admin by id
   */
  fun deleteById(id: Long): Boolean

  /**
   * Check if admin exists by username
   */
  fun existsByUsername(username: String): Boolean

  /**
   * Check if admin exists by id
   */
  fun existsById(id: Long): Boolean

  /**
   * Find admins by role
   */
  fun findByRole(role: AdminRoleModel): List<AdminModel>

  /**
   * 获取具备配送能力的账号(role ∈ {DELIVERY_WORKER, ADMIN},兼容旧 deliveryWorkers 查询语义)。
   * 内部装配/统计用,SQL 侧有界(上限见实现);对外列表接口请用 [findDeliveryWorkers]。
   */
  fun findAllDeliveryCapable(): List<AdminModel>

  /**
   * 分页查询具备配送能力的账号(keyword 模糊匹配 username/realName/phone)。
   * AGENTS.md 规则 10:配送员列表接口必须分页 + 过滤。
   */
  fun findDeliveryWorkers(
    keyword: String?,
    role: AdminRoleModel?,
    pageable: Pageable,
  ): Page<AdminModel>

  /**
   * 具备配送能力的账号数(role ∈ {DELIVERY_WORKER, ADMIN},统计场景条件 count,避免全表加载)
   */
  fun countDeliveryCapable(): Long

  /**
   * 按在线状态查询配送能力账号
   */
  fun findByOnlineStatus(status: DeliverWorkerModelStatus): List<AdminModel>

  /**
   * Check if admin exists by phone
   */
  fun existsByPhone(phone: String): Boolean

  /**
   * 统计指定角色账号数(删除/停用保护用:至少保留一个超级管理员)
   */
  fun countByRole(role: AdminRoleModel): Long

  /**
   * 重置账号密码(G11 落地,spec/prd/07-user.md §F-USR-020/021)
   *
   * @param newPassword 新密码;为空时重置为默认密码(缺省 123456,与 PRD 一致)
   */
  fun resetPassword(
    id: Long,
    newPassword: String?,
  ): AdminModel

  /**
   * Create a new admin user
   *
   * role=DELIVERY_WORKER 且 username 为空时自动生成 `delivery_{账号ID}`(单表落库,
   * 不再创建 delivery_workers 档案,见 F-USR-021 业务规则扩充)。
   */
  fun createAdmin(
    username: String?,
    password: String?,
    realName: String?,
    phone: String?,
    role: AdminRoleModel,
    avatarUrl: String? = null,
    onlineStatus: DeliverWorkerModelStatus = DeliverWorkerModelStatus.OFFLINE,
    isAvailable: Boolean = true,
    coordinates: String? = null,
    currentLocation: String? = null,
    rating: Double? = null,
    earningCents: Long? = null,
    accountStatus: AdminAccountStatus = AdminAccountStatus.ACTIVE,
  ): AdminModel

  /**
   * Update an existing admin user
   */
  fun updateAdmin(
    id: Long,
    realName: String?,
    phone: String?,
    role: AdminRoleModel?,
    avatarUrl: String? = null,
    onlineStatus: DeliverWorkerModelStatus? = null,
    isAvailable: Boolean? = null,
    coordinates: String? = null,
    currentLocation: String? = null,
    rating: Double? = null,
    earningCents: Long? = null,
    accountStatus: AdminAccountStatus? = null,
  ): AdminModel

  fun getUserById(id: Long): UserModel
}
