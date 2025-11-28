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

package dev.yidafu.aqua.user.service

import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.model.User
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
  private val userRepository: UserRepository,
) : UserDetailsService {
  override fun loadUserByUsername(username: String): UserDetails {
    // For JWT authentication, username is the OpenID
    val user =
      userRepository.findByWechatOpenId(username)
        ?: throw UsernameNotFoundException("User not found with openid: $username")

    // Determine user type and authorities based on user data
    val userType = determineUserType(user)
    val authorities = determineAuthorities(user, userType)

    return UserPrincipal(
      id = user.id!!,
      _username = user.wechatOpenId,
      userType = userType,
      _authorities = authorities,
    )
  }

  private fun determineUserType(user: User): String {
    // Check if user is an admin (this could be based on a field in User entity)
    // For now, we'll use a simple logic - you can extend this based on your requirements
    return if (user.phone?.startsWith("admin") == true) {
      "ADMIN"
    } else if (user.phone?.startsWith("worker") == true) {
      "WORKER"
    } else {
      "USER"
    }
  }

  private fun determineAuthorities(
    user: User,
    userType: String,
  ): List<SimpleGrantedAuthority> {
    val authorities = mutableListOf<SimpleGrantedAuthority>()

    // Add role based on user type
    authorities.add(SimpleGrantedAuthority("ROLE_$userType"))

    // Add specific permissions based on user type
    when (userType) {
      "ADMIN" -> {
        authorities.add(SimpleGrantedAuthority("PERMISSION_USER_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_USER_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PRODUCT_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PRODUCT_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_DELIVERY_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_DELIVERY_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PAYMENT_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PAYMENT_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_STATISTICS_READ"))
      }
      "WORKER" -> {
        authorities.add(SimpleGrantedAuthority("PERMISSION_DELIVERY_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_DELIVERY_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_READ"))
      }
      "USER" -> {
        authorities.add(SimpleGrantedAuthority("PERMISSION_USER_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_USER_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_READ"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_ORDER_WRITE"))
        authorities.add(SimpleGrantedAuthority("PERMISSION_PRODUCT_READ"))
      }
    }

    return authorities
  }
}
