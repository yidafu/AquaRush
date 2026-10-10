/**
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
  ORDER_URGING, // 用户催单(F-ORD-008)
  DELIVERY_ASSIGNED, // 配送员分配
  DELIVERY_STARTED, // 开始配送
  DELIVERY_COMPLETED, // 配送完成(送达即完成 —— R-ORD-11 2026-10 简化)
  ORDER_CONFIRMED, // 用户确认收货(历史留痕:2026-10 前存量操作记录,该流转已废)
  AUTO_CONFIRMED, // 系统自动确认收货(历史留痕:同上,自动确认调度已移除)
  ORDER_COMPLETED, // 订单完成
  REFUND_INITIATED, // 退款发起
  REFUND_COMPLETED, // 退款完成
  PAYMENT_TIMEOUT, // 支付超时
  DELIVERY_PHOTO_APPROVED, // 配送照片核验通过(G23)
  DELIVERY_PHOTO_REJECTED, // 配送照片核验驳回(G23)
  ADDRESS_UPDATED, // 配送任务中修正收货地址(F-ORD-024:收货人/定位/楼层等)
  ;

  companion object {
    fun fromValue(value: String): OrderOperationType =
      when (value) {
        "ORDER_CREATED" -> ORDER_CREATED
        "ORDER_PAID" -> ORDER_PAID
        "ORDER_CANCELLED" -> ORDER_CANCELLED
        "ORDER_URGING" -> ORDER_URGING
        "DELIVERY_ASSIGNED" -> DELIVERY_ASSIGNED
        "DELIVERY_STARTED" -> DELIVERY_STARTED
        "DELIVERY_COMPLETED" -> DELIVERY_COMPLETED
        "ORDER_CONFIRMED" -> ORDER_CONFIRMED
        "AUTO_CONFIRMED" -> AUTO_CONFIRMED
        "ORDER_COMPLETED" -> ORDER_COMPLETED
        "REFUND_INITIATED" -> REFUND_INITIATED
        "REFUND_COMPLETED" -> REFUND_COMPLETED
        "PAYMENT_TIMEOUT" -> PAYMENT_TIMEOUT
        "DELIVERY_PHOTO_APPROVED" -> DELIVERY_PHOTO_APPROVED
        "DELIVERY_PHOTO_REJECTED" -> DELIVERY_PHOTO_REJECTED
        "ADDRESS_UPDATED" -> ADDRESS_UPDATED
        else -> throw IllegalArgumentException("Unknown OrderOperationType value: '" + value + "'")
      }
  }
}

/**
 * 操作人类型枚举
 */
enum class OperatorType {
  USER, // 普通用户
  ADMIN, // 管理员
  DELIVERY_WORKER, // 配送员
  SYSTEM, // 系统自动
  ;

  companion object {
    fun fromValue(value: String): OperatorType =
      when (value) {
        "USER" -> USER
        "ADMIN" -> ADMIN
        "DELIVERY_WORKER" -> DELIVERY_WORKER
        "SYSTEM" -> SYSTEM
        else -> throw IllegalArgumentException("Unknown OperatorType value: '" + value + "'")
      }
  }
}
