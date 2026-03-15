package dev.yidafu.aqua.common.domain.model

/**
 * 管理员权限枚举
 * 按模块区分查询(READ)和管理(WRITE)权限
 */
enum class AdminPermission(val description: String) {
  // 用户模块
  USER_READ("用户查询"),
  USER_WRITE("用户管理"),

  // 订单模块
  ORDER_READ("订单查询"),
  ORDER_WRITE("订单管理"),

  // 产品模块
  PRODUCT_READ("产品查询"),
  PRODUCT_WRITE("产品管理"),

  // 配送模块
  DELIVERY_READ("配送查询"),
  DELIVERY_WRITE("配送管理"),

  // 支付模块
  PAYMENT_READ("支付查询"),
  PAYMENT_WRITE("支付管理"),

  // 统计模块
  STATISTICS_READ("统计查询"),
  STATISTICS_EXPORT("统计导出"),

  // 系统管理
  ADMIN_USER_READ("管理员查询"),
  ADMIN_USER_WRITE("管理员管理"),
  SYSTEM_CONFIG_READ("系统配置查询"),
  SYSTEM_CONFIG_WRITE("系统配置修改"),
}
