package dev.yidafu.aqua.api.dto

/**
 * Response data class for admin login
 */
data class LoginResponse(
  val accessToken: String,
  val refreshToken: String,
  val expiresIn: Long,
  val tokenType: String,
  val userInfo: AdminUserInfo,
)

/**
 * Admin user information in login response
 */
data class AdminUserInfo(
  val id: Long,
  val username: String,
  val realName: String?,
  val role: String,
  // 头像URL(后台顶栏展示当前管理员头像;未设置时前端按用户名自动生成,见 F-USR-020)
  val avatar: String? = null,
)
