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

package dev.yidafu.aqua.logging.repository

import dev.yidafu.aqua.common.domain.model.UserActionLogModel
import org.springframework.stereotype.Repository

/**
 * 用户操作日志仓储(`user_action_logs`,logging 域)。
 *
 * JPA→Exposed 迁移 P5:剥离 `JpaRepository`,仅声明实际被调用的方法
 * (原 `save`/`count`/`deleteAll` 继承自 JpaRepository);日志表**无软删列**,
 * `deleteAll` 是硬删(与实体无 `@SoftDelete` 一致),测试用它做用例间清理。
 * 复杂过滤/聚合见 [UserActionLogRepositoryCustom]。
 */
@Repository
interface UserActionLogRepository : UserActionLogRepositoryCustom {
  /** upsert 语义(对齐 JPA `save`):无 id 或 id 不存在时插入雪花 id,否则按 id 覆盖业务列。 */
  fun save(entity: UserActionLogModel): UserActionLogModel

  /** 总行数(集成测试轮询异步落库用)。 */
  fun count(): Long

  /** 清空全部行为日志(硬删),返回受影响行数。 */
  fun deleteAll(): Int

  /** 按用户查询(user_id 为字符串,前端上报的匿名 id 也用同一列)。 */
  fun findByUserId(userId: String): List<UserActionLogModel>

  /** 按行为类型查询(审计断言:登录/点击等)。 */
  fun findByActionType(actionType: String): List<UserActionLogModel>
}
