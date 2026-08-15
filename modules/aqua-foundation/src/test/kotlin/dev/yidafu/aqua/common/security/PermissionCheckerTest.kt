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
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionCheckerTest {
  private val checker = PermissionChecker()

  @BeforeEach
  fun setUp() {
    SecurityContextHolder.clearContext()
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  private fun authenticate(vararg authorities: String) {
    val authentication =
      UsernamePasswordAuthenticationToken(
        "admin",
        null,
        authorities.map { SimpleGrantedAuthority(it) },
      )
    SecurityContextHolder.getContext().authentication = authentication
  }

  @Test
  fun `hasPermission should return true when authority present`() {
    authenticate("PERMISSION_ORDER_READ")
    assertTrue(checker.hasPermission(AdminPermission.ORDER_READ))
  }

  @Test
  fun `hasPermission should return false when authority missing`() {
    authenticate("PERMISSION_ORDER_READ")
    assertFalse(checker.hasPermission(AdminPermission.ORDER_WRITE))
  }

  @Test
  fun `hasPermission should return false when no authentication`() {
    assertFalse(checker.hasPermission(AdminPermission.ORDER_READ))
  }

  @Test
  fun `hasAnyPermission should return true when any matches`() {
    authenticate("PERMISSION_ORDER_READ")
    assertTrue(
      checker.hasAnyPermission(
        AdminPermission.ORDER_WRITE,
        AdminPermission.ORDER_READ,
      ),
    )
  }

  @Test
  fun `hasAnyPermission should return false when none match`() {
    authenticate("PERMISSION_ORDER_READ")
    assertFalse(checker.hasAnyPermission(AdminPermission.ORDER_WRITE, AdminPermission.PRODUCT_WRITE))
  }

  @Test
  fun `hasAllPermissions should return true when all present`() {
    authenticate("PERMISSION_ORDER_READ", "PERMISSION_ORDER_WRITE")
    assertTrue(
      checker.hasAllPermissions(
        setOf(AdminPermission.ORDER_READ, AdminPermission.ORDER_WRITE),
      ),
    )
  }

  @Test
  fun `hasAllPermissions should return false when one missing`() {
    authenticate("PERMISSION_ORDER_READ")
    assertFalse(
      checker.hasAllPermissions(
        setOf(AdminPermission.ORDER_READ, AdminPermission.ORDER_WRITE),
      ),
    )
  }

  @Test
  fun `hasPermission with string should match exact authority`() {
    authenticate("PERMISSION_ORDER_READ")
    assertTrue(checker.hasPermission("PERMISSION_ORDER_READ"))
    assertFalse(checker.hasPermission("PERMISSION_OTHER"))
  }
}
