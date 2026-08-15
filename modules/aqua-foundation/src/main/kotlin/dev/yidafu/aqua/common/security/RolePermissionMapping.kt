package dev.yidafu.aqua.common.security

import dev.yidafu.aqua.common.domain.model.enums.AdminPermission
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel

/**
 * 角色-权限映射配置
 * 与 PRD 01-overview.md §角色权限矩阵 保持一致。
 * - SUPER_ADMIN: 全部 38 码
 * - ADMIN: 业务域 32 码(缺 ADMIN_USER_*、SYSTEM_CONFIG_*、ORDER_CREATE、RECONCILIATION_SUBMIT 共 6 码)
 * - DELIVERY_WORKER: 8 码(ORDER_READ/PRODUCT_READ/ADDRESS_READ/ORDER_CREATE/DELIVERY_TASK/RECONCILIATION_READ/RECONCILIATION_SUBMIT/STATISTICS_READ)
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

      // 管理员拥有业务域权限(不含管理员账号管理与系统配置;DELIVERY_TASK 已授予,
      // 支持「管理员兼送水」F-USR-023 —— 见 PRD 01-overview.md §角色权限矩阵)
      AdminRoleModel.ADMIN -> {
        setOf(
          // 商品
          AdminPermission.PRODUCT_READ,
          AdminPermission.PRODUCT_WRITE,
          // 订单
          AdminPermission.ORDER_READ,
          AdminPermission.ORDER_WRITE,
          AdminPermission.ORDER_ASSIGN,
          AdminPermission.ORDER_REVIEW,
          // 配送执行(管理员兼送水,F-USR-023)
          AdminPermission.DELIVERY_TASK,
          // 送水工管理
          AdminPermission.DELIVERY_WORKER_READ,
          AdminPermission.DELIVERY_WORKER_WRITE,
          AdminPermission.DELIVERY_WORKER_DELETE,
          AdminPermission.DELIVERY_WORKER_RESET_PASSWORD,
          // 用户
          AdminPermission.USER_READ,
          AdminPermission.USER_WRITE,
          AdminPermission.USER_DELETE,
          AdminPermission.USER_RESET_PASSWORD,
          // 地址
          AdminPermission.ADDRESS_READ,
          AdminPermission.ADDRESS_WRITE,
          // 对账
          AdminPermission.RECONCILIATION_READ,
          AdminPermission.RECONCILIATION_CONFIRM,
          AdminPermission.RECONCILIATION_REVIEW,
          AdminPermission.RECONCILIATION_EXECUTE,
          // 支付
          AdminPermission.PAYMENT_READ,
          AdminPermission.PAYMENT_REFUND,
          // 统计
          AdminPermission.STATISTICS_READ,
          AdminPermission.STATISTICS_EXPORT,
          // 日志
          AdminPermission.LOG_READ,
          // 运营配置
          AdminPermission.OPS_CONFIG_READ,
          AdminPermission.OPS_CONFIG_WRITE,
          // 意见反馈
          AdminPermission.FEEDBACK_READ,
          AdminPermission.FEEDBACK_REVIEW,
          // 营销(客户分组/分组价/优惠券/满减折扣/桶水票)
          AdminPermission.MKT_READ,
          AdminPermission.MKT_WRITE,
        )
      }

      // 送水工:配送执行、代客下单、本人订单/日报/业绩
      AdminRoleModel.DELIVERY_WORKER -> {
        setOf(
          AdminPermission.PRODUCT_READ,
          AdminPermission.ORDER_READ,
          AdminPermission.ORDER_CREATE,
          AdminPermission.DELIVERY_TASK,
          AdminPermission.ADDRESS_READ,
          AdminPermission.RECONCILIATION_READ,
          AdminPermission.RECONCILIATION_SUBMIT,
          AdminPermission.STATISTICS_READ,
        )
      }
    }
}
