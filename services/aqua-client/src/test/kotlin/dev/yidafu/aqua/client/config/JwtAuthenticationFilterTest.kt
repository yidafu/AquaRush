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

package dev.yidafu.aqua.client.config

import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest :
  FunSpec({
    var jwtTokenService: JwtTokenService = mockk()
    var customUserDetailsService: ClientUserDetailsService = mockk()
    var filter: JwtAuthenticationFilter = mockk()

    fun requestWithToken(
      token: String?,
      uri: String = "/api/orders",
    ): HttpServletRequest {
      val request = mockk<HttpServletRequest>(relaxed = true)
      every { request.getHeader("Authorization") } returns if (token != null) "Bearer $token" else null
      every { request.getParameter("token") } returns null
      every { request.method } returns "GET"
      every { request.requestURI } returns uri
      every { request.dispatcherType } returns DispatcherType.REQUEST
      every { request.getAttribute(any()) } returns null
      return request
    }

    beforeTest {
      jwtTokenService = mockk(relaxed = true)
      customUserDetailsService = mockk(relaxed = true)
      filter = JwtAuthenticationFilter(jwtTokenService, customUserDetailsService)
    }
    afterTest {
      SecurityContextHolder.clearContext()
    }
    test("doFilter should skip authentication on public endpoints") {
      // Given
      val request = requestWithToken("token123", uri = "/login")
      val response = mockk<HttpServletResponse>(relaxed = true)
      val chain = mockk<FilterChain>(relaxed = true)

      // When
      filter.doFilter(request, response, chain)

      // Then
      verify { chain.doFilter(request, response) }
      SecurityContextHolder.getContext().authentication.shouldBeNull()
    }
    test("doFilter should continue chain when no token") {
      // Given
      val request = requestWithToken(null)
      val response = mockk<HttpServletResponse>(relaxed = true)
      val chain = mockk<FilterChain>(relaxed = true)

      // When
      filter.doFilter(request, response, chain)

      // Then
      verify { chain.doFilter(request, response) }
      SecurityContextHolder.getContext().authentication.shouldBeNull()
    }
    test("doFilter should authenticate valid token") {
      // Given
      val request = requestWithToken("token123")
      val response = mockk<HttpServletResponse>(relaxed = true)
      val chain = mockk<FilterChain>(relaxed = true)
      every { jwtTokenService.extractUsername("token123") } returns "testuser"
      every { customUserDetailsService.loadUserByUsername("testuser") } returns
        UserPrincipal(1L, "testuser", "USER", listOf(SimpleGrantedAuthority("ROLE_USER")))
      every { jwtTokenService.validateToken("token123", any()) } returns true

      // When
      filter.doFilter(request, response, chain)

      // Then
      verify { chain.doFilter(request, response) }
      val authentication = SecurityContextHolder.getContext().authentication
      authentication.shouldNotBeNull()
      (authentication!!.principal is UserPrincipal) shouldBe true
    }
    test("doFilter should skip authentication for invalid token") {
      // Given
      val request = requestWithToken("token-bad")
      val response = mockk<HttpServletResponse>(relaxed = true)
      val chain = mockk<FilterChain>(relaxed = true)
      every { jwtTokenService.extractUsername("token-bad") } returns "testuser"
      every { customUserDetailsService.loadUserByUsername("testuser") } returns
        UserPrincipal(1L, "testuser", "USER", emptyList())
      every { jwtTokenService.validateToken("token-bad", any()) } returns false

      // When
      filter.doFilter(request, response, chain)

      // Then
      verify { chain.doFilter(request, response) }
      SecurityContextHolder.getContext().authentication.shouldBeNull()
    }
    test("doFilter should skip authentication when username blank") {
      // Given
      val request = requestWithToken("token-blank")
      val response = mockk<HttpServletResponse>(relaxed = true)
      val chain = mockk<FilterChain>(relaxed = true)
      every { jwtTokenService.extractUsername("token-blank") } returns null

      // When
      filter.doFilter(request, response, chain)

      // Then
      verify { chain.doFilter(request, response) }
      SecurityContextHolder.getContext().authentication.shouldBeNull()
    }
    test("doFilter should rethrow chain exceptions") {
      // Given
      val request = requestWithToken(null)
      val response = mockk<HttpServletResponse>(relaxed = true)
      val chain = mockk<FilterChain>(relaxed = true)
      every { chain.doFilter(request, response) } throws IllegalStateException("chain down")

      // When & Then
      shouldThrow<IllegalStateException> { filter.doFilter(request, response, chain) }
    }
  })
