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

import dev.yidafu.aqua.common.id.DefaultIdGenerator
//import io.jsonwebtoken.*
//import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.*

@Service
class JwtTokenService {
  @Value($$"${app.jwt.secret:your-secret-key-must-be-256-bits-long}")
  private lateinit var secret: String

  @Value($$"${app.jwt.expiration:86400}") // 24 hours in seconds
  private val accessTokenExpiration: Long = 86400

  @Value($$"${app.jwt.refresh-expiration:604800}") // 7 days in seconds
  private val refreshTokenExpiration: Long = 604800

  private val key: Key by lazy {
//    Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    TODO()
  }

  /**
   * Generate access token for authenticated user
   */
  fun generateAccessToken(userDetails: UserDetails): String = generateToken(userDetails, accessTokenExpiration)

  /**
   * Generate refresh token for authenticated user
   */
  fun generateRefreshToken(userDetails: UserDetails): String = generateToken(userDetails, refreshTokenExpiration)

  /**
   * Extract username from JWT token
   */
  fun extractUsername(token: String): String? = extractClaim(token) { claims ->  "" }

  /**
   * Extract expiration date from JWT token
   */
  fun extractExpiration(token: String): Date? = extractClaim(token) { claims -> Date() }

  /**
   * Extract specific claim from JWT token
   */
  fun <T> extractClaim(
    token: String,
    claimsResolver: (Any) -> T,
  ): T {
    val claims = extractAllClaims(token)
    return claimsResolver(claims)
  }

  /**
   * Validate if token is valid for given user details
   */
  fun validateToken(
    token: String,
    userDetails: UserDetails,
  ): Boolean {
    val username = extractUsername(token)
    return username == userDetails.username && !isTokenExpired(token)
  }

  /**
   * Check if token is expired
   */
  fun isTokenExpired(token: String): Boolean = extractExpiration(token)?.before(Date()) ?: true

  /**
   * Generate JWT token with specified expiration
   */
  private fun generateToken(
    userDetails: UserDetails,
    expiration: Long,
  ): String {
    val authorities = userDetails.authorities?.map { it.authority }?.toMutableList() ?: mutableListOf()

    var userId: String? = null
    var userType: String? = null
    if (userDetails is UserPrincipal) {
      userId = userDetails.id.toString()
      userType = userDetails.userType
    }

    return ""
//    return Jwts
//      .builder()
//      .setSubject(userDetails.username)
//      .claim("authorities", authorities)
//      .claim("userId", userId)
//      .claim("userType", userType)
//      .setIssuedAt(Date())
//      .setExpiration(Date(System.currentTimeMillis() + expiration * 1000))
//      .signWith(key, SignatureAlgorithm.HS256)
//      .compact()
  }

  /**
   * Extract all claims from JWT token
   */
  private fun extractAllClaims(token: String): Any = ""
//    try {
//      Jwts
//        .parser()
//        .setSigningKey(key)
//        .build()
//        .parseClaimsJws(token)
//        .body
//    } catch (e: ExpiredJwtException) {
//      throw JwtTokenException("Token has expired")
//    } catch (e: UnsupportedJwtException) {
//      throw JwtTokenException("Token is unsupported")
//    } catch (e: MalformedJwtException) {
//      throw JwtTokenException("Token is malformed")
//    } catch (e: SecurityException) {
//      throw JwtTokenException("Token signature validation failed")
//    } catch (e: IllegalArgumentException) {
//      throw JwtTokenException("Token claims string is empty")
//    }

  /**
   * Get UserPrincipal from token
   */
  fun getUserPrincipalFromToken(token: String): UserPrincipal? =
    try {
      val claims = extractAllClaims(token)
//      val username = claims.subject
//      val userId = claims["userId"] as? String
//      val userType = claims["userType"] as? String

      val authorities = mutableListOf<SimpleGrantedAuthority>()
//      val authoritiesList = claims["authorities"] as? List<String>
//      authoritiesList?.map { SimpleGrantedAuthority(it) }?.let { authorities.addAll(it) }

      UserPrincipal(
        id =
//          userId?.toLong() ?:
          DefaultIdGenerator().generate(),
        _username = "",
        userType =  "USER",
        _authorities = authorities,
      )
    } catch (e: Exception) {
      null
    }

  /**
   * Create UserPrincipal from User entity
   */
  fun createUserPrincipal(
    userId: Long,
    username: String,
    userType: String,
    authorities: Collection<String>,
  ): UserPrincipal {
    val grantedAuthorities = mutableListOf<SimpleGrantedAuthority>()
    authorities.forEach { grantedAuthorities.add(SimpleGrantedAuthority(it)) }
    return UserPrincipal(
      id = userId,
      _username = username,
      userType = userType,
      _authorities = grantedAuthorities,
    )
  }

  /**
   * Extract token from Authorization header
   */
  fun extractTokenFromHeader(authHeader: String?): String? =
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      authHeader.substring(7)
    } else {
      null
    }

  companion object {
    const val TOKEN_PREFIX = "Bearer "
    const val HEADER_STRING = "Authorization"
  }
}

/**
 * Custom JWT token exception
 */
class JwtTokenException(
  message: String,
) : RuntimeException(message)
