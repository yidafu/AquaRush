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

package dev.yidafu.aqua.common.domain.model

import dev.yidafu.aqua.common.utils.MoneyUtils
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.Where
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime

import org.hibernate.annotations.SoftDelete

@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "delivery_workers")
open class  DeliveryWorkerModel(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Column(name = "user_id", nullable = false)
  val userId: Long = -1L,
  @Column(name = "wechat_openid", unique = true, nullable = false)
  var wechatOpenId: String = "",
  @Column(name = "name", nullable = false)
  var name: String = "",
  @Column(name = "phone", unique = true, nullable = false)
  var phone: String = "",
  @Column(name = "avatar_url")
  var avatarUrl: String? = null,
  @Column(name = "online_status", nullable = false)
  @Convert(converter = DeliverWorkerStatusConverter::class)
  var onlineStatus: DeliverWorkerModelStatus = DeliverWorkerModelStatus.OFFLINE,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "coordinates", columnDefinition = "json")
  var coordinates: String? = null,
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "current_location", columnDefinition = "json")
  var currentLocation: String? = null, // 存储 JSON 格式的坐标
  @Column(name = "rating")
  var rating: Double? = null,
  @Column(name = "total_orders", nullable = false)
  var totalOrders: Int = 0,
  @Column(name = "completed_orders", nullable = false)
  var completedOrders: Int = 0,
  @Column(name = "average_rating")
  var averageRating: Double? = null,
  @Column(name = "earning_cents")
  var earningCents: Long? = null,

  @Column(name = "is_available", nullable = false)
  var isAvailable: Boolean = true,
  @Column(name = "created_at", nullable = false, updatable = false)
  val createdAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
@Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,

  @Column(name = "deleted_by")
  override var deletedBy: Long? = null
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  // Backward compatibility property
  val earning: BigDecimal?
    get() = earningCents?.let { MoneyUtils.fromCents(it) }
}

@Converter(autoApply = false)
class DeliverWorkerStatusConverter : AttributeConverter<DeliverWorkerModelStatus, String> {
  override fun convertToDatabaseColumn(attribute: DeliverWorkerModelStatus?): String? {
    return attribute?.label
  }

  override fun convertToEntityAttribute(dbData: String?): DeliverWorkerModelStatus? {
    return DeliverWorkerModelStatus.fromString(dbData)
  }
}

enum class DeliverWorkerModelStatus(val label: String) {
  ONLINE("ONLINE"), // 上线
  OFFLINE("OFFLINE"), // 下线
;

  companion object {
    fun fromString(value: String?): DeliverWorkerModelStatus {
      return when (value) {
        "ONLINE", "Online" -> ONLINE
        "OFFLINE", "Offline" -> OFFLINE
        else -> OFFLINE // 默认值
      }
    }
  }
}
