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

import dev.yidafu.aqua.common.domain.model.BusinessLogModel
import org.springframework.stereotype.Repository

/**
 * 业务日志仓储(`business_logs`,logging 域)。
 *
 * JPA→Exposed 迁移 P5:剥离 `JpaRepository`,仅声明实际被调用的方法
 * (原 `save` 继承自 JpaRepository);日志表**无软删列**,查询即全量可见。
 * 多条件过滤分页见 [BusinessLogRepositoryCustom]。
 */
@Repository
interface BusinessLogRepository : BusinessLogRepositoryCustom {
  /** upsert 语义(对齐 JPA `save`):无 id 或 id 不存在时插入雪花 id,否则按 id 覆盖业务列。 */
  fun save(entity: BusinessLogModel): BusinessLogModel

  /** 按关联 id 取同一请求链路的所有日志(created_at 升序)。 */
  fun findByCorrelationId(correlationId: String): List<BusinessLogModel>

  /** 按操作标识精确查询(OPS 审计用例:同操作同步写入 business_logs)。 */
  fun findByOperation(operation: String): List<BusinessLogModel>
}
