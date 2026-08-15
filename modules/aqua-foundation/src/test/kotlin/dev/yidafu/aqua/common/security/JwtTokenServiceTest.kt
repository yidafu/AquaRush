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

import dev.yidafu.aqua.common.exception.JwtTokenException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.nio.charset.StandardCharsets
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JwtTokenServiceTest {
  private lateinit var service: JwtTokenService

  private val secret = "unit-test-secret-key-that-is-at-least-256-bits-long!!"

  private fun userPrincipal(
    id: Long = 1L,
    username: String = "user1",
  ): UserPrincipal =
    UserPrincipal(
      id = id,
      _username = username,
      userType = "USER",
      _authorities = listOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("PERMISSION_ORDER_READ")),
    )

  private fun signingKey() = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

  @BeforeEach
  fun setUp() {
    service = JwtTokenService()
    val field = JwtTokenService::class.java.getDeclaredField("secret")
    field.isAccessible = true
    field.set(service, secret)
  }

  @Test
  fun `generateAccessToken should produce a valid JWT`() {
    val token = service.generateAccessToken(userPrincipal())
    assertEquals(3, token.split(".").size)
    assertNotNull(token)
  }

  @Test
  fun `generateRefreshToken and generateWorkerToken should produce tokens`() {
    val principal = userPrincipal()
    assertNotNull(service.generateRefreshToken(principal))
    assertNotNull(service.generateWorkerToken(principal))
  }

  @Test
  fun `extractUsername should return subject`() {
    val token = service.generateAccessToken(userPrincipal(username = "alice"))
    assertEquals("alice", service.extractUsername(token))
  }

  @Test
  fun `validateToken should return true for matching user`() {
    val principal = userPrincipal(username = "alice")
    val token = service.generateAccessToken(principal)
    assertTrue(service.validateToken(token, principal))
  }

  @Test
  fun `validateToken should return false for mismatched user`() {
    val token = service.generateAccessToken(userPrincipal(username = "alice"))
    assertFalse(service.validateToken(token, userPrincipal(username = "bob")))
  }

  @Test
  fun `isTokenExpired should return false for fresh token`() {
    val token = service.generateAccessToken(userPrincipal())
    assertFalse(service.isTokenExpired(token))
  }

  @Test
  fun `extractClaim should extract custom claims`() {
    val token = service.generateAccessToken(userPrincipal())
    val subject = service.extractClaim(token) { claims -> claims.subject }
    assertEquals("user1", subject)
  }

  @Test
  fun `isTokenExpired should throw for expired token`() {
    val expiredToken =
      Jwts
        .builder()
        .setSubject("user1")
        .setIssuedAt(Date(System.currentTimeMillis() - 100000))
        .setExpiration(Date(System.currentTimeMillis() - 1000))
        .signWith(signingKey(), SignatureAlgorithm.HS256)
        .compact()
    assertThrows<JwtTokenException> { service.isTokenExpired(expiredToken) }
  }

  @Test
  fun `extractUsername should throw for malformed token`() {
    assertThrows<JwtTokenException> { service.extractUsername("not-a-jwt") }
  }

  @Test
  fun `extractUsername should throw for token with wrong signature`() {
    val otherKey = Keys.hmacShaKeyFor("another-secret-key-that-is-also-256-bits-long!!!".toByteArray(StandardCharsets.UTF_8))
    val forged =
      Jwts
        .builder()
        .setSubject("user1")
        .setExpiration(Date(System.currentTimeMillis() + 100000))
        .signWith(otherKey, SignatureAlgorithm.HS256)
        .compact()
    assertThrows<io.jsonwebtoken.security.SignatureException> { service.extractUsername(forged) }
  }

  @Test
  fun `getUserPrincipalFromToken should reconstruct principal`() {
    val token = service.generateAccessToken(userPrincipal(id = 42L, username = "alice"))
    val principal = service.getUserPrincipalFromToken(token)
    assertNotNull(principal)
    assertEquals(42L, principal.id)
    assertEquals("alice", principal.username)
    assertEquals("USER", principal.userType)
    assertTrue(principal.authorities.any { it.authority == "PERMISSION_ORDER_READ" })
  }

  @Test
  fun `getUserPrincipalFromToken should throw when userId claim missing`() {
    val token =
      Jwts
        .builder()
        .setSubject("alice")
        .claim("userType", "USER")
        .setExpiration(Date(System.currentTimeMillis() + 100000))
        .signWith(signingKey(), SignatureAlgorithm.HS256)
        .compact()
    assertThrows<JwtTokenException> { service.getUserPrincipalFromToken(token) }
  }

  @Test
  fun `createUserPrincipal should build principal from parts`() {
    val principal = service.createUserPrincipal(7L, "admin", "ADMIN", listOf("ROLE_ADMIN", "PERMISSION_USER_READ"))
    assertEquals(7L, principal.id)
    assertEquals("admin", principal.username)
    assertEquals("ADMIN", principal.userType)
    assertEquals(2, principal.authorities.size)
  }

  @Test
  fun `extractTokenFromHeader should strip Bearer prefix`() {
    assertEquals("abc.def", service.extractTokenFromHeader("Bearer abc.def"))
  }

  @Test
  fun `extractTokenFromHeader should return null for non-bearer header`() {
    assertNull(service.extractTokenFromHeader("Basic abc"))
    assertNull(service.extractTokenFromHeader(null))
    assertNull(service.extractTokenFromHeader("bearer lower"))
  }

  @Test
  fun `constants should match expected prefix and header`() {
    assertEquals("Bearer ", JwtTokenService.TOKEN_PREFIX)
    assertEquals("Authorization", JwtTokenService.HEADER_STRING)
  }
}
