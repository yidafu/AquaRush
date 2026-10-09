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

package dev.yidafu.aqua.payment.domain.repository

import dev.yidafu.aqua.common.domain.model.PaymentRefundModel
import java.util.Optional

/**
 * 退款记录仓储(JPA→Exposed 迁移 P4):剥离 JpaRepository,仅保留实际被调用的方法
 * (findByPaymentId/findByRefundId/findByOutRefundNo 无调用方,已废除);
 * 查询过滤 is_deleted,聚合/分页方法在 [PaymentRefundRepositoryCustom]。
 */
interface PaymentRefundRepository : PaymentRefundRepositoryCustom {
  /** 按 id 单行查询:命中且未软删返回实体,不存在或已软删返回 [Optional.empty]。 */
  fun findById(id: Long): Optional<PaymentRefundModel>

  /**
   * upsert 语义:无 id 或 id 不存在时插入(雪花 id 由 [dev.yidafu.aqua.common.id.SnowflakeIdGenerator] 回填),
   * 否则按 id 更新业务列(更新时 updated_at = now)。
   */
  fun save(entity: PaymentRefundModel): PaymentRefundModel
}
