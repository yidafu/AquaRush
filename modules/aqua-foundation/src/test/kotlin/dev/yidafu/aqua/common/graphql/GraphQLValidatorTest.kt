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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class GraphQLValidatorTest :
  FunSpec({
    val principal =
      UserPrincipal(
        id = 1L,
        _username = "alice",
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("PERMISSION_ORDER_READ")),
      )

    beforeTest {
      SecurityContextHolder.clearContext()
    }
    afterTest {
      SecurityContextHolder.clearContext()
    }
    test("getCurrentUser should return principal when authenticated") {
      SecurityContextHolder.getContext().authentication =
        UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
      GraphQLValidator.getCurrentUser() shouldBe principal
    }
    test("getCurrentUser should return null when not authenticated") {
      GraphQLValidator.getCurrentUser().shouldBeNull()
    }
    test("getCurrentUser should return null for string principal") {
      SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken("anonymous", null)
      GraphQLValidator.getCurrentUser().shouldBeNull()
    }
    test("requireAuthenticated should return principal when present") {
      GraphQLValidator.requireAuthenticated(principal) shouldBe principal
    }
    test("requireAuthenticated should throw when null") {
      shouldThrow<SecurityException> { GraphQLValidator.requireAuthenticated(null) }
    }
    test("hasRole should return true when role matches") {
      GraphQLValidator.hasRole(principal, "ROLE_USER") shouldBe true
    }
    test("hasRole should return false when role missing or principal null") {
      GraphQLValidator.hasRole(principal, "ROLE_ADMIN") shouldBe false
      GraphQLValidator.hasRole(null, "ROLE_USER") shouldBe false
    }
    test("requireRole should not throw when role present") {
      GraphQLValidator.requireRole(principal, "ROLE_USER")
    }
    test("requireRole should throw when role missing") {
      shouldThrow<SecurityException> { GraphQLValidator.requireRole(principal, "ROLE_ADMIN") }
    }
    test("requireRole should throw when principal null") {
      shouldThrow<SecurityException> { GraphQLValidator.requireRole(null, "ROLE_USER") }
    }
  })
