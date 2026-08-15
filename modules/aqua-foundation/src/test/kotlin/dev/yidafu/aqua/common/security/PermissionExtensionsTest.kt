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

class PermissionExtensionsTest {
  @Test
  fun `toSimpleGrantedAuthority should prefix permission name`() {
    val authority = AdminPermission.ORDER_READ.toSimpleGrantedAuthority()
    assertEquals("PERMISSION_ORDER_READ", authority.authority)
  }

  @Test
  fun `toSimpleGrantedAuthorities should map all permissions`() {
    val authorities = listOf(AdminPermission.ORDER_READ, AdminPermission.PRODUCT_WRITE).toSimpleGrantedAuthorities()
    assertEquals(2, authorities.size)
    assertEquals("PERMISSION_ORDER_READ", authorities[0].authority)
    assertEquals("PERMISSION_PRODUCT_WRITE", authorities[1].authority)
  }

  @Test
  fun `toPermissionAuthorities should include role authority`() {
    val authorities = AdminRoleModel.ADMIN.toPermissionAuthorities()
    assertTrue(authorities.any { it.authority == "ROLE_ADMIN" })
    assertTrue(authorities.any { it.authority == "PERMISSION_ORDER_READ" })
  }

  @Test
  fun `toPermissionAuthorities for super admin should include ROLE_ADMIN`() {
    val authorities = AdminRoleModel.SUPER_ADMIN.toPermissionAuthorities()
    assertTrue(authorities.any { it.authority == "ROLE_SUPER_ADMIN" })
    assertTrue(authorities.any { it.authority == "ROLE_ADMIN" })
  }

  @Test
  fun `toPermissionAuthorities for delivery worker should not include admin roles`() {
    val authorities = AdminRoleModel.DELIVERY_WORKER.toPermissionAuthorities()
    assertTrue(authorities.none { it.authority == "ROLE_ADMIN" })
    assertTrue(authorities.any { it.authority == "ROLE_DELIVERY_WORKER" })
  }
}
