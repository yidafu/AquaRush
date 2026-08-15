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

package dev.yidafu.aqua.admin.config

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.api.service.user.UserService
import dev.yidafu.aqua.common.domain.model.AdminModel
import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.DispatcherType
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import java.time.LocalDateTime
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtAuthenticationFilterTest {
  private lateinit var jwtTokenService: JwtTokenService
  private lateinit var customUserDetailsService: UserDetailsService
  private lateinit var userService: UserService
  private lateinit var adminService: AdminService
  private lateinit var filter: JwtAuthenticationFilter

  private fun createSampleAdmin(status: AdminAccountStatus = AdminAccountStatus.ACTIVE): AdminModel =
    AdminModel(
      id = 1L,
      username = "admin",
      passwordHash = "hashed_password",
      realName = "管理员",
      phone = "13900139000",
      role = AdminRoleModel.ADMIN,
      accountStatus = status,
      createdAt = LocalDateTime.now(),
      updatedAt = LocalDateTime.now(),
    )

  private fun requestWithToken(
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

  @BeforeEach
  fun setUp() {
    jwtTokenService = mockk(relaxed = true)
    customUserDetailsService = mockk(relaxed = true)
    userService = mockk(relaxed = true)
    adminService = mockk(relaxed = true)
    filter = JwtAuthenticationFilter(jwtTokenService, customUserDetailsService, userService, adminService)
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `doFilter should continue chain when no token`() {
    // Given
    val request = requestWithToken(null)
    val response = mockk<HttpServletResponse>(relaxed = true)
    val chain = mockk<FilterChain>(relaxed = true)

    // When
    filter.doFilter(request, response, chain)

    // Then
    verify { chain.doFilter(request, response) }
    assertNull(SecurityContextHolder.getContext().authentication)
  }

  @Test
  fun `doFilter should authenticate admin by username`() {
    // Given
    val request = requestWithToken("token123")
    val response = mockk<HttpServletResponse>(relaxed = true)
    val chain = mockk<FilterChain>(relaxed = true)
    every { jwtTokenService.extractUsername("token123") } returns "admin"
    every { userService.findByWechatOpenId("admin") } returns null
    every { adminService.findByUsername("admin") } returns createSampleAdmin()
    every { customUserDetailsService.loadUserByUsername("admin") } returns
      UserPrincipal(1L, "admin", "ADMIN", emptyList())
    every { jwtTokenService.validateToken("token123", any()) } returns true

    // When
    filter.doFilter(request, response, chain)

    // Then
    verify { chain.doFilter(request, response) }
    val authentication = SecurityContextHolder.getContext().authentication
    assertNotNull(authentication)
    assertTrue(authentication!!.principal is UserPrincipal)
  }

  @Test
  fun `doFilter should authenticate wechat openid with admin association`() {
    // Given
    val request = requestWithToken("token456")
    val response = mockk<HttpServletResponse>(relaxed = true)
    val chain = mockk<FilterChain>(relaxed = true)
    every { jwtTokenService.extractUsername("token456") } returns "openid-1"
    every { userService.findByWechatOpenId("openid-1") } returns
      UserModel(id = 1L, wechatOpenId = "openid-1", email = "", lastLoginAt = LocalDateTime.now())
    every { adminService.findByUserId(1L) } returns createSampleAdmin()

    // When
    filter.doFilter(request, response, chain)

    // Then
    verify { chain.doFilter(request, response) }
    assertNotNull(SecurityContextHolder.getContext().authentication)
  }

  @Test
  fun `doFilter should skip authentication for disabled admin`() {
    // Given
    val request = requestWithToken("token789")
    val response = mockk<HttpServletResponse>(relaxed = true)
    val chain = mockk<FilterChain>(relaxed = true)
    every { jwtTokenService.extractUsername("token789") } returns "admin"
    every { userService.findByWechatOpenId("admin") } returns null
    every { adminService.findByUsername("admin") } returns createSampleAdmin(AdminAccountStatus.DISABLED)

    // When
    filter.doFilter(request, response, chain)

    // Then
    verify { chain.doFilter(request, response) }
    assertNull(SecurityContextHolder.getContext().authentication)
  }

  @Test
  fun `doFilter should skip authentication when username cannot be extracted`() {
    // Given
    val request = requestWithToken("token-invalid")
    val response = mockk<HttpServletResponse>(relaxed = true)
    val chain = mockk<FilterChain>(relaxed = true)
    every { jwtTokenService.extractUsername("token-invalid") } returns null

    // When
    filter.doFilter(request, response, chain)

    // Then
    verify { chain.doFilter(request, response) }
    assertNull(SecurityContextHolder.getContext().authentication)
  }

  @Test
  fun `doFilter should read token from query parameter`() {
    // Given
    val request = requestWithToken(null)
    every { request.getParameter("token") } returns "query-token"
    every { jwtTokenService.extractUsername("query-token") } returns "admin"
    every { userService.findByWechatOpenId("admin") } returns null
    every { adminService.findByUsername("admin") } returns createSampleAdmin()
    every { customUserDetailsService.loadUserByUsername("admin") } returns
      UserPrincipal(1L, "admin", "ADMIN", emptyList())
    every { jwtTokenService.validateToken("query-token", any()) } returns true
    val response = mockk<HttpServletResponse>(relaxed = true)
    val chain = mockk<FilterChain>(relaxed = true)

    // When
    filter.doFilter(request, response, chain)

    // Then
    assertNotNull(SecurityContextHolder.getContext().authentication)
  }

  @Test
  fun `doFilter should rethrow chain exceptions`() {
    // Given
    val request = requestWithToken(null)
    val response = mockk<HttpServletResponse>(relaxed = true)
    val chain = mockk<FilterChain>(relaxed = true)
    every { chain.doFilter(request, response) } throws IllegalStateException("chain down")

    // When & Then
    assertFailsWith<IllegalStateException> { filter.doFilter(request, response, chain) }
  }
}
