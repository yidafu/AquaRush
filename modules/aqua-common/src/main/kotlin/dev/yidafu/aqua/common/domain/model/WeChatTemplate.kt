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

package dev.yidafu.aqua.common.domain.model

data class WeChatTemplateModel(
  val touser: String,
  val template_id: String,
  val page: String? = null,
  val data: Map<String, WeChatTemplateData>,
)
open class  WeChatTemplateData(
  val value: String,
  val color: String? = null,
)

enum class MessageType(val templateId: String, val description: String) {
  ORDER_UPDATE("ORDER_UPDATE", "订单更新通知"),
  ORDER_CREATED("order_created", "订单创建通知"),
  ORDER_PAID("ORDER_PAID", "订单支付成功"),
  ORDER_CANCELLED("order_cancelled", "订单取消通知"),
  ORDER_DELIVERED("order_delivered", "订单配送完成"),
  PAYMENT_SUCCESS("PAYMENT_SUCCESS", "支付成功通知"),
  PAYMENT_FAILURE("PAYMENT_FAILURE", "支付失败通知"),
  DELIVERY_UPDATE("DELIVERY_UPDATE", "配送更新通知"),
  DELIVERY_ASSIGNED("delivery_assigned", "配送员分配通知"),
  DELIVERY_STARTED("delivery_started", "配送开始通知"),
  DELIVERY_DELAYED("delivery_delayed", "配送延迟通知"),
  SYSTEM_NOTICE("SYSTEM_NOTICE", "系统公告通知"),
  PROMOTIONAL("promotional", "促销活动通知"),
  ;

  companion object {
    fun fromString(value: String): MessageType {
      return values().find { it.templateId == value }
        ?: throw IllegalArgumentException("Unknown message type: $value")
    }
  }
}
