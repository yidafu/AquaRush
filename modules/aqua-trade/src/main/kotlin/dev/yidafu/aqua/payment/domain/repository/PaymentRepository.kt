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

import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

/**
 * 支付单仓储(JPA→Exposed 迁移 P4):剥离 JpaRepository/JpaSpecificationExecutor,
 * 仅保留实际被调用的方法;原 PaymentSpecifications 组装的过滤语义并入
 * [PaymentRepositoryImpl] 的 Exposed 条件构建,所有查询过滤 is_deleted。
 */
interface PaymentRepository {
  fun findByOrderId(orderId: Long): Optional<PaymentModel>

  fun findByTransactionId(transactionId: String): Optional<PaymentModel>

  /**
   * 兜底扫描分页查询(R-ORD-04 支付超时优化):status 列等于入参 [status] 且 expired_at < 入参 [expiredAt]
   * (严格小于,同原 JPA 派生查询),分批游标处理,禁止全表拉取(CLAUDE.md 规则 13);
   * content 与 count 同事务;排序应用 pageable.sort(可映射属性),未指定时回退 id 升序防跨页漂移。
   */
  fun findByStatusAndExpiredAtBefore(
    status: PaymentStatus,
    expiredAt: LocalDateTime,
    pageable: Pageable,
  ): Page<PaymentModel>

  /**
   * 创建时间闭区间查询:created_at >= startDate AND created_at <= endDate
   * (同原 PaymentSpecifications.createdAtBetween,两端闭区间),结果无排序。
   */
  fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel>

  /** 按 id 单行查询:命中且未软删返回实体,不存在或已软删返回 [Optional.empty]。 */
  fun findById(id: Long): Optional<PaymentModel>

  /**
   * upsert 语义:无 id 或 id 不存在时插入(雪花 id 由 [dev.yidafu.aqua.common.id.SnowflakeIdGenerator] 回填),
   * 否则按 id 更新业务列(更新时 version 数据库侧 +1,对齐 JPA @Version 每次更新递增)。
   */
  fun save(entity: PaymentModel): PaymentModel

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
