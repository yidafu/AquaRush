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

import java.time.LocalDateTime

/**
 * Payment 仓储自定义方法(JPA→Exposed 迁移 P4 仅保留实际被调用的方法;
 * 原 *Enhanced/findPaymentsWithFilters 死方法随 QueryDSL 实现一并废除)。
 */
interface PaymentRepositoryCustom {
  /**
   * 原子认领过期支付(单条条件 UPDATE,R-ORD-04 支付超时优化):
   * 仅未软删、PENDING 且已到期(expired_at 非空且 <= now)的行置 EXPIRED,
   * 同时 version 数据库侧 +1、updated_at = now;
   * 返回受影响行数(1=认领成功,0=已被处理/未到期/已软删/不存在)——单语句原子,幂等且互斥。
   * KDoc 与实现一致:WHERE 带 activeOnly(软删行不可认领,对齐营销域 CAS 认领先例)。
   */
  fun claimExpired(
    id: Long,
    now: LocalDateTime,
  ): Int
}
