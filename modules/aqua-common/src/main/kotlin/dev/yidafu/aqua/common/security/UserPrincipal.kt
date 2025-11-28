/*
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

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserPrincipal(
  val id: Long,
  private val _username: String,
  val userType: String, // USER, WORKER, ADMIN
  private val _authorities: Collection<GrantedAuthority>,
) : UserDetails {
  override fun getAuthorities(): Collection<GrantedAuthority> = _authorities

  override fun getPassword(): String? = null // No password for JWT-based authentication

  override fun getUsername(): String = _username

  override fun isAccountNonExpired(): Boolean = true

  override fun isAccountNonLocked(): Boolean = true

  override fun isCredentialsNonExpired(): Boolean = true

  override fun isEnabled(): Boolean = true

  fun hasRole(role: String): Boolean = _authorities.any { it.authority == "ROLE_$role" }

  fun hasAuthority(authority: String): Boolean = _authorities.any { it.authority == authority }

  fun getOpenId(): String = ""

  fun getPhone(): String = ""
}
