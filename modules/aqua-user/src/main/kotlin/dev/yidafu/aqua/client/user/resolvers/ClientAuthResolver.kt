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
  fun logout(
      @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Boolean {
    // In a stateless JWT system, logout is primarily client-side
    return true
  }
}
