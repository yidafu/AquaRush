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

import dev.yidafu.aqua.common.graphql.generated.UserRole
import dev.yidafu.aqua.common.graphql.generated.UserStatus
import dev.yidafu.aqua.common.util.MoneyUtils
import java.math.BigDecimal
import java.time.LocalDateTime

open class UserModel(
  var id: Long? = null,
  val wechatOpenId: String = "",
  // 对齐 DDL users.wechat_unionid(实体历史缺失,Hibernate 建表少列导致 Exposed 读炸)
  var wechatUnionid: String? = null,
  var nickname: String? = null,
  var phone: String? = null,
  var avatarUrl: String? = null,
  var email: String = "",
  // 后台用户管理可编辑状态(F-USR-022;微信登录创建时为 ACTIVE)
  var status: UserStatus = UserStatus.INACTIVE,
  val role: UserRole = UserRole.NONE,
  val balanceCents: Long = 0L,
  val totalSpentCents: Long = 0L,
  // 所属客户分组(单分组,空=未分组;营销受众维度,PRD 10-marketing.md §F-MKT-001)
  var customerGroupId: Long? = null,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  var lastLoginAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable {
  // Backward compatibility property
  val balance: BigDecimal
    get() = MoneyUtils.fromCents(balanceCents)

  // Backward compatibility property
  val totalSpent: BigDecimal
    get() = MoneyUtils.fromCents(totalSpentCents)
}
