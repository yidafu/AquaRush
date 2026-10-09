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

package dev.yidafu.aqua.notice.domain.repository

import dev.yidafu.aqua.common.domain.model.UserNotificationSettingsModel
import java.util.Optional

/** 用户通知开关仓储(JPA→Exposed 迁移 P3,原 JpaRepository 派生查询已显式化)。 */
interface UserNotificationSettingsRepository {
  fun findByUserId(userId: Long): Optional<UserNotificationSettingsModel>

  fun existsByUserId(userId: Long): Boolean

  /** 语义对齐 @SoftDelete 下的派生 delete:按 user_id 软删,返回影响行数。 */
  fun deleteByUserId(userId: Long): Int

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: UserNotificationSettingsModel): UserNotificationSettingsModel
}
