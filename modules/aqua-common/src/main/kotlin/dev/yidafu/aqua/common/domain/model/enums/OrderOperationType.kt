/*
 * AquaRush
 *
 * Copyright (C) 2025 AquaRush Team
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
 * 订单操作类型枚举
 * 用于记录订单的操作历史（时间线）
 */
enum class OrderOperationType {
  ORDER_CREATED, // 订单创建
  ORDER_PAID, // 支付成功
  ORDER_CANCELLED, // 订单取消
  DELIVERY_ASSIGNED, // 配送员分配
  DELIVERY_STARTED, // 开始配送
  DELIVERY_COMPLETED, // 配送完成
  ORDER_COMPLETED, // 订单完成
  REFUND_INITIATED, // 退款发起
  REFUND_COMPLETED, // 退款完成
  PAYMENT_TIMEOUT, // 支付超时
}

/**
 * 操作人类型枚举
 */
enum class OperatorType {
  USER, // 普通用户
  ADMIN, // 管理员
  DELIVERY_WORKER, // 配送员
  SYSTEM, // 系统自动
}
