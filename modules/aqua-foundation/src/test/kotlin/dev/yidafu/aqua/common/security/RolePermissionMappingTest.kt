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

/**
 * 验证角色→权限矩阵(PRD 01-overview.md §角色权限矩阵)与角色→authority 映射。
 */
class RolePermissionMappingTest :
  FunSpec({

    test("permission catalog should contain exactly 42 codes") {
      // 40 → 42:G93 微信支付对账新增 PAYMENT_RECONCILIATION_READ/EXECUTE(F-ORD-031,2026-10)
      AdminPermission.entries.size shouldBe 42
    }
    test("SUPER_ADMIN should have all permissions") {
      val permissions = RolePermissionMapping.getPermissions(AdminRoleModel.SUPER_ADMIN)
      permissions shouldBe AdminPermission.entries.toSet()
    }
    test("ADMIN should have 37 permissions excluding admin-user and system-config ops") {
      val permissions = RolePermissionMapping.getPermissions(AdminRoleModel.ADMIN)
      permissions.size shouldBe 37
      (AdminPermission.ADMIN_USER_READ !in permissions) shouldBe true
      (AdminPermission.ADMIN_USER_WRITE !in permissions) shouldBe true
      (AdminPermission.SYSTEM_CONFIG_READ !in permissions) shouldBe true
      (AdminPermission.SYSTEM_CONFIG_WRITE !in permissions) shouldBe true
      (AdminPermission.NOTIFICATION_WRITE !in permissions) shouldBe true
      // 管理员兼送水(F-USR-023):DELIVERY_TASK/ORDER_CREATE/RECONCILIATION_SUBMIT 已授予管理员
      // (代客下单送水工端入口 ADMIN 可见;日报提交为本人日报) —— PRD 01-overview §角色权限矩阵
      (AdminPermission.DELIVERY_TASK in permissions) shouldBe true
      (AdminPermission.ORDER_CREATE in permissions) shouldBe true
      (AdminPermission.RECONCILIATION_SUBMIT in permissions) shouldBe true
      // 运营配置/意见反馈/营销 6 码属于管理员
      (AdminPermission.OPS_CONFIG_READ in permissions) shouldBe true
      (AdminPermission.OPS_CONFIG_WRITE in permissions) shouldBe true
      (AdminPermission.FEEDBACK_READ in permissions) shouldBe true
      (AdminPermission.FEEDBACK_REVIEW in permissions) shouldBe true
      (AdminPermission.MKT_READ in permissions) shouldBe true
      (AdminPermission.MKT_WRITE in permissions) shouldBe true
      // 支付对账(F-ORD-031/G93):独立权限码,管理员可查可执行
      (AdminPermission.PAYMENT_RECONCILIATION_READ in permissions) shouldBe true
      (AdminPermission.PAYMENT_RECONCILIATION_EXECUTE in permissions) shouldBe true
    }
    test("DELIVERY_WORKER should have 8 permissions") {
      val permissions = RolePermissionMapping.getPermissions(AdminRoleModel.DELIVERY_WORKER)
      permissions.size shouldBe 8
      (AdminPermission.DELIVERY_TASK in permissions) shouldBe true
      (AdminPermission.ORDER_CREATE in permissions) shouldBe true
      (AdminPermission.RECONCILIATION_SUBMIT in permissions) shouldBe true
      (AdminPermission.ORDER_READ in permissions) shouldBe true
      (AdminPermission.PRODUCT_READ in permissions) shouldBe true
      (AdminPermission.FEEDBACK_REVIEW !in permissions) shouldBe true
      (AdminPermission.OPS_CONFIG_READ !in permissions) shouldBe true
      // 支付对账不授送水工(F-ORD-031:管理后台资金对账)
      (AdminPermission.PAYMENT_RECONCILIATION_READ !in permissions) shouldBe true
      (AdminPermission.PAYMENT_RECONCILIATION_EXECUTE !in permissions) shouldBe true
    }
    test("SUPER_ADMIN authorities should include ROLE_ADMIN and all permissions") {
      val authorities = AdminRoleModel.SUPER_ADMIN.toPermissionAuthorities()
      authorities.any { it.authority == "ROLE_SUPER_ADMIN" } shouldBe true
      authorities.any { it.authority == "ROLE_ADMIN" } shouldBe true
      authorities.any { it.authority == "PERMISSION_ADMIN_USER_WRITE" } shouldBe true
    }
    test("ADMIN authorities should not include admin-user permissions") {
      val authorities = AdminRoleModel.ADMIN.toPermissionAuthorities()
      authorities.any { it.authority == "ROLE_ADMIN" } shouldBe true
      authorities.none { it.authority == "PERMISSION_ADMIN_USER_WRITE" } shouldBe true
      authorities.none { it.authority == "PERMISSION_SYSTEM_CONFIG_WRITE" } shouldBe true
    }
    test("DELIVERY_WORKER authorities should carry worker task permissions") {
      val authorities = AdminRoleModel.DELIVERY_WORKER.toPermissionAuthorities()
      authorities.any { it.authority == "ROLE_DELIVERY_WORKER" } shouldBe true
      authorities.any { it.authority == "PERMISSION_DELIVERY_TASK" } shouldBe true
      authorities.none { it.authority == "PERMISSION_ADMIN_USER_READ" } shouldBe true
    }
  })
