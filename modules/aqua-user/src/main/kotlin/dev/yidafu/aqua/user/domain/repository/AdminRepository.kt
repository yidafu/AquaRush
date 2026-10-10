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

import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.Optional

/** 账号/送水员仓储(JPA→Exposed 迁移 P3 收尾;Specification 已内联为显式条件)。 */
interface AdminRepository {
  fun findByUsername(username: String): AdminModel?

  fun findByUserId(userId: Long): AdminModel?

  fun findByUsernameAndRole(
    username: String,
    role: AdminRoleModel,
  ): AdminModel?

  fun findByPhone(phone: String): AdminModel?

  fun findByRole(role: AdminRoleModel): List<AdminModel>

  /**
   * 按角色集合分页查询(送水员档案并入 admins 单表后,配送能力角色查询走这里)。
   * keyword 模糊匹配 username/realName/phone,null=不过滤 —— 列表接口一律分页 + 过滤(规则 10)。
   */
  fun findAllByRoleIn(
    roles: Collection<AdminRoleModel>,
    keyword: String?,
    pageable: Pageable,
  ): Page<AdminModel>

  /** 按角色集合取前 [limit] 条(内部装配/统计用,SQL 侧有界,不对外提供无界查询)。 */
  fun findAllByRoleIn(
    roles: Collection<AdminRoleModel>,
    limit: Int,
  ): List<AdminModel>

  /** 按角色集合计数(配送员统计 totalWorkers 等,避免全表加载) */
  fun countByRoleIn(roles: Collection<AdminRoleModel>): Long

  /** 按在线状态查询(DeliverWorkerStatusConverter 自动转换;SQL 侧 limit 有界) */
  fun findByOnlineStatus(
    status: DeliverWorkerModelStatus,
    limit: Int,
  ): List<AdminModel>

  fun countByRole(role: AdminRoleModel): Long

  fun existsByUsername(username: String): Boolean

  fun existsByPhone(phone: String): Boolean

  /**
   * 分页查询账号(keyword 模糊匹配 username/realName/phone,null=不过滤)。
   * AGENTS.md 规则 10:列表接口必须分页 + 过滤,禁止返回全表。
   */
  fun findAllAdmins(
    keyword: String?,
    role: AdminRoleModel?,
    pageable: Pageable,
  ): Page<AdminModel>

  fun findAdminById(id: Long): AdminModel?

  // ===== 基础操作(JpaRepository 显式化)=====
  fun findById(id: Long): Optional<AdminModel>

  fun existsById(id: Long): Boolean

  fun count(): Long

  /** 语义对齐 @SoftDelete 下的 deleteById:软删。 */
  fun deleteById(id: Long)

  /** 语义对齐 @SoftDelete 下的 deleteAll:全表软删。 */
  fun deleteAll(): Int

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: AdminModel): AdminModel
}
