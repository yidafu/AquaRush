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

import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import dev.yidafu.aqua.user.service.CustomUserDetailsService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
  private val jwtTokenService: JwtTokenService,
  private val customUserDetailsService: CustomUserDetailsService,
) : OncePerRequestFilter() {
  private val logger = LoggerFactory.getLogger("dev.yidafu.aqua.security.JwtAuthenticationFilter")
  private val auditLogger = LoggerFactory.getLogger("dev.yidafu.aqua.audit")

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val correlationId = CorrelationIdHolder.getCorrelationId() ?: "unknown"
    val startTime = System.currentTimeMillis()
    var authenticationResult = "SKIPPED"

    try {
      val token = extractTokenFromRequest(request)

      token?.let { t ->
        val isValid = validateAndAuthenticate(t, request, correlationId)

        if (isValid) {
          authenticationResult = "AUTHENTICATED"
        } else {
          authenticationResult = "INVALID_TOKEN"
        }
      } ?: run {
        authenticationResult = "NO_TOKEN"
      }

      filterChain.doFilter(request, response)

      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime

      logger.info(
        "JWT_FILTER_COMPLETE - CorrelationId: {}, Method: {}, URI: {}, Result: {}, Duration: {}ms",
        correlationId,
        request.method,
        request.requestURI,
        authenticationResult,
        duration
      )

    } catch (ex: Exception) {
      authenticationResult = "ERROR"
      val endTime = System.currentTimeMillis()
      val duration = endTime - startTime

      logger.error(
        "JWT_FILTER_ERROR - CorrelationId: {}, Method: {}, URI: {}, Result: {}, Duration: {}ms, Error: {}",
        correlationId,
        request.method,
        request.requestURI,
        authenticationResult,
        duration,
        ex.message,
        ex
      )

      auditLogger.error(
        "SECURITY_FILTER_ERROR - CorrelationId: {}, Method: {}, URI: {}, ErrorType: {}, Message: {}",
        correlationId,
        request.method,
        request.requestURI,
        ex.javaClass.simpleName,
        ex.message
      )

      throw ex
    }
  }

  override fun shouldNotFilter(request: HttpServletRequest): Boolean {
    val requestURI = request.requestURI

    // Skip JWT processing for public endpoints
    return requestURI.startsWith("/login") ||
           requestURI.startsWith("/css/") ||
           requestURI.startsWith("/js/") ||
           requestURI.startsWith("/images/") ||
           requestURI.startsWith("/graphiql") ||
           requestURI.startsWith("/error")
  }

  private fun extractTokenFromRequest(request: HttpServletRequest): String? {
    // Try Authorization header first
    val authHeader = request.getHeader("Authorization")
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      return authHeader.substring(7)
    }

    // Try query parameter
    val tokenParam = request.getParameter("token")
    if (!tokenParam.isNullOrBlank()) {
      return tokenParam
    }

    return null
  }

  private fun validateAndAuthenticate(
    token: String,
    request: HttpServletRequest,
    correlationId: String,
  ): Boolean {
    return try {
      // Extract username from token
      val username = jwtTokenService.extractUsername(token)
      if (username.isNullOrBlank()) {
        logger.warn("JWT validation failed: cannot extract username from token")
        return false
      }

      // Load user details
      val userDetails = customUserDetailsService.loadUserByUsername(username)

      // Check if token is valid
      if (!jwtTokenService.validateToken(token, userDetails)) {
        logger.warn("JWT validation failed for user: {}", username)
        return false
      }

      // Set authentication in SecurityContext
      val authentication = UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.authorities
      )
      authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
      SecurityContextHolder.getContext().authentication = authentication

      auditLogger.info(
        "JWT authentication successful - Username: {}, Method: {}, URI: {}",
        username,
        request.method,
        request.requestURI
      )

      true
    } catch (ex: Exception) {
      logger.error("JWT validation error", ex)

      auditLogger.error(
        "JWT authentication error - ErrorType: {}, Message: {}",
        ex.javaClass.simpleName,
        ex.message
      )

      false
    }
  }
}
