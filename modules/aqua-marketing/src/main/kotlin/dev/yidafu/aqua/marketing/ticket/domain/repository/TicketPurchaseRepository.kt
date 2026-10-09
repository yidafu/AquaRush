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

package dev.yidafu.aqua.marketing.ticket.domain.repository

import dev.yidafu.aqua.common.domain.model.TicketPurchaseModel
import dev.yidafu.aqua.common.domain.model.enums.TicketPurchaseStatus
import java.util.Optional

/**
 * 购票单仓库(F-MKT-007):orderNo 作为微信支付 out_trade_no;支付入账原子流转(PENDING→PAID,幂等)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository,仅保留实际被调用的方法;
 * 查询过滤 is_deleted,delete 软删;销售统计/原子流转见 [TicketPurchaseRepositoryCustom]。
 */
interface TicketPurchaseRepository : TicketPurchaseRepositoryCustom {
  fun findByOrderNo(orderNo: String): TicketPurchaseModel?

  fun countByUserIdAndTicketTypeIdAndPaymentStatus(
    userId: Long,
    ticketTypeId: Long,
    paymentStatus: TicketPurchaseStatus,
  ): Long

  fun findById(id: Long): Optional<TicketPurchaseModel>

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: TicketPurchaseModel): TicketPurchaseModel
}
