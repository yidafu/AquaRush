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

package dev.yidafu.aqua.common.security

import dev.yidafu.aqua.common.domain.model.enums.AdminPermission
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证角色→权限矩阵(PRD 01-overview.md §角色权限矩阵)与角色→authority 映射。
 */
class RolePermissionMappingTest {
  @Test
  fun `permission catalog should contain exactly 36 codes`() {
    assertEquals(36, AdminPermission.entries.size)
  }

  @Test
  fun `SUPER_ADMIN should have all permissions`() {
    val permissions = RolePermissionMapping.getPermissions(AdminRoleModel.SUPER_ADMIN)
    assertEquals(AdminPermission.entries.toSet(), permissions)
  }

  @Test
  fun `ADMIN should have 29 permissions excluding admin-user system-config and worker-specific ops`() {
    val permissions = RolePermissionMapping.getPermissions(AdminRoleModel.ADMIN)
    assertEquals(29, permissions.size)
    assertTrue(AdminPermission.ADMIN_USER_READ !in permissions)
    assertTrue(AdminPermission.ADMIN_USER_WRITE !in permissions)
    assertTrue(AdminPermission.SYSTEM_CONFIG_READ !in permissions)
    assertTrue(AdminPermission.SYSTEM_CONFIG_WRITE !in permissions)
    assertTrue(AdminPermission.ORDER_CREATE !in permissions)
    assertTrue(AdminPermission.DELIVERY_TASK !in permissions)
    assertTrue(AdminPermission.RECONCILIATION_SUBMIT !in permissions)
    // 运营配置/意见反馈 4 码属于管理员
    assertTrue(AdminPermission.OPS_CONFIG_READ in permissions)
    assertTrue(AdminPermission.OPS_CONFIG_WRITE in permissions)
    assertTrue(AdminPermission.FEEDBACK_READ in permissions)
    assertTrue(AdminPermission.FEEDBACK_REVIEW in permissions)
  }

  @Test
  fun `DELIVERY_WORKER should have 8 permissions`() {
    val permissions = RolePermissionMapping.getPermissions(AdminRoleModel.DELIVERY_WORKER)
    assertEquals(8, permissions.size)
    assertTrue(AdminPermission.DELIVERY_TASK in permissions)
    assertTrue(AdminPermission.ORDER_CREATE in permissions)
    assertTrue(AdminPermission.RECONCILIATION_SUBMIT in permissions)
    assertTrue(AdminPermission.ORDER_READ in permissions)
    assertTrue(AdminPermission.PRODUCT_READ in permissions)
    assertTrue(AdminPermission.FEEDBACK_REVIEW !in permissions)
    assertTrue(AdminPermission.OPS_CONFIG_READ !in permissions)
  }

  @Test
  fun `SUPER_ADMIN authorities should include ROLE_ADMIN and all permissions`() {
    val authorities = AdminRoleModel.SUPER_ADMIN.toPermissionAuthorities()
    assertTrue(authorities.any { it.authority == "ROLE_SUPER_ADMIN" })
    assertTrue(authorities.any { it.authority == "ROLE_ADMIN" })
    assertTrue(authorities.any { it.authority == "PERMISSION_ADMIN_USER_WRITE" })
  }

  @Test
  fun `ADMIN authorities should not include admin-user permissions`() {
    val authorities = AdminRoleModel.ADMIN.toPermissionAuthorities()
    assertTrue(authorities.any { it.authority == "ROLE_ADMIN" })
    assertTrue(authorities.none { it.authority == "PERMISSION_ADMIN_USER_WRITE" })
    assertTrue(authorities.none { it.authority == "PERMISSION_SYSTEM_CONFIG_WRITE" })
  }

  @Test
  fun `DELIVERY_WORKER authorities should carry worker task permissions`() {
    val authorities = AdminRoleModel.DELIVERY_WORKER.toPermissionAuthorities()
    assertTrue(authorities.any { it.authority == "ROLE_DELIVERY_WORKER" })
    assertTrue(authorities.any { it.authority == "PERMISSION_DELIVERY_TASK" })
    assertTrue(authorities.none { it.authority == "PERMISSION_ADMIN_USER_READ" })
  }
}
