package dev.yidafu.aqua.common.security

import dev.yidafu.aqua.common.domain.model.AdminPermission
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

/**
 * 权限检查工具组件
 * 可在业务代码中注入使用
 */
@Component
class PermissionChecker {
  /**
   * 检查当前用户是否拥有指定权限
   */
  fun hasPermission(permission: AdminPermission): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication
    return authentication?.authorities?.any {
      it.authority == "PERMISSION_${permission.name}"
    } == true
  }

  /**
   * 检查当前用户是否拥有指定权限之一
   */
  fun hasAnyPermission(vararg permissions: AdminPermission): Boolean {
    return permissions.any { hasPermission(it) }
  }

  /**
   * 检查当前用户是否拥有所有指定权限
   */
  fun hasAllPermissions(permissions: Set<AdminPermission>): Boolean {
    return permissions.all { hasPermission(it) }
  }

  /**
   * 检查当前用户是否拥有指定权限（字符串形式）
   */
  fun hasPermission(permissionName: String): Boolean {
    val authentication = SecurityContextHolder.getContext().authentication
    return authentication?.authorities?.any {
      it.authority == permissionName
    } == true
  }
}
