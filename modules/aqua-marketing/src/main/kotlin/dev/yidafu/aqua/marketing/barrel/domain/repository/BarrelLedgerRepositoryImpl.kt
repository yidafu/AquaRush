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

package dev.yidafu.aqua.marketing.barrel.domain.repository

import dev.yidafu.aqua.api.dto.BarrelLedgerRow
import org.jetbrains.exposed.v1.core.IntegerColumnType
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.springframework.data.domain.Page
import org.springframework.stereotype.Repository

/**
 * 桶账聚合读侧实现(原生 SQL 聚合,对齐 UserTicketRepositoryImpl exec 模式):
 * 用户集 userIds 内联为 IN 谓词(调用方保证为 DB 读出的 Long,禁全表)。
 */
@Repository
class BarrelLedgerRepositoryImpl(
  private val database: Database,
) : BarrelLedgerRepository {
  private fun userIdsIn(userIds: List<Long>): String {
    require(userIds.isNotEmpty()) { "userIds must not be empty (禁全表聚合)" }
    return userIds.joinToString(",")
  }

  override fun sumHeldByUserAndSpec(userIds: List<Long>): List<BarrelLedgerRepository.SpecBucketSum> {
    val sql =
      """
      SELECT user_id, specification, SUM(bucket_count) AS bucket_count,
             SUM(deposit_amount_cents) AS amount_cents
      FROM deposit_records
      WHERE status = 'HELD' AND is_deleted = false
        AND specification IS NOT NULL
        AND user_id IN (${userIdsIn(userIds)})
      GROUP BY user_id, specification
      """.trimIndent()
    return transaction(database) {
      exec(sql) { rs ->
        val result = mutableListOf<BarrelLedgerRepository.SpecBucketSum>()
        while (rs.next()) {
          result +=
            BarrelLedgerRepository.SpecBucketSum(
              userId = rs.getLong("user_id"),
              specification = rs.getString("specification"),
              bucketCount = rs.getLong("bucket_count"),
              amountCents = rs.getLong("amount_cents"),
            )
        }
        result
      } ?: emptyList()
    }
  }

  override fun sumDeliveredByUserAndSpec(userIds: List<Long>): List<BarrelLedgerRepository.SpecBucketSum> {
    // 已交付 = 已完成订单 items;仅计配置押金价目的规格(价目表存在性作桶口径,
    // 【开放决策】历史无押金桶归属见 01 §3.7)
    val sql =
      """
      SELECT o.user_id, (oi.product_snapshot->>'specification') AS specification,
             SUM(oi.quantity) AS bucket_count, 0 AS amount_cents
      FROM order_items oi
      JOIN orders o ON o.id = oi.order_id
      WHERE o.status = 'COMPLETED' AND o.is_deleted = false
        AND oi.is_deleted = false
        AND o.user_id IN (${userIdsIn(userIds)})
        AND (oi.product_snapshot->>'specification') IN (
          SELECT specification FROM barrel_fees WHERE is_deleted = false
        )
      GROUP BY o.user_id, (oi.product_snapshot->>'specification')
      """.trimIndent()
    return transaction(database) {
      exec(sql) { rs ->
        val result = mutableListOf<BarrelLedgerRepository.SpecBucketSum>()
        while (rs.next()) {
          result +=
            BarrelLedgerRepository.SpecBucketSum(
              userId = rs.getLong("user_id"),
              specification = rs.getString("specification"),
              bucketCount = rs.getLong("bucket_count"),
              amountCents = rs.getLong("amount_cents"),
            )
        }
        result
      } ?: emptyList()
    }
  }

  override fun sumRecoveredByUserAndSpec(userIds: List<Long>): List<BarrelLedgerRepository.SpecBucketSum> {
    // 与交付口径对称:仅计押金价目规格(无价目规格的回收不进欠桶数学,R-PRO-04)
    val sql =
      """
      SELECT o.user_id, r.specification, SUM(r.bucket_count) AS bucket_count, 0 AS amount_cents
      FROM barrel_recovery_records r
      JOIN orders o ON o.id = r.order_id
      WHERE r.is_deleted = false AND o.is_deleted = false
        AND o.user_id IN (${userIdsIn(userIds)})
        AND r.specification IN (
          SELECT specification FROM barrel_fees WHERE is_deleted = false
        )
      GROUP BY o.user_id, r.specification
      """.trimIndent()
    return transaction(database) {
      exec(sql) { rs ->
        val result = mutableListOf<BarrelLedgerRepository.SpecBucketSum>()
        while (rs.next()) {
          result +=
            BarrelLedgerRepository.SpecBucketSum(
              userId = rs.getLong("user_id"),
              specification = rs.getString("specification"),
              bucketCount = rs.getLong("bucket_count"),
              amountCents = rs.getLong("amount_cents"),
            )
        }
        result
      } ?: emptyList()
    }
  }

  override fun pageLedger(
    keyword: String?,
    onlyOwed: Boolean,
    page: Int,
    size: Int,
  ): Page<BarrelLedgerRow> {
    // 台账为按客户的实时聚合视图(R-PRO-05):四路 CTE 汇入 users 维度,LIMIT/OFFSET 分页
    val ledgerCte =
      """
      WITH held AS (
        SELECT user_id, SUM(bucket_count) AS buckets, SUM(deposit_amount_cents) AS cents
        FROM deposit_records
        WHERE status = 'HELD' AND is_deleted = false AND specification IS NOT NULL
        GROUP BY user_id
      ),
      refunded AS (
        SELECT user_id, SUM(deposit_amount_cents) AS cents
        FROM deposit_records
        WHERE status = 'REFUNDED' AND is_deleted = false
        GROUP BY user_id
      ),
      delivered AS (
        SELECT o.user_id AS user_id, SUM(oi.quantity) AS buckets
        FROM order_items oi
        JOIN orders o ON o.id = oi.order_id
        WHERE o.status = 'COMPLETED' AND o.is_deleted = false AND oi.is_deleted = false
          AND (oi.product_snapshot->>'specification') IN (
            SELECT specification FROM barrel_fees WHERE is_deleted = false
          )
        GROUP BY o.user_id
      ),
      recovered AS (
        SELECT o.user_id AS user_id, SUM(r.bucket_count) AS buckets
        FROM barrel_recovery_records r
        JOIN orders o ON o.id = r.order_id
        WHERE r.is_deleted = false AND o.is_deleted = false
          AND r.specification IN (
            SELECT specification FROM barrel_fees WHERE is_deleted = false
          )
        GROUP BY o.user_id
      ),
      ledger AS (
        SELECT u.id AS user_id, u.nickname AS nickname, u.phone AS phone,
               COALESCE(h.buckets, 0) AS held_buckets,
               COALESCE(h.cents, 0) AS held_cents,
               COALESCE(rf.cents, 0) AS refunded_cents,
               COALESCE(d.buckets, 0) AS delivered_buckets,
               COALESCE(rc.buckets, 0) AS recovered_buckets
        FROM users u
        LEFT JOIN held h ON h.user_id = u.id
        LEFT JOIN refunded rf ON rf.user_id = u.id
        LEFT JOIN delivered d ON d.user_id = u.id
        LEFT JOIN recovered rc ON rc.user_id = u.id
        WHERE u.is_deleted = false
      )
      """.trimIndent()
    val filters = mutableListOf<String>()
    // 仅出有桶账活动的用户(在租/交付/回收任一 > 0),零活跃客户不进台账
    filters += "(held_buckets > 0 OR delivered_buckets > 0 OR recovered_buckets > 0)"
    if (onlyOwed) {
      filters += "(delivered_buckets - recovered_buckets) > 0"
    }
    val kw = keyword?.trim()?.takeIf { it.isNotEmpty() }
    if (kw != null) {
      filters += "(COALESCE(nickname, '') ILIKE ? OR COALESCE(phone, '') ILIKE ?)"
    }
    val whereSql = if (filters.isEmpty()) "" else " WHERE " + filters.joinToString(" AND ")

    val args = mutableListOf<Pair<org.jetbrains.exposed.v1.core.ColumnType<*>, Any>>()
    if (kw != null) {
      val pattern = "%$kw%"
      args += VarCharColumnType() to pattern
      args += VarCharColumnType() to pattern
    }

    val contentSql =
      """
      $ledgerCte
      SELECT user_id, nickname, phone, held_buckets, held_cents, refunded_cents,
             delivered_buckets, recovered_buckets,
             GREATEST(delivered_buckets - recovered_buckets, 0) AS owed_buckets
      FROM ledger
      $whereSql
      ORDER BY owed_buckets DESC, user_id ASC
      LIMIT ? OFFSET ?
      """.trimIndent()
    val countSql =
      """
      $ledgerCte
      SELECT COUNT(*) AS total FROM ledger
      $whereSql
      """.trimIndent()

    val limitOffset =
      listOf(
        IntegerColumnType() to size,
        IntegerColumnType() to (page * size),
      )

    return transaction(database) {
      // WITH 前缀的查询必须显式 StatementType.SELECT(否则 Exposed 走 executeUpdate,
      // PG 对返回结果集的调用报「传回预期之外的结果」——LogPartitionRepositoryImpl 同款)
      val total =
        exec(countSql, args, StatementType.SELECT) { rs -> if (rs.next()) rs.getLong("total") else 0L } ?: 0L
      val rows =
        exec(contentSql, args + limitOffset, StatementType.SELECT) { rs ->
          val result = mutableListOf<BarrelLedgerRow>()
          while (rs.next()) {
            result +=
              BarrelLedgerRow(
                userId = rs.getLong("user_id"),
                nickname = rs.getString("nickname"),
                phone = rs.getString("phone"),
                heldBucketCount = rs.getLong("held_buckets"),
                depositHeldCents = rs.getLong("held_cents"),
                depositRefundedCents = rs.getLong("refunded_cents"),
                deliveredBucketCount = rs.getLong("delivered_buckets"),
                recoveredBucketCount = rs.getLong("recovered_buckets"),
                owedBucketCount = rs.getLong("owed_buckets"),
              )
          }
          result
        } ?: emptyList()
      org.springframework.data.domain
        .PageImpl(
          rows,
          org.springframework.data.domain.PageRequest
            .of(page, size),
          total,
        )
    }
  }
}
