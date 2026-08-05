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

import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator

/**
 * 订单操作记录实体类
 * 用于记录订单的操作历史（时间线），方便展示订单的相关操作记录
 */
@Entity
@Table(
  name = "order_operations",
  indexes = [
    Index(name = "idx_order_operations_order_id", columnList = "order_id"),
    Index(name = "idx_order_operations_created_at", columnList = "created_at"),
  ],
)
class OrderOperationModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "order_id", nullable = false)
  val orderId: Long = -1L,
  @Column(name = "operation_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  val operationType: OrderOperationType = OrderOperationType.ORDER_CREATED,
  @Column(name = "operator_id")
  val operatorId: Long? = null,
  @Column(name = "operator_type", length = 20)
  @Enumerated(EnumType.STRING)
  val operatorType: OperatorType = OperatorType.SYSTEM,
  @Column(name = "description", length = 500)
  val description: String? = null,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extra_data", columnDefinition = "json")
  val extraData: String? = null,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
