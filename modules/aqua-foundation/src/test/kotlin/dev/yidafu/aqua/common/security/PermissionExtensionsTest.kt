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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PermissionExtensionsTest :
  FunSpec({

    test("toSimpleGrantedAuthority should prefix permission name") {
      val authority = AdminPermission.ORDER_READ.toSimpleGrantedAuthority()
      authority.authority shouldBe "PERMISSION_ORDER_READ"
    }
    test("toSimpleGrantedAuthorities should map all permissions") {
      val authorities = listOf(AdminPermission.ORDER_READ, AdminPermission.PRODUCT_WRITE).toSimpleGrantedAuthorities()
      authorities.size shouldBe 2
      authorities[0].authority shouldBe "PERMISSION_ORDER_READ"
      authorities[1].authority shouldBe "PERMISSION_PRODUCT_WRITE"
    }
    test("toPermissionAuthorities should include role authority") {
      val authorities = AdminRoleModel.ADMIN.toPermissionAuthorities()
      authorities.any { it.authority == "ROLE_ADMIN" } shouldBe true
      authorities.any { it.authority == "PERMISSION_ORDER_READ" } shouldBe true
    }
    test("toPermissionAuthorities for super admin should include ROLE_ADMIN") {
      val authorities = AdminRoleModel.SUPER_ADMIN.toPermissionAuthorities()
      authorities.any { it.authority == "ROLE_SUPER_ADMIN" } shouldBe true
      authorities.any { it.authority == "ROLE_ADMIN" } shouldBe true
    }
    test("toPermissionAuthorities for delivery worker should not include admin roles") {
      val authorities = AdminRoleModel.DELIVERY_WORKER.toPermissionAuthorities()
      authorities.none { it.authority == "ROLE_ADMIN" } shouldBe true
      authorities.any { it.authority == "ROLE_DELIVERY_WORKER" } shouldBe true
    }
  })
