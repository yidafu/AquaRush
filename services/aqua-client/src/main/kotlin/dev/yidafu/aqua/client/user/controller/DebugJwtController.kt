package dev.yidafu.aqua.client.user.controller

import dev.yidafu.aqua.client.user.dto.JwtGenerationRequest
import dev.yidafu.aqua.client.user.dto.JwtGenerationResponse
import dev.yidafu.aqua.client.user.dto.UserDebugInfo
import dev.yidafu.aqua.common.security.JwtTokenService
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.ApiResponse
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * JWT generation debug controller for development and testing
 * This controller should only be enabled in development/test environments
 */
@RestController
@RequestMapping("/api/debug/auth")
@ConditionalOnProperty(prefix = "app.debug", name = ["enabled"], havingValue = "true")
class DebugJwtController(
  private val jwtTokenService: JwtTokenService,
  private val userRepository: UserRepository,
) {
  /**
   * Generate JWT token for a user by their ID
   * This endpoint is for debugging purposes only
   */
  @PostMapping("/generate-jwt")
  fun generateJwtByUserId(
    @RequestBody request: JwtGenerationRequest,
  ): ResponseEntity<ApiResponse<JwtGenerationResponse>> {
    try {
      // Find user by ID
      val user =
        userRepository
          .findById(request.userId)
          .orElseThrow {
            IllegalArgumentException("User with ID ${request.userId} not found")
          }

      // Create UserPrincipal for the user
      val authorities = listOf("ROLE_${user.role.name}")
      val grantedAuthorities = authorities.map { SimpleGrantedAuthority(it) }
      val userPrincipal =
        UserPrincipal(
          id = (user.id ?: -1),
          _username = user.wechatOpenId,
          userType = user.role.name,
          _authorities = grantedAuthorities,
        )

      // Generate JWT token
      val token = jwtTokenService.generateAccessToken(userPrincipal)

      val response =
        JwtGenerationResponse(
          token = token,
          userId = user.id!!,
          username = user.wechatOpenId,
          userType = user.role.name,
          nickname = user.nickname,
          status = user.status.name,
          authorities = authorities,
        )

      return ResponseEntity.ok(ApiResponse.Companion.success(response, "JWT token generated successfully"))
    } catch (e: IllegalArgumentException) {
      return ResponseEntity.ok(ApiResponse.Companion.error<JwtGenerationResponse>(e.message ?: "User not found"))
    } catch (e: Exception) {
      return ResponseEntity.ok(ApiResponse.Companion.error<JwtGenerationResponse>("Failed to generate JWT token"))
    }
  }

  /**
   * Get user info by ID (for debugging)
   */
  @GetMapping("/user/{userId}")
  fun getUserById(
    @PathVariable userId: Long,
  ): ResponseEntity<ApiResponse<UserDebugInfo>> {
    try {
      val user =
        userRepository
          .findById(userId)
          .orElseThrow {
            IllegalArgumentException("User with ID $userId not found")
          }

      val userInfo =
        UserDebugInfo(
          id = user.id!!,
          wechatOpenId = user.wechatOpenId,
          nickname = user.nickname,
          phone = user.phone,
          email = user.email,
          role = user.role.name,
          status = user.status.name,
          balance = user.balanceCents,
          totalSpent = 0L,
          createdAt = LocalDateTime.now(),
          lastLoginAt = LocalDateTime.now(),
        )

      return ResponseEntity.ok(ApiResponse.Companion.success(userInfo))
    } catch (e: IllegalArgumentException) {
      return ResponseEntity.ok(ApiResponse.Companion.error<UserDebugInfo>(e.message ?: "User not found"))
    } catch (e: Exception) {
      return ResponseEntity.ok(ApiResponse.Companion.error<UserDebugInfo>("Failed to get user info"))
    }
  }

  /**
   * List all users (for debugging)
   */
  @GetMapping("/users")
  fun listUsers(): ResponseEntity<ApiResponse<List<UserDebugInfo>>> {
    try {
      val users =
        userRepository.findAll().map { user ->
          UserDebugInfo(
            id = user.id!!,
            wechatOpenId = user.wechatOpenId,
            nickname = user.nickname,
            phone = user.phone,
            email = user.email,
            role = user.role.name,
            status = user.status.name,
            balance = user.balanceCents,
            totalSpent = user.totalSpentCents,
            createdAt = user.createdAt,
            lastLoginAt = user.lastLoginAt,
          )
        }

      return ResponseEntity.ok(ApiResponse.Companion.success(users))
    } catch (e: Exception) {
      return ResponseEntity.ok(ApiResponse.Companion.error<List<UserDebugInfo>>("Failed to list users"))
    }
  }
}
