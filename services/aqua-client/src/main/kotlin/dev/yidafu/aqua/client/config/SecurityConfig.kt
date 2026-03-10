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

import dev.yidafu.aqua.common.config.CustomAccessDeniedHandler
import dev.yidafu.aqua.common.config.CustomAuthenticationEntryPoint
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsUtils

@Configuration
@EnableWebSecurity
class SecurityConfig(
  private val customAccessDeniedHandler: CustomAccessDeniedHandler,
  private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint,
  private val userDetailsService: UserDetailsService,
) {
  private val log = LoggerFactory.getLogger(SecurityConfig::class.java)

  @Bean
  fun filterChain(
    http: HttpSecurity,
    jwtAuthenticationFilter: JwtAuthenticationFilter,
  ): SecurityFilterChain {
    http
      .authorizeHttpRequests { auth ->
        auth
          .requestMatchers("/api/auth/wechat/login")
          .permitAll()
          .requestMatchers("/api/auth/delivery/check")
          .permitAll()
          .requestMatchers("/api/debug/*")
          .permitAll()
          .requestMatchers("/api/*")
          .authenticated()
          // 允许 GraphQL 端点（用于调试和开发）
          .requestMatchers("/graphql")
          .authenticated()
          .requestMatchers(
            "/login",
            "/css/**",
            "/js/**",
            "/images/**",
            "/graphiql",
          ).permitAll()
          // Allow CORS preflight requests for GraphQL
          .requestMatchers(CorsUtils::isPreFlightRequest)
          .permitAll()
          .anyRequest()
          .permitAll()
      }.csrf {
        it.disable()
      }.cors { cors ->
        cors.configure(http)
      }.formLogin { form ->
        form.disable()
      }.logout { logout ->
        logout
          .logoutUrl("/logout")
          .logoutSuccessUrl("/login?logout")
          .permitAll()
      }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
      .exceptionHandling { exceptions ->
        exceptions
          .accessDeniedHandler(customAccessDeniedHandler)
          .authenticationEntryPoint(customAuthenticationEntryPoint)
      }
    return http.build()
  }

  @Bean
  fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
