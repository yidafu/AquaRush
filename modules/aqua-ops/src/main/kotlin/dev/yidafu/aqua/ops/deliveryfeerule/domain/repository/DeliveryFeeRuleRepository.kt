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

package dev.yidafu.aqua.ops.deliveryfeerule.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryFeeRuleModel
import dev.yidafu.aqua.ops.deliveryfeerule.domain.table.DeliveryFeeRuleTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.springframework.stereotype.Repository
import java.util.Optional

/**
 * 配送费规则仓库（Exposed 实现，单行配置固定 id = [SINGLETON_ID]，F-OPS-005）。
 *
 * `save` 更新时显式刷新 `updated_at`（对齐实体 `@PreUpdate`）；
 * 软删过滤显式带上 `is_deleted = false`（原 `@SoftDelete` 由 Hibernate 自动追加）。
 */
@Repository
class DeliveryFeeRuleRepository(
  private val database: Database,
) {
  companion object {
    /** 单行配置固定主键 */
    const val SINGLETON_ID = 1L
  }

  fun findById(id: Long): Optional<DeliveryFeeRuleModel> =
    transaction(database) {
      DeliveryFeeRuleTable
        .selectAll()
        .where { (DeliveryFeeRuleTable.id eq id) and (DeliveryFeeRuleTable.isDeleted eq false) }
        .singleOrNull()
        ?.let(::toModel)
    }.let { Optional.ofNullable(it) }

  fun save(rule: DeliveryFeeRuleModel): DeliveryFeeRuleModel =
    transaction(database) {
      // 本表主键由服务层显式指定(固定 id=1)，与 JPA 期一致：无主键无法落库
      val id =
        rule.id
          ?: error("DeliveryFeeRuleModel.id 必须在保存前赋值(单行配置固定 id=${SINGLETON_ID})")
      val exists =
        DeliveryFeeRuleTable
          .selectAll()
          .where { DeliveryFeeRuleTable.id eq id }
          .limit(1)
          .any()
      if (exists) {
        val now = java.time.LocalDateTime.now()
        DeliveryFeeRuleTable.update({ DeliveryFeeRuleTable.id eq id }) {
          it[freeFloorLimit] = rule.freeFloorLimit
          it[surchargeCents] = rule.surchargeCents
          it[deletedAt] = rule.deletedAt
          it[deletedBy] = rule.deletedBy
          it[updatedAt] = now
        }
        rule.updatedAt = now
      } else {
        DeliveryFeeRuleTable.insert {
          it[DeliveryFeeRuleTable.id] = id
          it[isDeleted] = false // 显式写入：Exposed 的 default() 不会进 INSERT 语句，而 DDL 的 NOT NULL 无兜底
          it[freeFloorLimit] = rule.freeFloorLimit
          it[surchargeCents] = rule.surchargeCents
          it[deletedAt] = rule.deletedAt
          it[deletedBy] = rule.deletedBy
          it[createdAt] = rule.createdAt
          it[updatedAt] = rule.updatedAt
        }
      }
      rule
    }

  /**
   * 原生硬删除（绕过软删除）：
   * 软删行仍占用 SINGLETON_ID，单行配置重新保存会撞主键（测试复位用）。
   * 迁移前该行为由手写原生 SQL 保证（Hibernate @SoftDelete 会把 JPQL/QueryDSL 的 delete
   * 一律改写为软删 UPDATE）；Exposed 没有这层改写，`deleteAll` 天然就是物理删除。
   */
  fun hardDeleteAll() {
    transaction(database) {
      DeliveryFeeRuleTable.deleteAll()
    }
  }

  private fun toModel(row: ResultRow): DeliveryFeeRuleModel =
    DeliveryFeeRuleModel(
      id = row[DeliveryFeeRuleTable.id],
      freeFloorLimit = row[DeliveryFeeRuleTable.freeFloorLimit],
      surchargeCents = row[DeliveryFeeRuleTable.surchargeCents],
      deletedAt = row[DeliveryFeeRuleTable.deletedAt],
      deletedBy = row[DeliveryFeeRuleTable.deletedBy],
      createdAt = row[DeliveryFeeRuleTable.createdAt],
      updatedAt = row[DeliveryFeeRuleTable.updatedAt],
    )
}
