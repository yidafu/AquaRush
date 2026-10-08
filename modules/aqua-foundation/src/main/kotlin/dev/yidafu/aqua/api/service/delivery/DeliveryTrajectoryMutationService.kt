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

package dev.yidafu.aqua.api.service.delivery

import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import java.time.LocalDateTime

/**
 * 配送轨迹上报服务接口(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03)
 *
 * 送水工在订单「配送中」期间按设定频率(默认 5s)自动上报实时位置;
 * 「开始配送」启动上报、「完成配送/订单取消」停止。服务端强制校验:
 * - 订单存在且处于 DELIVERING(非配送中上报丢弃);
 * - 上报人必须是该订单的配送员(本人订单,防越权)。
 */
interface DeliveryTrajectoryMutationService {
  /**
   * 上报一个轨迹点
   *
   * @param deliveryWorkerId 送水工账号 id(admins 单表)
   * @param orderId 关联订单
   * @param longitude 经度
   * @param latitude 纬度
   * @param accuracy 定位精度(米,可空)
   * @param reportedAt 上报时间(客户端定位产生时间;为空取服务端当前时间)
   * @return 落库后的轨迹点;校验失败抛 BadRequestException
   */
  fun reportTrajectory(
    deliveryWorkerId: Long,
    orderId: Long,
    longitude: Double,
    latitude: Double,
    accuracy: Double?,
    reportedAt: LocalDateTime?,
  ): DeliveryTrajectoryModel
}
