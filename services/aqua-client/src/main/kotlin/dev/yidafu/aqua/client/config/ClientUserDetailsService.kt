package dev.yidafu.aqua.client.config

import dev.yidafu.aqua.common.domain.model.UserModel
import dev.yidafu.aqua.common.domain.model.enums.AdminPermission
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.security.RolePermissionMapping
import dev.yidafu.aqua.common.security.UserPrincipal
import dev.yidafu.aqua.common.security.toSimpleGrantedAuthorities
import dev.yidafu.aqua.user.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class ClientUserDetailsService(
  private val userRepository: UserRepository,
) : UserDetailsService {
  val logger = LoggerFactory.getLogger(ClientUserDetailsService::class.java)

  override fun loadUserByUsername(username: String): UserDetails {
    logger.info("loadUserByUsername $username")
    // For JWT authentication, username is the OpenID
    val user =
      userRepository.findByWechatOpenId(username)
        ?: throw UsernameNotFoundException("User not found with openid: $username")

    // 角色唯一来源为持久化的 users.role (R-AUTH-04: 禁止按手机号前缀等不可信信息推断角色)
    val authorities = buildAuthorities(user)

    return UserPrincipal(
      id = user.id!!,
      _username = user.wechatOpenId,
      userType = roleName(user.role),
      _authorities = authorities,
    )
  }

  private fun roleName(role: UserRole): String =
    when (role) {
      UserRole.ADMIN -> "ADMIN"
      // 与后端 AdminRoleModel.DELIVERY_WORKER 对齐
      UserRole.WORKER -> "DELIVERY_WORKER"
      UserRole.USER, UserRole.NONE -> "USER"
    }

  private fun buildAuthorities(user: UserModel): List<SimpleGrantedAuthority> {
    val authorities = mutableListOf<SimpleGrantedAuthority>()
    val role = user.role
    val permissions: Set<AdminPermission> =
      when (role) {
        UserRole.WORKER -> RolePermissionMapping.getPermissions(AdminRoleModel.DELIVERY_WORKER)
        UserRole.ADMIN -> RolePermissionMapping.getPermissions(AdminRoleModel.ADMIN)
        // 普通用户仅授予本人数据操作相关权限，备将来细控；实际由 isAuthenticated() + 数据隔离(R-AUTH-02) 强制
        UserRole.USER, UserRole.NONE -> {
          setOf(
            AdminPermission.USER_READ,
            AdminPermission.USER_WRITE,
            AdminPermission.ORDER_READ,
            AdminPermission.ORDER_WRITE,
            AdminPermission.PRODUCT_READ,
            AdminPermission.ADDRESS_READ,
            AdminPermission.ADDRESS_WRITE,
          )
        }
      }

    authorities.add(SimpleGrantedAuthority("ROLE_${roleName(role)}"))
    authorities.addAll(permissions.toSimpleGrantedAuthorities())
    return authorities
  }
}
