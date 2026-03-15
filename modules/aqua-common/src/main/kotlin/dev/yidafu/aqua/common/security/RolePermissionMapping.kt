package dev.yidafu.aqua.common.security

import dev.yidafu.aqua.common.domain.model.AdminPermission
import dev.yidafu.aqua.common.domain.model.AdminRoleModel

/**
 * 角色-权限映射配置
 */
object RolePermissionMapping {
  /**
   * 获取角色对应的权限集合
   */
  fun getPermissions(role: AdminRoleModel): Set<AdminPermission> =
    when (role) {
      // 超级管理员拥有所有权限
      AdminRoleModel.SUPER_ADMIN -> {
        AdminPermission.entries.toSet()
      }

      // 管理员拥有大部分业务权限
      AdminRoleModel.ADMIN -> {
        setOf(
          // 用户
          AdminPermission.USER_READ,
          AdminPermission.USER_WRITE,
          // 订单
          AdminPermission.ORDER_READ,
          AdminPermission.ORDER_WRITE,
          // 产品
          AdminPermission.PRODUCT_READ,
          AdminPermission.PRODUCT_WRITE,
          // 配送
          AdminPermission.DELIVERY_READ,
          AdminPermission.DELIVERY_WRITE,
          // 支付
          AdminPermission.PAYMENT_READ,
          AdminPermission.PAYMENT_WRITE,
          // 统计
          AdminPermission.STATISTICS_READ,
          AdminPermission.STATISTICS_EXPORT,
        )
      }

      // 送水员只能查看配送和订单
      AdminRoleModel.DELIVERY_WORKER -> {
        setOf(
          AdminPermission.PRODUCT_READ,
          AdminPermission.DELIVERY_READ,
          AdminPermission.DELIVERY_WRITE,
          AdminPermission.ORDER_READ,
        )
      }
    }
}
