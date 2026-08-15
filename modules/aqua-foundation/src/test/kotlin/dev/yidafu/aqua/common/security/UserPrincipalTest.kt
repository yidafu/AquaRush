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

import org.junit.jupiter.api.Test
import org.springframework.security.core.authority.SimpleGrantedAuthority
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserPrincipalTest {
  private fun principal(vararg authorities: String): UserPrincipal =
    UserPrincipal(
      id = 1L,
      _username = "alice",
      userType = "USER",
      _authorities = authorities.map { SimpleGrantedAuthority(it) },
    )

  @Test
  fun `getAuthorities should return configured authorities`() {
    val p = principal("ROLE_USER", "PERMISSION_ORDER_READ")
    assertEquals(2, p.authorities.size)
    assertEquals("ROLE_USER", p.authorities.first().authority)
  }

  @Test
  fun `getUsername should return username`() {
    assertEquals("alice", principal().username)
  }

  @Test
  fun `getPassword should return null`() {
    assertNull(principal().password)
  }

  @Test
  fun `account flags should all be true`() {
    val p = principal()
    assertTrue(p.isAccountNonExpired)
    assertTrue(p.isAccountNonLocked)
    assertTrue(p.isCredentialsNonExpired)
    assertTrue(p.isEnabled)
  }

  @Test
  fun `hasRole should match role authority`() {
    val p = principal("ROLE_USER")
    assertTrue(p.hasRole("USER"))
    assertFalse(p.hasRole("ADMIN"))
  }

  @Test
  fun `hasAuthority should match exact authority`() {
    val p = principal("PERMISSION_ORDER_READ")
    assertTrue(p.hasAuthority("PERMISSION_ORDER_READ"))
    assertFalse(p.hasAuthority("PERMISSION_ORDER_WRITE"))
  }

  @Test
  fun `id and userType should be accessible`() {
    val p = principal()
    assertEquals(1L, p.id)
    assertEquals("USER", p.userType)
  }

  @Test
  fun `getOpenId and getPhone should return empty strings`() {
    val p = principal()
    assertEquals("", p.getOpenId())
    assertEquals("", p.getPhone())
  }

  @Test
  fun `equals should consider fields`() {
    val a = principal()
    val b = principal()
    assertEquals(a, b)
  }
}
