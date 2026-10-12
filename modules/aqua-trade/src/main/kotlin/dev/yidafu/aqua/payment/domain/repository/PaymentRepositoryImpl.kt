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

import dev.yidafu.aqua.common.domain.model.PaymentMethod
import dev.yidafu.aqua.common.domain.model.PaymentModel
import dev.yidafu.aqua.common.domain.model.PaymentStatus
import dev.yidafu.aqua.common.domain.table.PaymentsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

/**
 * 支付单仓储实现(JPA→Exposed 迁移 P4,替代原 QueryDSL 实现 + PaymentSpecifications 组装)。
 * 查询过滤 is_deleted([PaymentsTable.activeOnly]);金额一律 Long 分;
 * [claimExpired] 为单条条件 UPDATE(原子,幂等且互斥),WHERE 同样带 activeOnly;
 * version 仅由 [save] 按 JPA @Version 语义递增(无消费方比对),claim 路径不触碰。
 */
@Repository
class PaymentRepositoryImpl(
  private val database: Database,
) : PaymentRepository {
  /** [Pageable] Sort 属性名 → 列映射(原 JPA 派生查询按实体属性名解析 Sort)。 */
  private val sortColumns: Map<String, Expression<*>> =
    mapOf(
      "id" to PaymentsTable.id,
      "orderId" to PaymentsTable.orderId,
      "userId" to PaymentsTable.userId,
      "status" to PaymentsTable.status,
      "amount" to PaymentsTable.amountCents,
      "createdAt" to PaymentsTable.createdAt,
      "updatedAt" to PaymentsTable.updatedAt,
      "expiredAt" to PaymentsTable.expiredAt,
    )

  private fun toModel(row: ResultRow): PaymentModel =
    PaymentModel(
      id = row[PaymentsTable.id],
      orderId = row[PaymentsTable.orderId],
      userId = row[PaymentsTable.userId],
      transactionId = row[PaymentsTable.transactionId],
      prepayId = row[PaymentsTable.prepayId],
      amount = row[PaymentsTable.amountCents],
      currency = row[PaymentsTable.currency],
      status = PaymentStatus.fromValue(row[PaymentsTable.status]),
      paymentMethod = PaymentMethod.fromValue(row[PaymentsTable.paymentMethod]),
      description = row[PaymentsTable.description],
      failureReason = row[PaymentsTable.failureReason],
      paidAt = row[PaymentsTable.paidAt],
      expiredAt = row[PaymentsTable.expiredAt],
      version = row[PaymentsTable.version],
      createdAt = row[PaymentsTable.createdAt],
      updatedAt = row[PaymentsTable.updatedAt],
      deletedAt = row[PaymentsTable.deletedAt],
      deletedBy = row[PaymentsTable.deletedBy],
    )

  override fun findByOrderId(orderId: Long): Optional<PaymentModel> =
    Optional.ofNullable(
      transaction(database) {
        PaymentsTable
          .selectAll()
          .andWhere { PaymentsTable.orderId eq orderId }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByTransactionId(transactionId: String): Optional<PaymentModel> =
    Optional.ofNullable(
      transaction(database) {
        PaymentsTable
          .selectAll()
          .andWhere { PaymentsTable.transactionId eq transactionId }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun findByStatusAndExpiredAtBefore(
    status: PaymentStatus,
    expiredAt: LocalDateTime,
    pageable: Pageable,
  ): Page<PaymentModel> {
    val orders =
      pageable.sort
        .mapNotNull { order ->
          sortColumns[order.property]?.let { col ->
            val sortOrder = if (order.isAscending) SortOrder.ASC else SortOrder.DESC
            col to sortOrder
          }
        }.ifEmpty { listOf(PaymentsTable.id to SortOrder.ASC) }
    val (content, total) =
      transaction(database) {
        val condition = (PaymentsTable.status eq status.name) and (PaymentsTable.expiredAt less expiredAt)
        val content =
          PaymentsTable
            .selectAll()
            .andWhere { condition }
            .orderBy(*orders.toTypedArray())
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to PaymentsTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findPollableByStatusAfterId(
    status: PaymentStatus,
    expiredAt: LocalDateTime,
    afterId: Long,
    limit: Int,
  ): List<PaymentModel> =
    transaction(database) {
      PaymentsTable
        .selectAll()
        .andWhere {
          (PaymentsTable.status eq status.name) and
            (PaymentsTable.expiredAt greater expiredAt) and
            (PaymentsTable.id greater afterId)
        }.orderBy(PaymentsTable.id to SortOrder.ASC)
        .limit(limit.coerceAtLeast(1))
        .toList()
        .map(::toModel)
    }

  override fun findByCreatedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<PaymentModel> =
    transaction(database) {
      PaymentsTable
        .selectAll()
        .andWhere { (PaymentsTable.createdAt greaterEq startDate) and (PaymentsTable.createdAt lessEq endDate) }
        .toList()
    }.map(::toModel)

  override fun findPaidByBillDateWindow(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    page: Int,
    size: Int,
  ): Page<PaymentModel> {
    // R-PAY-04 本地账实基准:仅「账单上会出现的」成功支付流水的状态集合(设计 07 §5.1)
    val paidStatuses =
      listOf(PaymentStatus.SUCCESS, PaymentStatus.REFUNDING, PaymentStatus.REFUNDED).map { it.name }
    val condition =
      (PaymentsTable.paidAt greaterEq startDate) and
        (PaymentsTable.paidAt lessEq endDate) and
        (PaymentsTable.status inList paidStatuses)
    val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 500))
    val (content, total) =
      transaction(database) {
        val content =
          PaymentsTable
            .selectAll()
            .andWhere { condition }
            .orderBy(PaymentsTable.paidAt to SortOrder.DESC, PaymentsTable.id to SortOrder.ASC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to PaymentsTable.selectAll().andWhere { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findById(id: Long): Optional<PaymentModel> =
    Optional.ofNullable(
      transaction(database) {
        PaymentsTable
          .selectAll()
          .andWhere { PaymentsTable.id eq id }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun save(entity: PaymentModel): PaymentModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val exists =
        PaymentsTable
          .selectAllIncludingDeleted()
          .where { PaymentsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        PaymentsTable.update({ PaymentsTable.id eq id }) {
          it[orderId] = entity.orderId
          it[userId] = entity.userId
          it[transactionId] = entity.transactionId
          it[prepayId] = entity.prepayId
          it[amountCents] = entity.amount
          it[currency] = entity.currency
          it[status] = entity.status.name
          it[paymentMethod] = entity.paymentMethod.name
          it[description] = entity.description
          it[failureReason] = entity.failureReason
          it[paidAt] = entity.paidAt
          it[expiredAt] = entity.expiredAt
          // version 数据库侧 +1(对齐 JPA @Version 每次更新递增;避免陈旧实体回写导致版本回退)
          it[version] = PaymentsTable.version + 1L
          it[updatedAt] = LocalDateTime.now()
        }
      } else {
        PaymentsTable.insert {
          it[PaymentsTable.id] = id
          it[orderId] = entity.orderId
          it[userId] = entity.userId
          it[transactionId] = entity.transactionId
          it[prepayId] = entity.prepayId
          it[amountCents] = entity.amount
          it[currency] = entity.currency
          it[status] = entity.status.name
          it[paymentMethod] = entity.paymentMethod.name
          it[description] = entity.description
          it[failureReason] = entity.failureReason
          it[paidAt] = entity.paidAt
          it[expiredAt] = entity.expiredAt
          it[version] = entity.version
          it[PaymentsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  override fun claimExpired(
    id: Long,
    now: LocalDateTime,
  ): Int =
    transaction(database) {
      PaymentsTable.update({
        PaymentsTable.activeOnly {
          (PaymentsTable.id eq id) and
            (PaymentsTable.status eq PaymentStatus.PENDING.name) and
            PaymentsTable.expiredAt.isNotNull() and
            (PaymentsTable.expiredAt lessEq now)
        }
      }) {
        it[status] = PaymentStatus.EXPIRED.name
        // version 不在此递增(裁决结构 #9 ②):全仓无人读 version 做 CAS 比对,递增属装饰;
        // 防重领由 WHERE 状态谓词(PENDING + 已到期)原子保证,不引入 version 比对的新行为风险
        it[updatedAt] = now
      }
    }
}
