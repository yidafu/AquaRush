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

package dev.yidafu.aqua.client.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager

@Configuration
class FormLoginUserConfig {
  @Bean
  @Primary
  fun formLoginUserDetailsService(passwordEncoder: PasswordEncoder): UserDetailsService {
    val userDetailsService = InMemoryUserDetailsManager()

    // Create admin user
    val adminUser = User.builder()
      .username("admin")
      .password(passwordEncoder.encode("123456"))
      .roles("ADMIN")
      .authorities(
        "PERMISSION_USER_READ", "PERMISSION_USER_WRITE",
        "PERMISSION_ORDER_READ", "PERMISSION_ORDER_WRITE",
        "PERMISSION_PRODUCT_READ", "PERMISSION_PRODUCT_WRITE",
        "PERMISSION_DELIVERY_READ", "PERMISSION_DELIVERY_WRITE",
        "PERMISSION_PAYMENT_READ", "PERMISSION_PAYMENT_WRITE",
        "PERMISSION_STATISTICS_READ"
      )
      .build()

    // Create regular user
    val normalUser = User.builder()
      .username("user")
      .password(passwordEncoder.encode("user123"))
      .roles("USER")
      .authorities(
        "PERMISSION_USER_READ", "PERMISSION_USER_WRITE",
        "PERMISSION_ORDER_READ", "PERMISSION_ORDER_WRITE",
        "PERMISSION_PRODUCT_READ"
      )
      .build()

    // Create delivery worker
    val workerUser = User.builder()
      .username("worker")
      .password(passwordEncoder.encode("worker123"))
      .roles("WORKER")
      .authorities(
        "PERMISSION_DELIVERY_READ", "PERMISSION_DELIVERY_WRITE",
        "PERMISSION_ORDER_READ"
      )
      .build()

    userDetailsService.createUser(adminUser)
    userDetailsService.createUser(normalUser)
    userDetailsService.createUser(workerUser)

    return userDetailsService
  }
}
