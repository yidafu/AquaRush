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

import dev.yidafu.aqua.common.domain.model.enums.AdminAccountStatus
import dev.yidafu.aqua.common.domain.model.enums.AdminRoleModel
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
data class AdminModel(
  var id: Long? = null,
  var username: String,
  var passwordHash: String,
  var realName: String? = null,
  var phone: String? = null,
  var role: AdminRoleModel = AdminRoleModel.ADMIN,
  var userId: Long? = null,
  // 账号状态(停用后不可登录/不可派单接单,v1.2.2 迁移;F-USR-020/021)
  var accountStatus: AdminAccountStatus = AdminAccountStatus.ACTIVE,
  // ========================================================================
  // 配送业务字段(送水员档案并入账号行,v1.2.1 迁移)
  // ========================================================================
  var avatarUrl: String? = null,
  var onlineStatus: DeliverWorkerModelStatus = DeliverWorkerModelStatus.OFFLINE,
  var coordinates: String? = null,
  var currentLocation: String? = null,
  var rating: Double? = null,
  var totalOrders: Int = 0,
  var completedOrders: Int = 0,
  var averageRating: Double? = null,
  var earningCents: Long? = null,
  var isAvailable: Boolean = true,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var lastLoginAt: LocalDateTime? = null,
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable {
  // Backward compatibility property - returns earnings in yuan as BigDecimal
  val earning: BigDecimal?
    get() =
      earningCents?.let {
        dev.yidafu.aqua.common.util.MoneyUtils
          .fromCents(it)
      }
}

/** 在线状态与库中字符串标签互转(JPA `AttributeConverter` 已随 P5 下线,保留为普通工具类)。 */
class DeliverWorkerStatusConverter {
  fun convertToDatabaseColumn(attribute: DeliverWorkerModelStatus?): String? = attribute?.label

  fun convertToEntityAttribute(dbData: String?): DeliverWorkerModelStatus? = DeliverWorkerModelStatus.fromString(dbData)
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
