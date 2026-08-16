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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class PermissionCheckerTest :
  FunSpec({
    val checker = PermissionChecker()

    fun authenticate(vararg authorities: String) {
      val authentication =
        UsernamePasswordAuthenticationToken(
          "admin",
          null,
          authorities.map { SimpleGrantedAuthority(it) },
        )
      SecurityContextHolder.getContext().authentication = authentication
    }

    beforeTest {
      SecurityContextHolder.clearContext()
    }
    afterTest {
      SecurityContextHolder.clearContext()
    }
    test("hasPermission should return true when authority present") {
      authenticate("PERMISSION_ORDER_READ")
      checker.hasPermission(AdminPermission.ORDER_READ) shouldBe true
    }
    test("hasPermission should return false when authority missing") {
      authenticate("PERMISSION_ORDER_READ")
      checker.hasPermission(AdminPermission.ORDER_WRITE) shouldBe false
    }
    test("hasPermission should return false when no authentication") {
      checker.hasPermission(AdminPermission.ORDER_READ) shouldBe false
    }
    test("hasAnyPermission should return true when any matches") {
      authenticate("PERMISSION_ORDER_READ")
      checker.hasAnyPermission(
        AdminPermission.ORDER_WRITE,
        AdminPermission.ORDER_READ,
      ) shouldBe true
    }
    test("hasAnyPermission should return false when none match") {
      authenticate("PERMISSION_ORDER_READ")
      checker.hasAnyPermission(AdminPermission.ORDER_WRITE, AdminPermission.PRODUCT_WRITE) shouldBe false
    }
    test("hasAllPermissions should return true when all present") {
      authenticate("PERMISSION_ORDER_READ", "PERMISSION_ORDER_WRITE")
      checker.hasAllPermissions(
        setOf(AdminPermission.ORDER_READ, AdminPermission.ORDER_WRITE),
      ) shouldBe true
    }
    test("hasAllPermissions should return false when one missing") {
      authenticate("PERMISSION_ORDER_READ")
      checker.hasAllPermissions(
        setOf(AdminPermission.ORDER_READ, AdminPermission.ORDER_WRITE),
      ) shouldBe false
    }
    test("hasPermission with string should match exact authority") {
      authenticate("PERMISSION_ORDER_READ")
      checker.hasPermission("PERMISSION_ORDER_READ") shouldBe true
      checker.hasPermission("PERMISSION_OTHER") shouldBe false
    }
  })
