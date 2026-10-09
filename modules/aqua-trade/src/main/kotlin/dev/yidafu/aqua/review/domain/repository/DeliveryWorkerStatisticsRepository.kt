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

package dev.yidafu.aqua.review.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryWorkerStatisticsModel

/**
 * 送水员评价统计仓储(JPA→Exposed 迁移 P4):剥离 JpaRepository,仅保留实际被调用的方法;
 * 查询过滤 is_deleted(对齐 @SoftDelete)。
 */
interface DeliveryWorkerStatisticsRepository : DeliveryWorkerStatisticsRepositoryCustom {
  fun findByDeliveryWorkerId(deliveryWorkerId: Long): DeliveryWorkerStatisticsModel?

  /**
   * upsert 语义:无 id 或 id 不存在时插入(雪花 id;INSERT 显式写入 last_updated——
   * DDL 无列默认),否则按 id 更新业务列(last_updated 由库触发器刷新,UPDATE 不写它)。
   */
  fun save(entity: DeliveryWorkerStatisticsModel): DeliveryWorkerStatisticsModel
}
