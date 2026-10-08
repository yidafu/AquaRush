package dev.yidafu.aqua.common.security

import dev.yidafu.aqua.common.domain.model.enums.AdminPermission
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * 将 AdminPermission 转换为 Spring Security 的 GrantedAuthority
 */
fun AdminPermission.toSimpleGrantedAuthority(): SimpleGrantedAuthority = SimpleGrantedAuthority("PERMISSION_${this.name}")

/**
 * 将权限集合转换为 GrantedAuthority 列表
 */
fun Collection<AdminPermission>.toSimpleGrantedAuthorities(): List<SimpleGrantedAuthority> = this.map { it.toSimpleGrantedAuthority() }

/**
 * 将角色转换为权限列表（包含角色本身）
 */
fun AdminRoleModel.toPermissionAuthorities(): List<GrantedAuthority> {
  val authorities = mutableListOf<GrantedAuthority>()
  // 添加角色本身
  authorities.add(SimpleGrantedAuthority("ROLE_${this.name}"))
  // 超级管理员隐含 ADMIN 角色，保证 hasRole('ADMIN') / hasAnyRole('ADMIN', 'DELIVERY_WORKER') 对超管成立
  if (this == AdminRoleModel.SUPER_ADMIN) {
    authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
  }
  // 添加角色对应的权限
  authorities.addAll(RolePermissionMapping.getPermissions(this).toSimpleGrantedAuthorities())
  return authorities
}
