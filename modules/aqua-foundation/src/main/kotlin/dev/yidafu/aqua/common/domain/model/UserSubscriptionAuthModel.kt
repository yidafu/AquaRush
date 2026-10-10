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

import java.time.LocalDateTime

/**
 * 微信订阅消息授权记录(R-NTF-01)
 *
 * 一次性模板:用户每授权一次可发送一次(remaining_count=1),发送成功后消耗;
 * 达上限/拒绝后不再弹窗授权(微信规范),未授权用户不推送,降级站内查看。
 */
open class UserSubscriptionAuthModel(
  var id: Long? = null,
  val userId: Long,
  val templateId: String,
  var remainingCount: Int = 1,
  val createdAt: LocalDateTime = LocalDateTime.now(),
  var updatedAt: LocalDateTime = LocalDateTime.now(),
  override var deletedAt: LocalDateTime? = null,
  override var deletedBy: Long? = null,
) : SoftDeletable {
  /** 消耗一次授权(一次性模板),返回是否还有剩余 */
  fun consume(): Boolean {
    if (remainingCount <= 0) return false
    remainingCount -= 1
    return true
  }
}
