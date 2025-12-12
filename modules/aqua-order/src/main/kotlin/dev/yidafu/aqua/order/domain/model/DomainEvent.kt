/*
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

package dev.yidafu.aqua.order.domain.model

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity(name = "OrderDomainEvent")
@Table(
  name = "order_domain_events",
  indexes = [
    Index(name = "idx_order_event_type", columnList = "eventType"),
    Index(name = "idx_order_next_run_at", columnList = "nextRunAt"),
    Index(name = "idx_order_event_status", columnList = "status"),
  ],
)
data class DomainEventModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "event_type", nullable = false, length = 100)
  var eventType: String,
  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  var payload: String,
  @Column(name = "retry_count", nullable = false)
  var retryCount: Int = 0,
  @Column(name = "next_run_at")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  var nextRunAt: LocalDateTime? = null,
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  var status: EventStatusModel = EventStatusModel.PENDING,
  @Column(name = "created_at", nullable = false)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "error_message", columnDefinition = "TEXT")
  var errorMessage: String? = null,
)

// Event type constants
object EventType {
  const val ORDER_PAID = "ORDER_PAID"
  const val ORDER_ASSIGNED = "ORDER_ASSIGNED"
  const val ORDER_DELIVERED = "ORDER_DELIVERED"
  const val PAYMENT_TIMEOUT = "PAYMENT_TIMEOUT"
  const val DELIVERY_TIMEOUT = "DELIVERY_TIMEOUT"
}
