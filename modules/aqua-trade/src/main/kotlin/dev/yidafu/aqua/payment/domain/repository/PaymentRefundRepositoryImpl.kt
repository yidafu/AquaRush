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
import dev.yidafu.aqua.common.domain.table.PaymentRefundsTable
import dev.yidafu.aqua.common.domain.table.activeOnly
import dev.yidafu.aqua.common.graphql.generated.RefundStatus
import dev.yidafu.aqua.common.graphql.generated.RefundType
import dev.yidafu.aqua.common.id.SnowflakeIdGenerator
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

/**
 * 退款记录仓储实现(JPA→Exposed 迁移 P4,替代原 QueryDSL 实现)。
 * 查询过滤 is_deleted;聚合口径 = status=COMPLETED 按退款成功时间(processed_at)归属闭区间窗口
 * (@SoftDelete 自动过滤已删,原 KDoc 口径不变);金额一律 Long 分;
 * 分页 content 与 count 同事务,排序键补 id 破平防跨页重复。
 */
@Repository
class PaymentRefundRepositoryImpl(
  private val database: Database,
) : PaymentRefundRepository {
  private fun toModel(row: ResultRow): PaymentRefundModel =
    PaymentRefundModel(
      id = row[PaymentRefundsTable.id],
      paymentId = row[PaymentRefundsTable.paymentId],
      orderId = row[PaymentRefundsTable.orderId],
      userId = row[PaymentRefundsTable.userId],
      refundId = row[PaymentRefundsTable.refundId],
      outRefundNo = row[PaymentRefundsTable.refundNo],
      refundAmount = row[PaymentRefundsTable.refundAmountCents],
      refundReason = row[PaymentRefundsTable.reason],
      status = RefundStatus.valueOf(row[PaymentRefundsTable.status]),
      refundType = RefundType.valueOf(row[PaymentRefundsTable.refundType]),
      refundAccount = row[PaymentRefundsTable.refundAccount],
      refundedAt = row[PaymentRefundsTable.processedAt],
      processedBy = row[PaymentRefundsTable.processedBy],
      adminNote = row[PaymentRefundsTable.adminNote],
      failureReason = row[PaymentRefundsTable.failureReason],
      createdAt = row[PaymentRefundsTable.createdAt],
      updatedAt = row[PaymentRefundsTable.updatedAt],
      deletedAt = row[PaymentRefundsTable.deletedAt],
      deletedBy = row[PaymentRefundsTable.deletedBy],
    )

  /** 聚合/对账窗口条件(原 buildWhereClause):processed_at 闭区间 + COMPLETED + 未软删。 */
  private fun completedInWindow(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Op<Boolean> =
    PaymentRefundsTable.activeOnly {
      (PaymentRefundsTable.status eq RefundStatus.COMPLETED.name) and
        (PaymentRefundsTable.processedAt greaterEq startDate) and
        (PaymentRefundsTable.processedAt lessEq endDate)
    }

  override fun findById(id: Long): Optional<PaymentRefundModel> =
    Optional.ofNullable(
      transaction(database) {
        PaymentRefundsTable
          .selectAll()
          .where { PaymentRefundsTable.activeOnly { PaymentRefundsTable.id eq id } }
          .limit(1)
          .firstOrNull()
      }?.let(::toModel),
    )

  override fun save(entity: PaymentRefundModel): PaymentRefundModel =
    transaction(database) {
      val id = entity.id ?: SnowflakeIdGenerator.getInstance().generate().also { entity.id = it }
      val exists =
        PaymentRefundsTable
          .selectAll()
          .where { PaymentRefundsTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        PaymentRefundsTable.update({ PaymentRefundsTable.id eq id }) {
          it[paymentId] = entity.paymentId
          it[orderId] = entity.orderId
          it[userId] = entity.userId
          it[refundId] = entity.refundId
          it[refundNo] = entity.outRefundNo
          it[refundAmountCents] = entity.refundAmount
          it[reason] = entity.refundReason
          it[status] = entity.status.name
          it[refundType] = entity.refundType.name
          it[refundAccount] = entity.refundAccount
          it[processedAt] = entity.refundedAt
          it[processedBy] = entity.processedBy
          it[adminNote] = entity.adminNote
          it[failureReason] = entity.failureReason
          it[updatedAt] = LocalDateTime.now()
        }
      } else {
        PaymentRefundsTable.insert {
          it[PaymentRefundsTable.id] = id
          it[paymentId] = entity.paymentId
          it[orderId] = entity.orderId
          it[userId] = entity.userId
          it[refundId] = entity.refundId
          it[refundNo] = entity.outRefundNo
          it[refundAmountCents] = entity.refundAmount
          it[reason] = entity.refundReason
          it[status] = entity.status.name
          it[refundType] = entity.refundType.name
          it[refundAccount] = entity.refundAccount
          it[processedAt] = entity.refundedAt
          it[processedBy] = entity.processedBy
          it[adminNote] = entity.adminNote
          it[failureReason] = entity.failureReason
          it[PaymentRefundsTable.createdAt] = entity.createdAt
          it[updatedAt] = entity.updatedAt
        }
      }
      entity
    }

  // ===== PaymentRefundRepositoryCustom =====

  override fun sumRefundedAmountByProcessedAtBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long {
    val amountSum = PaymentRefundsTable.refundAmountCents.sum()
    return transaction(database) {
      PaymentRefundsTable
        .select(amountSum)
        .where { completedInWindow(startDate, endDate) }
        .first()
        .longOf(amountSum)
    }
  }

  override fun sumRefundedAmountGroupByDay(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Map<LocalDate, Long> {
    // 按退款成功时间(processed_at)逐日聚合(DATE_TRUNC('day') 返回 TIMESTAMP,查询侧转 LocalDate)
    val dayExpr = DateTrunc("day", PaymentRefundsTable.processedAt)
    val amountSum = PaymentRefundsTable.refundAmountCents.sum()
    return transaction(database) {
      PaymentRefundsTable
        .select(dayExpr, amountSum)
        .where { completedInWindow(startDate, endDate) }
        .groupBy(dayExpr)
        .toList()
        .associate { row -> row[dayExpr].toLocalDate() to row.longOf(amountSum) }
    }
  }

  override fun findRefundRequests(
    status: RefundStatus?,
    dateFrom: LocalDateTime?,
    dateTo: LocalDateTime?,
    page: Int,
    size: Int,
  ): Page<PaymentRefundModel> {
    var condition: Op<Boolean> = Op.TRUE
    status?.let { condition = condition and (PaymentRefundsTable.status eq it.name) }
    dateFrom?.let { condition = condition and (PaymentRefundsTable.createdAt greaterEq it) }
    dateTo?.let { condition = condition and (PaymentRefundsTable.createdAt lessEq it) }
    val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))

    val (content, total) =
      transaction(database) {
        val content =
          PaymentRefundsTable
            .selectAll()
            .where { PaymentRefundsTable.activeOnly { condition } }
            // 创建时间倒序(同原 Pageable 语义),id 破平防跨页重复
            .orderBy(PaymentRefundsTable.createdAt to SortOrder.DESC, PaymentRefundsTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to PaymentRefundsTable.selectAll().where { PaymentRefundsTable.activeOnly { condition } }.count()
      }
    return PageImpl(content, pageable, total)
  }

  override fun findRefundedByDateRange(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    page: Int,
    size: Int,
  ): Page<PaymentRefundModel> {
    val condition = completedInWindow(startDate, endDate)
    val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100))

    val (content, total) =
      transaction(database) {
        val content =
          PaymentRefundsTable
            .selectAll()
            .where { condition }
            // 退款成功时间倒序(与原 QueryDSL 一致),id 破平防跨页重复
            .orderBy(PaymentRefundsTable.processedAt to SortOrder.DESC, PaymentRefundsTable.id to SortOrder.DESC)
            .offset(pageable.offset)
            .limit(pageable.pageSize)
            .toList()
            .map(::toModel)
        content to PaymentRefundsTable.selectAll().where { condition }.count()
      }
    return PageImpl(content, pageable, total)
  }
}

/**
 * `DATE_TRUNC('<part>', expr)` —— 与原 QueryDSL `Expressions.dateTemplate` 逐字一致的按日分组
 * (PostgreSQL/H2 兼容;trade 域不依赖 analytics 模块,故本文件内私有复制)。
 */
private class DateTrunc(
  private val part: String,
  private val target: Expression<*>,
) : ExpressionWithColumnType<LocalDateTime>() {
  override val columnType: IColumnType<LocalDateTime> = JavaLocalDateTimeColumnType()

  override fun toQueryBuilder(queryBuilder: QueryBuilder) {
    queryBuilder.append("DATE_TRUNC('")
    queryBuilder.append(part)
    queryBuilder.append("', ")
    target.toQueryBuilder(queryBuilder)
    queryBuilder.append(")")
  }
}

/** 从 ResultRow 取 SUM/COUNT 等聚合结果并转 Long,空值(SUM 无匹配行)回退 0(同 analytics longOf 口径)。 */
private fun ResultRow.longOf(expr: Expression<*>): Long = (this[expr] as? Number)?.toLong() ?: 0L
