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

import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.logging.context.CorrelationIdHolder
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
//import org.springframework.security.core.context.SecurityContextHolder
//import org.springframework.security.core.userdetails.UserDetailsService
//import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

//@Component
class JwtAuthenticationFilter(
  private val jwtTokenService: JwtTokenService,
//  private val userDetailsService: UserDetailsService,
) : OncePerRequestFilter() {
  private val logger = LoggerFactory.getLogger("dev.yidafu.aqua.security.jwt")
  private val auditLogger = LoggerFactory.getLogger("dev.yidafu.aqua.audit")

  override fun doFilterInternal(
    request: HttpServletRequest,
    response: HttpServletResponse,
    filterChain: FilterChain,
  ) {
    val correlationId = CorrelationIdHolder.getCorrelationId()
    val startTime = System.currentTimeMillis()
    var tokenProcessed = false
    var authenticationResult = "SKIPPED"

    try {
      logger.debug(
        "JWT_FILTER_START - CorrelationId: {}, Method: {}, URI: {}",
        correlationId,
        request.method,
        request.requestURI
      )

      // 直接放行所有请求，不做任何认证处理
      filterChain.doFilter(request, response)

      authenticationResult = "ALLOWED"

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
    val correlationId = CorrelationIdHolder.getCorrelationId()
    val shouldNotFilter = false // 返回 false，因为 SecurityConfig 已经配置为允许所有请求

    logger.debug(
      "JWT_FILTER_SHOULD_NOT_FILTER - CorrelationId: {}, Method: {}, URI: {}, ShouldNotFilter: {}",
      correlationId,
      request.method,
      request.requestURI,
      shouldNotFilter
    )

    return shouldNotFilter
  }

  private fun extractTokenFromRequest(request: HttpServletRequest): String? {
    val correlationId = CorrelationIdHolder.getCorrelationId()

    // Try Authorization header first
    val authHeader = request.getHeader("Authorization")
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      val token = authHeader.substring(7)
      logger.debug(
        "JWT_TOKEN_EXTRACTED - CorrelationId: {}, Source: Authorization header, TokenLength: {}",
        correlationId,
        token.length
      )
      return token
    }

    // Try query parameter
    val tokenParam = request.getParameter("token")
    if (!tokenParam.isNullOrBlank()) {
      logger.debug(
        "JWT_TOKEN_EXTRACTED - CorrelationId: {}, Source: Query parameter, TokenLength: {}",
        correlationId,
        tokenParam.length
      )
      return tokenParam
    }

    logger.debug(
      "JWT_TOKEN_NOT_FOUND - CorrelationId: {}, Method: {}, URI: {}",
      correlationId,
      request.method,
      request.requestURI
    )

    return null
  }

  private fun validateAndAuthenticate(
    token: String,
    request: HttpServletRequest,
    correlationId: String,
  ): Boolean {
    return try {
      logger.debug(
        "JWT_VALIDATION_START - CorrelationId: {}, TokenLength: {}",
        correlationId,
        token.length
      )

      // Extract username from token
      val username = jwtTokenService.extractUsername(token)
      if (username.isNullOrBlank()) {
        logger.warn(
          "JWT_VALIDATION_FAILED - CorrelationId: {}, Reason: Cannot extract username from token",
          correlationId
        )
        return false
      }

      logger.debug(
        "JWT_USERNAME_EXTRACTED - CorrelationId: {}, Username: {}",
        correlationId,
        username
      )

      // Load user details and set authentication
//      val userDetails = userDetailsService.loadUserByUsername(username)
//
//      // Check if token is valid
//      if (!jwtTokenService.validateToken(token, userDetails)) {
//        logger.warn(
//          "JWT_VALIDATION_FAILED - CorrelationId: {}, Username: {}, Reason: Token validation failed",
//          correlationId,
//          username
//        )
//        return false
//      }

      logger.debug(
        "JWT_VALIDATION_SUCCESS - CorrelationId: {}, Username: {}",
        correlationId,
        username
      )

//      val authentication = UsernamePasswordAuthenticationToken(
//        userDetails,
//        null,
//        userDetails.authorities
//      )
//      authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
//
//      SecurityContextHolder.getContext().authentication = authentication

      auditLogger.info(
        "JWT_AUTHENTICATION_SUCCESS - CorrelationId: {}, Username: {}, Method: {}, URI: {}",
        correlationId,
        username,
        request.method,
        request.requestURI
      )

      true
    } catch (ex: Exception) {
      logger.error(
        "JWT_VALIDATION_ERROR - CorrelationId: {}, Error: {}",
        correlationId,
        ex.message,
        ex
      )

      auditLogger.error(
        "JWT_AUTHENTICATION_ERROR - CorrelationId: {}, ErrorType: {}, Message: {}",
        correlationId,
        ex.javaClass.simpleName,
        ex.message
      )

      false
    }
  }
}
