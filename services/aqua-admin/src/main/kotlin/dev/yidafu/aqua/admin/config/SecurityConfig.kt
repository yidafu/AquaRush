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

package dev.yidafu.aqua.admin.config

import dev.yidafu.aqua.common.config.CustomAccessDeniedHandler
import dev.yidafu.aqua.common.config.CustomAuthenticationEntryPoint
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
  private val jwtAuthenticationFilter: JwtAuthenticationFilter,
  private val customAccessDeniedHandler: CustomAccessDeniedHandler,
  private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
  private val adminUserDetailsService: AdminUserDetailsService,
) {
  private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http
      .authorizeHttpRequests { auth ->
        auth
          .requestMatchers(
            "/login",
            "/api/auth/login",
            "/api/auth/delivery/login",
            "/css/**",
            "/js/**",
            "/images/**",
            "/graphiql",
          ).permitAll()
          .requestMatchers("/api/*")
          .authenticated()
          .requestMatchers("/graphql")
          .authenticated()
      }.csrf {
        it.disable()
      }.formLogin { form ->
        form
          .loginPage("/login")
          .defaultSuccessUrl("/admin", true)
          .permitAll()
      }.logout { logout ->
        logout
          .logoutUrl("/logout")
          .logoutSuccessUrl("/login?logout")
          .permitAll()
      }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
      .exceptionHandling { authenticationException ->
        authenticationException
          .accessDeniedHandler(customAccessDeniedHandler)
          .authenticationEntryPoint(customAuthenticationEntryPoint)
      }
    return http.build()
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder {
    val encoder = BCryptPasswordEncoder()
    log.info(
      "passwordEncoder " + encoder.encode("123456") + " " +
        encoder.matches(
          "123456",
          "\$2a\$10\$7EhtjPxrF0/nxhnxk7HBQ..yAiZtOyqDJ6BQOy53Dnh.sNJebbm0C",
        ),
    )
    log.info(
      "passwordEncoder " + encoder.encode("admin") + " " +
        encoder.matches(
          "admin",
          "\$2a\$10\$JnQeCYaWpLlN6KPgF.aAluIbGWDjYrjKMCNaUF964NwL4ATrYNTba",
        ),
    )

    return encoder
  }

  @Bean
  fun authenticationProvider(): DaoAuthenticationProvider {
    val provider = DaoAuthenticationProvider(adminUserDetailsService)
    provider.setPasswordEncoder(passwordEncoder())
    return provider
  }

  @Bean
  fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager {
    assert(authenticationConfiguration.authenticationManager != null) { "Authentication Manager not configured" }
    return authenticationConfiguration.authenticationManager
  }
}
