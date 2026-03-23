package dev.yidafu.aqua.api.query

import com.fasterxml.jackson.annotation.JsonProperty
import dev.yidafu.aqua.common.graphql.generated.UserRole
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * 创建用户请求DTO
 */
data class CreateUserRequest(
  @field:JsonProperty("openId")
  @field:NotNull(message = "微信OpenID不能为空")
  @field:NotBlank(message = "微信OpenID不能为空")
  @field:Pattern(regexp = "^[a-zA-Z0-9]{25}$", message = "微信OpenID格式不正确")
  val openId: String,
  @field:JsonProperty("nickname")
  @field:Size(max = 50, message = "昵称长度不能超过50个字符")
  val nickname: String?,
  @field:JsonProperty("phone")
  @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
  val phone: String?,
  @field:JsonProperty("email")
  @field:NotBlank(message = "邮箱不能为空")
  @field:Email(message = "邮箱格式不正确")
  val email: String,
  @field:JsonProperty("avatar")
  val avatar: String?,
  @field:JsonProperty("role")
  val role: UserRole? = null,
)
