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

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.springframework.security.core.authority.SimpleGrantedAuthority

class UserPrincipalTest :
  FunSpec({
    fun principal(vararg authorities: String): UserPrincipal =
      UserPrincipal(
        id = 1L,
        _username = "alice",
        userType = "USER",
        _authorities = authorities.map { SimpleGrantedAuthority(it) },
      )

    test("getAuthorities should return configured authorities") {
      val p = principal("ROLE_USER", "PERMISSION_ORDER_READ")
      p.authorities.size shouldBe 2
      p.authorities.first().authority shouldBe "ROLE_USER"
    }
    test("getUsername should return username") {
      principal().username shouldBe "alice"
    }
    test("getPassword should return null") {
      principal().password.shouldBeNull()
    }
    test("account flags should all be true") {
      val p = principal()
      p.isAccountNonExpired shouldBe true
      p.isAccountNonLocked shouldBe true
      p.isCredentialsNonExpired shouldBe true
      p.isEnabled shouldBe true
    }
    test("hasRole should match role authority") {
      val p = principal("ROLE_USER")
      p.hasRole("USER") shouldBe true
      p.hasRole("ADMIN") shouldBe false
    }
    test("hasAuthority should match exact authority") {
      val p = principal("PERMISSION_ORDER_READ")
      p.hasAuthority("PERMISSION_ORDER_READ") shouldBe true
      p.hasAuthority("PERMISSION_ORDER_WRITE") shouldBe false
    }
    test("id and userType should be accessible") {
      val p = principal()
      p.id shouldBe 1L
      p.userType shouldBe "USER"
    }
    test("getOpenId and getPhone should return empty strings") {
      val p = principal()
      p.getOpenId() shouldBe ""
      p.getPhone() shouldBe ""
    }
    test("equals should consider fields") {
      val a = principal()
      val b = principal()
      b shouldBe a
    }
  })
