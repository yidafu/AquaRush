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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.domain.model.enums.UserTicketStatus
import java.time.LocalDateTime

/**
 * 用户水票实例(PRD 10-marketing.md §F-MKT-007,每票一行):
 * 一票抵一桶,购票入账 N+M 张(赠送票 is_gift 与正常票同池);
 * 每张票带唯一递增票号 ticketNo(格式 SP+入账年月+8位序号,按月重置,见 v2.2.0 迁移);
 * 状态机 UNUSED → USED(核销)→ RETURNED(订单取消/退款退回,未过期可复用)→ USED ... / EXPIRED;
 * 核销幂等:原子条件 UPDATE(status IN (UNUSED,RETURNED) 且未过期),防并发重复使用。
 */
open class UserTicketModel(
  var id: Long? = null,
  val userId: Long = 0,
  val ticketTypeId: Long = 0,
  val purchaseId: Long = 0,
  // 用户可见唯一递增票号(SP+入账年月+8位序号,按月重置;支付入账时生成)
  var ticketNo: String = "",
  var status: UserTicketStatus = UserTicketStatus.UNUSED,
  // 过期时间(快照计算:固定区间=validTo;购票后N天=购票时间+validDays)
  var expireTime: LocalDateTime = LocalDateTime.now(),
  var isGift: Boolean = false,
  var usedOrderId: Long? = null,
  var usedAt: LocalDateTime? = null,
  var returnedAt: LocalDateTime? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable
