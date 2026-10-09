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

package dev.yidafu.aqua.delivery.domain.repository

import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import java.time.LocalDateTime

/**
 * 配送轨迹点仓库(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03)
 *
 * 追加式日志数据(不做软删除);delivery_worker_id 即账号 id(admins 单表,迁移 v1.2.1)。
 * JPA→Exposed 迁移 P4:剥离 JpaRepository,仅保留实际被调用的方法;
 * 轨迹表为裸 Table 无软删列,查询不带 is_deleted 过滤。
 */
interface DeliveryTrajectoryRepository {
  /**
   * 查询送水工在某订单上的轨迹点(按上报时间升序,轨迹回放顺序)。
   * 同秒上报以 id 升序破平。
   */
  fun findByDeliveryWorkerIdAndOrderIdOrderByReportedAtAsc(
    deliveryWorkerId: Long,
    orderId: Long,
  ): List<DeliveryTrajectoryModel>

  /**
   * 查询送水工在时间范围内的全部轨迹点(按上报时间升序,后台轨迹查看)。
   * 口径同 JPA Between:[start, end] 闭区间;同秒上报以 id 升序破平。
   */
  fun findByDeliveryWorkerIdAndReportedAtBetweenOrderByReportedAtAsc(
    deliveryWorkerId: Long,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<DeliveryTrajectoryModel>

  /**
   * 查询送水工全部轨迹点(按上报时间升序;不限定时间范围)。
   * 同秒上报以 id 升序破平。
   */
  fun findByDeliveryWorkerIdOrderByReportedAtAsc(deliveryWorkerId: Long): List<DeliveryTrajectoryModel>

  /**
   * 查询送水工最近一个轨迹点(实时位置);同秒上报以 id 降序取最晚落库行。
   */
  fun findFirstByDeliveryWorkerIdOrderByReportedAtDesc(deliveryWorkerId: Long): DeliveryTrajectoryModel?

  /** upsert 语义:无 id 或 id 不存在时插入(雪花 id),否则按 id 更新业务列。 */
  fun save(entity: DeliveryTrajectoryModel): DeliveryTrajectoryModel

  /** 批量 upsert,**整个批次包在单个数据库事务内**(原子:全成或全败)。 */
  fun saveAll(entities: List<DeliveryTrajectoryModel>): List<DeliveryTrajectoryModel>
}
