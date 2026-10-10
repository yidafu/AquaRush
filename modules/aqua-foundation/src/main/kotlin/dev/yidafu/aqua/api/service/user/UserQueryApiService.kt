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

package dev.yidafu.aqua.api.service.user

import dev.yidafu.aqua.common.domain.model.UserModel
import java.time.LocalDateTime

/**
 * 跨模块用户查询服务接口
 * 由 aqua-user 模块实现，供其他模块使用
 */
interface UserQueryApiService {
  /**
   * 用户总数(委托 JpaRepository.count(),统计场景避免全表加载)
   */
  fun countAll(): Long

  fun findById(id: Long): UserModel?

  fun findByIds(ids: List<Long>): List<UserModel>

  fun findByPhone(phone: String): UserModel?

  /** 按微信 openid 查找用户(WebSocket 推送会话鉴权用,F-NTF-003) */
  fun findByWechatOpenId(openid: String): UserModel?

  fun existsById(id: Long): Boolean

  fun findUsersCreatedAfter(startDate: LocalDateTime): List<UserModel>

  fun findUsersCreatedBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<UserModel>

  fun countUsersCreatedBetween(
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): Long

  // ===== 客户分组(F-MKT-001,营销受众维度)=====
  fun findByCustomerGroupId(groupId: Long): List<UserModel>

  fun countUsersByCustomerGroupId(groupId: Long): Long
}
