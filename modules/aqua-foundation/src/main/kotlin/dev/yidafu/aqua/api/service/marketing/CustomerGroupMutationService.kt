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

import dev.yidafu.aqua.api.dto.CustomerGroupCreateRequest
import dev.yidafu.aqua.api.dto.CustomerGroupUpdateRequest
import dev.yidafu.aqua.common.domain.model.CustomerGroupModel

/**
 * 客户分组变更服务(管理后台,PRD 10-marketing.md §F-MKT-001)
 * 操作人显式传参,由实现落审计日志。
 */
interface CustomerGroupMutationService {
  /**
   * 新建分组(名称唯一,重复抛异常)
   */
  fun createGroup(
    adminId: Long,
    adminName: String,
    request: CustomerGroupCreateRequest,
  ): CustomerGroupModel

  /**
   * 编辑分组(名称唯一,排除自身)
   */
  fun updateGroup(
    adminId: Long,
    adminName: String,
    id: Long,
    request: CustomerGroupUpdateRequest,
  ): CustomerGroupModel

  /**
   * 删除分组(软删除;成员回「未分组」)
   */
  fun deleteGroup(
    adminId: Long,
    adminName: String,
    id: Long,
  ): Boolean

  /**
   * 设置用户所属分组(单分组;customerGroupId=null 回「未分组」,即时生效)
   */
  fun setUserGroup(
    adminId: Long,
    adminName: String,
    userId: Long,
    customerGroupId: Long?,
  ): CustomerGroupModel?
}
