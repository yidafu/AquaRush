/**
 * AquaRush
 *
 * Copyright (C) 2026 AquaRush Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.yidafu.aqua.common.domain.model.enums

/**
 * 后台权限枚举
 * 按域(模块)拆分:查询(READ)/管理(WRITE)分离,关键操作(派单、核验、确认、审核、执行、导出、退款、删除、重置密码)独立成码。
 * 与 PRD 01-overview.md §权限码目录 保持一致。
 */
enum class AdminPermission(
  val description: String,
) {
  // 商品
  PRODUCT_READ("商品查询"),
  PRODUCT_WRITE("商品管理"),

  // 订单
  ORDER_READ("订单查询"),
  ORDER_WRITE("订单管理"),
  ORDER_CREATE("代客下单"),
  ORDER_ASSIGN("派单"),
  ORDER_REVIEW("配送照片核验"),

  // 配送执行
  DELIVERY_TASK("配送任务执行"),

  // 送水工管理
  DELIVERY_WORKER_READ("送水工查询"),
  DELIVERY_WORKER_WRITE("送水工管理"),
  DELIVERY_WORKER_DELETE("删除送水工"),
  DELIVERY_WORKER_RESET_PASSWORD("重置送水工密码"),

  // 用户
  USER_READ("用户查询"),
  USER_WRITE("用户管理"),
  USER_DELETE("删除用户"),
  USER_RESET_PASSWORD("重置用户密码"),

  // 地址
  ADDRESS_READ("地址查询"),
  ADDRESS_WRITE("地址维护"),

  // 管理员
  ADMIN_USER_READ("管理员查询"),
  ADMIN_USER_WRITE("管理员管理"),

  // 对账
  RECONCILIATION_READ("对账/日报查询"),
  RECONCILIATION_SUBMIT("日报提交"),
  RECONCILIATION_CONFIRM("日报确认"),
  RECONCILIATION_REVIEW("对账审核"),
  RECONCILIATION_EXECUTE("执行对账"),

  // 支付
  PAYMENT_READ("支付查询"),
  PAYMENT_REFUND("退款"),

  // 支付对账(F-ORD-031/R-PAY-04,G93 微信支付对账,2026-10 接线;与 RECONCILIATION_* 日报对账相互独立,
  // 见 spec/prd/01-overview.md §权限码目录)
  PAYMENT_RECONCILIATION_READ("支付对账查询"),
  PAYMENT_RECONCILIATION_EXECUTE("支付对账执行"),

  // 统计
  STATISTICS_READ("统计查询"),
  STATISTICS_EXPORT("统计导出"),

  // 日志
  LOG_READ("日志查询"),

  // 系统配置
  SYSTEM_CONFIG_READ("系统配置查询"),
  SYSTEM_CONFIG_WRITE("系统配置修改"),

  // 运营配置
  OPS_CONFIG_READ("运营配置查询"),
  OPS_CONFIG_WRITE("运营配置修改"),

  // 意见反馈
  FEEDBACK_READ("意见反馈查询"),
  FEEDBACK_REVIEW("意见反馈处理"),

  // 营销(F-MKT-001/002/003/004/007:客户分组、分组价、优惠券、满减/折扣、桶水票)
  MKT_READ("营销查询"),
  MKT_WRITE("营销配置修改"),

  // 通知与消息(PRD 13-notification.md v0.20 新增:消息列表查询 / 推送与消息配置)
  NOTIFICATION_READ("通知与消息查询"),
  NOTIFICATION_WRITE("推送与消息配置"),
}
