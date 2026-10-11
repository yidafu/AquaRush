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

package dev.yidafu.aqua.order.mapper

import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.graphql.generated.OrderVo

/** 脱敏掩码段(F-ORD-032 固定形态「前 3 + **** + 后 4」) */
private const val PHONE_MASK = "****"

/**
 * 送水工可看完整收货电话的订单状态(配送阶段可见性窗口 —— R-ORD-17/F-ORD-032):
 * 仅「待配送/配送中」可见完整号码,其余状态(待支付/待派单/已完成/已取消/已退款)一律脱敏。
 */
val WORKER_FULL_PHONE_VISIBLE_STATUSES: Set<OrderModelStatus> =
  setOf(
    OrderModelStatus.PENDING_DELIVERY,
    OrderModelStatus.DELIVERING,
  )

/** 订单是否处于送水工可见完整号码的配送阶段(R-ORD-17 可见性窗口) */
fun OrderModelStatus.isFullPhoneVisibleForWorker(): Boolean = this in WORKER_FULL_PHONE_VISIBLE_STATUSES

/**
 * 客户电话脱敏(F-ORD-032):固定「前 3 + **** + 后 4」(如 138****1234)。
 * 长度不足 8 位(短号/异常格式)时前 3 与后 4 会重叠,整段置 **** 防部分泄漏;
 * 空白号码原样返回(无可泄漏内容)。可空电话由调用点以 ?.let 处理(避免 JVM 签名冲突)。
 */
fun maskCustomerPhone(phone: String): String =
  when {
    phone.isBlank() -> phone
    phone.length >= 8 -> phone.take(3) + PHONE_MASK + phone.takeLast(4)
    else -> PHONE_MASK
  }

/**
 * 按订单状态脱敏:配送阶段原样返回,其余阶段脱敏。
 * 脱敏在服务端 DTO 组装处完成,完整号码不对送水工下发、无「查看完整号码」旁路(AC-ORD-032-05)。
 */
fun maskCustomerPhoneForStatus(
  phone: String,
  status: OrderModelStatus,
): String = if (status.isFullPhoneVisibleForWorker()) phone else maskCustomerPhone(phone)

/**
 * 送水工视角订单输出脱敏(F-ORD-032/R-ORD-17):收货人电话(AddressVo.phone)与
 * 下单账号电话(UserVo.phone)同口径脱敏,防经账号电话绕过号码保护。
 * 是否套用由调用方按调用方角色决定 —— 管理员/超管不受限(AC-ORD-032-04)。
 */
fun OrderVo.maskCustomerContactsForWorker(status: OrderModelStatus): OrderVo =
  copy(
    address =
      address?.let { addr ->
        addr.copy(phone = maskCustomerPhoneForStatus(addr.phone, status))
      },
    user =
      user?.let { usr ->
        usr.copy(phone = usr.phone?.let { phone -> maskCustomerPhoneForStatus(phone, status) })
      },
  )
