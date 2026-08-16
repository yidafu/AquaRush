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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.nio.charset.StandardCharsets
import java.util.Date

class JwtTokenServiceTest :
  FunSpec({
    var service: JwtTokenService = mockk()
    val secret = "unit-test-secret-key-that-is-at-least-256-bits-long!!"

    fun userPrincipal(
      id: Long = 1L,
      username: String = "user1",
    ): UserPrincipal =
      UserPrincipal(
        id = id,
        _username = username,
        userType = "USER",
        _authorities = listOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("PERMISSION_ORDER_READ")),
      )

    fun signingKey() = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    beforeTest {
      service = JwtTokenService()
      val field = JwtTokenService::class.java.getDeclaredField("secret")
      field.isAccessible = true
      field.set(service, secret)
    }
    test("generateAccessToken should produce a valid JWT") {
      val token = service.generateAccessToken(userPrincipal())
      token.split(".").size shouldBe 3
      token.shouldNotBeNull()
    }
    test("generateRefreshToken and generateWorkerToken should produce tokens") {
      val principal = userPrincipal()
      service.generateRefreshToken(principal).shouldNotBeNull()
      service.generateWorkerToken(principal).shouldNotBeNull()
    }
    test("extractUsername should return subject") {
      val token = service.generateAccessToken(userPrincipal(username = "alice"))
      service.extractUsername(token) shouldBe "alice"
    }
    test("validateToken should return true for matching user") {
      val principal = userPrincipal(username = "alice")
      val token = service.generateAccessToken(principal)
      service.validateToken(token, principal) shouldBe true
    }
    test("validateToken should return false for mismatched user") {
      val token = service.generateAccessToken(userPrincipal(username = "alice"))
      service.validateToken(token, userPrincipal(username = "bob")) shouldBe false
    }
    test("isTokenExpired should return false for fresh token") {
      val token = service.generateAccessToken(userPrincipal())
      service.isTokenExpired(token) shouldBe false
    }
    test("extractClaim should extract custom claims") {
      val token = service.generateAccessToken(userPrincipal())
      val subject = service.extractClaim(token) { claims -> claims.subject }
      subject shouldBe "user1"
    }
    test("isTokenExpired should throw for expired token") {
      val expiredToken =
        Jwts
          .builder()
          .setSubject("user1")
          .setIssuedAt(Date(System.currentTimeMillis() - 100000))
          .setExpiration(Date(System.currentTimeMillis() - 1000))
          .signWith(signingKey(), SignatureAlgorithm.HS256)
          .compact()
      shouldThrow<JwtTokenException> { service.isTokenExpired(expiredToken) }
    }
    test("extractUsername should throw for malformed token") {
      shouldThrow<JwtTokenException> { service.extractUsername("not-a-jwt") }
    }
    test("extractUsername should throw for token with wrong signature") {
      val otherKey = Keys.hmacShaKeyFor("another-secret-key-that-is-also-256-bits-long!!!".toByteArray(StandardCharsets.UTF_8))
      val forged =
        Jwts
          .builder()
          .setSubject("user1")
          .setExpiration(Date(System.currentTimeMillis() + 100000))
          .signWith(otherKey, SignatureAlgorithm.HS256)
          .compact()
      shouldThrow<io.jsonwebtoken.security.SignatureException> { service.extractUsername(forged) }
    }
    test("getUserPrincipalFromToken should reconstruct principal") {
      val token = service.generateAccessToken(userPrincipal(id = 42L, username = "alice"))
      val principal = service.getUserPrincipalFromToken(token)
      principal.shouldNotBeNull()
      principal.id shouldBe 42L
      principal.username shouldBe "alice"
      principal.userType shouldBe "USER"
      principal.authorities.any { it.authority == "PERMISSION_ORDER_READ" } shouldBe true
    }
    test("getUserPrincipalFromToken should throw when userId claim missing") {
      val token =
        Jwts
          .builder()
          .setSubject("alice")
          .claim("userType", "USER")
          .setExpiration(Date(System.currentTimeMillis() + 100000))
          .signWith(signingKey(), SignatureAlgorithm.HS256)
          .compact()
      shouldThrow<JwtTokenException> { service.getUserPrincipalFromToken(token) }
    }
    test("createUserPrincipal should build principal from parts") {
      val principal = service.createUserPrincipal(7L, "admin", "ADMIN", listOf("ROLE_ADMIN", "PERMISSION_USER_READ"))
      principal.id shouldBe 7L
      principal.username shouldBe "admin"
      principal.userType shouldBe "ADMIN"
      principal.authorities.size shouldBe 2
    }
    test("extractTokenFromHeader should strip Bearer prefix") {
      service.extractTokenFromHeader("Bearer abc.def") shouldBe "abc.def"
    }
    test("extractTokenFromHeader should return null for non-bearer header") {
      service.extractTokenFromHeader("Basic abc").shouldBeNull()
      service.extractTokenFromHeader(null).shouldBeNull()
      service.extractTokenFromHeader("bearer lower").shouldBeNull()
    }
    test("constants should match expected prefix and header") {
      JwtTokenService.TOKEN_PREFIX shouldBe "Bearer "
      JwtTokenService.HEADER_STRING shouldBe "Authorization"
    }
  })
