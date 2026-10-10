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
 * 配送轨迹点实体(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03)
 *
 * 送水工在订单「配送中」期间按设定频率(默认 5s)自动上报的实时位置点,
 * 「开始配送」启动上报、「完成配送/订单取消」停止;轨迹点持久化形成轨迹序列可回溯。
 *
 * 设计说明:
 * - 追加式日志数据(参考 OrderOperationModel),不做软删除;
 * - delivery_worker_id 即账号 id(送水员档案并入 admins 单表,迁移 v1.2.1);
 * - 经度/纬度用 double 精度存储(数据库 double 列),GraphQL 投影按 Float 输出(与 Address 一致);
 * - 仅管理员可查询(R-AUTH-02 权限隔离,resolver 层强制)。
 */
class DeliveryTrajectoryModel(
  var id: Long? = null,
  /** 送水工账号 id(admins 单表,迁移 v1.2.1) */
  val deliveryWorkerId: Long,
  /** 关联订单 */
  val orderId: Long,
  /** 经度(WGS84/GCJ02 由客户端定位提供,展示用) */
  val longitude: Double,
  /** 纬度 */
  val latitude: Double,
  /** 定位精度(米,可空) */
  val accuracy: Double? = null,
  /** 上报时间(客户端定位产生时间,服务端落库时间见 createdAt) */
  val reportedAt: LocalDateTime = LocalDateTime.now(),
  /** 服务端落库时间 */
  val createdAt: LocalDateTime = LocalDateTime.now(),
)
