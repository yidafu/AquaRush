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

package dev.yidafu.aqua.ops.deliveryfeerule.domain.table

import dev.yidafu.aqua.common.domain.table.SoftDeletableTable
import dev.yidafu.aqua.common.domain.table.jsonString
import org.jetbrains.exposed.v1.javatime.datetime

/**
 * `delivery_fee_rules` 表(配送费规则,F-OPS-005,单行配置固定 id=1)。
 *
 * 列定义逐字对齐 `db/changelog/changes/v1.0.7-add-ops-notice-store-delivery-fee.xml`。
 * 对应 JPA 实体为 `DeliveryFeeRuleModel`。
 */
object DeliveryFeeRuleTable : SoftDeletableTable("delivery_fee_rules") {
  val id = long("id")
  val freeFloorLimit = integer("free_floor_limit").default(4)
  val surchargeCents = long("surcharge_cents").default(200L)
  val tiers = jsonString("tiers").nullable()
  val createdAt = datetime("created_at")
  val updatedAt = datetime("updated_at")

  override val primaryKey = PrimaryKey(id)
}
