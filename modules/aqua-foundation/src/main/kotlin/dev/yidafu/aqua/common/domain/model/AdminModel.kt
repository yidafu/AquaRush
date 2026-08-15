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

import dev.yidafu.aqua.common.annotation.SnowflakeIdGenerator
import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SoftDelete
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 后台账号(单表,PRD/07-user.md §F-USR-020/021 目标架构)。
 *
 * 送水员档案已并入本表:role=DELIVERY_WORKER 的账号行即送水员身份,
 * 配送业务字段(头像/在线状态/坐标/评分/单量/收入/接单权限)直接挂账号行;
 * 纯管理账号(SUPER_ADMIN)相关列留空。订单/评价/送水员统计/每日收款等
 * delivery_worker_id 引用均指向本表 id(=账号 id,迁移见 v1.2.1)。
 */
@Entity
@SoftDelete(columnName = "is_deleted")
@Table(name = "admins")
data class AdminModel(
  @Id
  @SnowflakeIdGenerator
  @Column(name = "id", nullable = false, updatable = false)
  var id: Long? = null,
  @Column(name = "username", unique = true, nullable = false, length = 50)
  var username: String,
  @Column(name = "password_hash", nullable = false)
  var passwordHash: String,
  @Column(name = "real_name", length = 100)
  var realName: String? = null,
  @Column(name = "phone", length = 20)
  var phone: String? = null,
  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false)
  var role: AdminRoleModel = AdminRoleModel.ADMIN,
  @Column(name = "user_id")
  var userId: Long? = null,
  // 账号状态(停用后不可登录/不可派单接单,v1.2.2 迁移;F-USR-020/021)
  @Enumerated(EnumType.STRING)
  @Column(name = "account_status", nullable = false)
  var accountStatus: AdminAccountStatus = AdminAccountStatus.ACTIVE,
  // ========================================================================
  // 配送业务字段(送水员档案并入账号行,v1.2.1 迁移)
  // ========================================================================
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
  var currentLocation: String? = null,
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
  @Column(name = "last_login_at")
  var lastLoginAt: LocalDateTime? = null,
  @Column(name = "updated_at", nullable = false)
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name = "deleted_at")
  override var deletedAt: LocalDateTime? = null,
  @Column(name = "deleted_by")
  override var deletedBy: Long? = null,
) : SoftDeletable {
  @PreUpdate
  fun preUpdate() {
    updatedAt = LocalDateTime.now()
  }

  // Backward compatibility property - returns earnings in yuan as BigDecimal
  val earning: BigDecimal?
    get() =
      earningCents?.let {
        dev.yidafu.aqua.common.utils.MoneyUtils
          .fromCents(it)
      }
}

@Converter(autoApply = false)
class DeliverWorkerStatusConverter : AttributeConverter<DeliverWorkerModelStatus, String> {
  override fun convertToDatabaseColumn(attribute: DeliverWorkerModelStatus?): String? = attribute?.label

  override fun convertToEntityAttribute(dbData: String?): DeliverWorkerModelStatus? = DeliverWorkerModelStatus.fromString(dbData)
}

enum class DeliverWorkerModelStatus(
  val label: String,
) {
  ONLINE("ONLINE"), // 上线
  OFFLINE("OFFLINE"), // 下线
  ;

  companion object {
    fun fromString(value: String?): DeliverWorkerModelStatus =
      when (value) {
        "ONLINE", "Online" -> ONLINE
        "OFFLINE", "Offline" -> OFFLINE
        else -> OFFLINE // 默认值
      }
  }
}
