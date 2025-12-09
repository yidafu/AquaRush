package dev.yidafu.aqua.client.user.resolvers

import dev.yidafu.aqua.common.annotation.ClientService
import dev.yidafu.aqua.common.graphql.generated.UpdateProfileInput
import dev.yidafu.aqua.common.graphql.generated.User
import dev.yidafu.aqua.common.graphql.generated.WechatLoginInput
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.user.service.WeChatAuthService
import dev.yidafu.aqua.user.service.WeChatLoginResponse
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
class ClientAuthResolver(
    private val weChatAuthService: WeChatAuthService,
) {
  @MutationMapping
  fun wechatLogin(
      @Argument @Valid input: WechatLoginInput,
  ): WeChatLoginResponse {
    return weChatAuthService.login(input.code)
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
      @AuthenticationPrincipal userPrincipal: UserPrincipal?,
  ): User {
    // 临时处理：如果用户未认证，返回默认用户信息
    if (userPrincipal == null) {
      return User(
          id = 1L, // 默认 ID
          wechatOpenId = "anonymous",
          nickname = input.nickname?.toString() ?: "匿名用户",
          phone = input.phone?.toString() ?: "",
          avatarUrl = input.avatar?.toString(),
          createdAt = LocalDateTime.now(),
          updatedAt = LocalDateTime.now()
      )
    }

    // 正常认证用户的处理
    return User(
        id = userPrincipal.id,
        wechatOpenId = userPrincipal.getOpenId(),
        nickname = input.nickname?.toString() ?: userPrincipal.username,
        phone = input.phone?.toString() ?: userPrincipal.getPhone(),
        avatarUrl = input.avatar?.toString(),
        createdAt = LocalDateTime.now(), // TODO: Get actual creation time
        updatedAt = LocalDateTime.now()
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
