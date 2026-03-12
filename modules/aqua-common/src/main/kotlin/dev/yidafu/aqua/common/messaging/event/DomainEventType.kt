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

package dev.yidafu.aqua.common.messaging.event

/**
 * 领域事件类型枚举
 */
enum class DomainEventType(
  val value: String,
) {
  // 订单事件
  ORDER_CREATED("ORDER_CREATED"),
  ORDER_PAID("ORDER_PAID"),
  ORDER_CANCELLED("ORDER_CANCELLED"),
  ORDER_COMPLETED("ORDER_COMPLETED"),
  ORDER_DELIVERED("ORDER_DELIVERED"),

  // 配送事件
  DELIVERY_ASSIGNED("DELIVERY_ASSIGNED"),
  DELIVERY_STARTED("DELIVERY_STARTED"),
  DELIVERY_COMPLETED("DELIVERY_COMPLETED"),
  DELIVERY_TIMEOUT("DELIVERY_TIMEOUT"),

  // 支付事件
  PAYMENT_TIMEOUT("PAYMENT_TIMEOUT"),
  PAYMENT_REFUNDED("PAYMENT_REFUNDED"),

  // 用户事件
  USER_REGISTERED("USER_REGISTERED"),
  USER_UPDATED("USER_UPDATED"),

  // 通用事件
  UNKNOWN("UNKNOWN"),
  ;

  companion object {
    fun fromValue(value: String): DomainEventType = entries.find { it.value == value } ?: UNKNOWN
  }
}
