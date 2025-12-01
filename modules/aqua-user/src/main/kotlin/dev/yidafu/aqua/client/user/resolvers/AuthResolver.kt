package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.api.dto.UserRole
import dev.yidafu.aqua.api.dto.UserStatus
import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.UpdateProfileInput
import dev.yidafu.aqua.common.graphql.generated.WechatLoginInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.domain.model.User
import dev.yidafu.aqua.user.service.UserInfo
import dev.yidafu.aqua.user.service.WeChatAuthService
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Controller
import java.math.BigDecimal
import java.time.LocalDateTime

@ClientService
@Controller
class AuthResolver(
    private val weChatAuthService: WeChatAuthService,
) {
  @MutationMapping
  fun wechatLogin(
      @Argument @Valid input: WechatLoginInput,
  ): UserInfo {
    val authResponse = weChatAuthService.login(input.code.toString())
    return authResponse.userInfo
  }

  @MutationMapping
  fun refreshToken(
      @Argument refreshToken: String,
  ): String {
    val authResponse = weChatAuthService.refreshToken(refreshToken)
    return authResponse.accessToken
  }

  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun updateProfile(
      @Argument @Valid input: UpdateProfileInput,
      @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): User {
    // Simplified implementation - just return user info
    return User(
        id = userPrincipal.id,
        wechatOpenId = userPrincipal.getOpenId(),
        nickname = input.nickname?.toString() ?: userPrincipal.username,
        phone = input.phone?.toString() ?: userPrincipal.getPhone(),
        avatarUrl = null, // Would be from user profile
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        email = "",
        status = UserStatus.ACTIVE,
        role = UserRole.USER,
        balance = BigDecimal(0),
        totalSpent = BigDecimal(0),
        lastLoginAt = LocalDateTime.now()
    )
  }

  @MutationMapping
  @PreAuthorize("isAuthenticated()")
  fun logout(
      @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Boolean {
    // In a stateless JWT system, logout is primarily client-side
    return true
  }
}
