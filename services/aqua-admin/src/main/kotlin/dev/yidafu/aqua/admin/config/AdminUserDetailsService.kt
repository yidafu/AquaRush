package dev.yidafu.aqua.admin.config

import dev.yidafu.aqua.api.service.admin.AdminService
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.exception.UserNotFoundException
import dev.yidafu.aqua.common.security.toPermissionAuthorities
import org.springframework.context.annotation.Primary
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
@Primary
class AdminUserDetailsService(
  private val adminService: AdminService,
) : UserDetailsService {
  /**
   * 有两种情况
   * 1. 后台账号密码登录
   * 2. 微信小程序登录
   */
  override fun loadUserByUsername(username: String): UserDetails {
    val admin = adminService.findByUsername(username) ?: throw UserNotFoundException("User $username not found")
    val user =
      User
        .withUsername(username)
        .password(admin.passwordHash)
        .authorities(admin.role.toPermissionAuthorities())
        .accountExpired(false)
        .accountLocked(false)
        // 停用账号(DISABLED)不可登录(AC-USR-020-07,PRD/07-user.md §F-USR-020)
        .disabled(admin.accountStatus == AdminAccountStatus.DISABLED)
        .build()

    return user
  }
}
