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

package dev.yidafu.aqua.common.graphql

import dev.yidafu.aqua.common.security.UserPrincipal
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GraphQLValidatorTest {
  private val principal =
    UserPrincipal(
      id = 1L,
      _username = "alice",
      userType = "USER",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("PERMISSION_ORDER_READ")),
    )

  @BeforeEach
  fun setUp() {
    SecurityContextHolder.clearContext()
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `getCurrentUser should return principal when authenticated`() {
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    assertEquals(principal, GraphQLValidator.getCurrentUser())
  }

  @Test
  fun `getCurrentUser should return null when not authenticated`() {
    assertNull(GraphQLValidator.getCurrentUser())
  }

  @Test
  fun `getCurrentUser should return null for string principal`() {
    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("anonymous", null)
    assertNull(GraphQLValidator.getCurrentUser())
  }

  @Test
  fun `requireAuthenticated should return principal when present`() {
    assertEquals(principal, GraphQLValidator.requireAuthenticated(principal))
  }

  @Test
  fun `requireAuthenticated should throw when null`() {
    assertThrows<SecurityException> { GraphQLValidator.requireAuthenticated(null) }
  }

  @Test
  fun `hasRole should return true when role matches`() {
    assertTrue(GraphQLValidator.hasRole(principal, "ROLE_USER"))
  }

  @Test
  fun `hasRole should return false when role missing or principal null`() {
    assertFalse(GraphQLValidator.hasRole(principal, "ROLE_ADMIN"))
    assertFalse(GraphQLValidator.hasRole(null, "ROLE_USER"))
  }

  @Test
  fun `requireRole should not throw when role present`() {
    GraphQLValidator.requireRole(principal, "ROLE_USER")
  }

  @Test
  fun `requireRole should throw when role missing`() {
    assertThrows<SecurityException> { GraphQLValidator.requireRole(principal, "ROLE_ADMIN") }
  }

  @Test
  fun `requireRole should throw when principal null`() {
    assertThrows<SecurityException> { GraphQLValidator.requireRole(null, "ROLE_USER") }
  }
}
