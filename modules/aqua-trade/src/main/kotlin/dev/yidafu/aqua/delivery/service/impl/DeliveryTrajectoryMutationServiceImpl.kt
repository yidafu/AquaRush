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

package dev.yidafu.aqua.delivery.service.impl

import dev.yidafu.aqua.api.service.delivery.DeliveryTrajectoryMutationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.DeliveryTrajectoryModel
import dev.yidafu.aqua.common.domain.model.enums.OrderModelStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryTrajectoryRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 配送轨迹上报服务实现(PRD 05-address-delivery.md §F-ADR-012,R-ADR-03)
 *
 * 服务端强制校验:
 * - 订单存在且处于 DELIVERING(「非配送中上报丢弃」,与状态变更并发以服务端状态为准);
 * - 上报人必须是该订单的配送员(本人订单,防越权);
 * - 经纬度合法性(经度 [-180,180]、纬度 [-90,90]),非法坐标拒绝。
 */
@Service
class DeliveryTrajectoryMutationServiceImpl(
  private val trajectoryRepository: DeliveryTrajectoryRepository,
  private val orderQueryService: OrderQueryService,
) : DeliveryTrajectoryMutationService {
  private val logger = LoggerFactory.getLogger(DeliveryTrajectoryMutationService::class.java)

  @Transactional
  override fun reportTrajectory(
    deliveryWorkerId: Long,
    orderId: Long,
    longitude: Double,
    latitude: Double,
    accuracy: Double?,
    reportedAt: LocalDateTime?,
  ): DeliveryTrajectoryModel {
    // 坐标合法性(经纬度范围校验)
    if (!longitude.isFinite() || longitude < -180.0 || longitude > 180.0) {
      throw BadRequestException("经度不合法: $longitude")
    }
    if (!latitude.isFinite() || latitude < -90.0 || latitude > 90.0) {
      throw BadRequestException("纬度不合法: $latitude")
    }
    accuracy?.let {
      if (!it.isFinite() || it < 0.0) {
        throw BadRequestException("定位精度不合法: $it")
      }
    }

    // 订单存在性校验(不存在视为订单状态不合法,统一提示)
    val order =
      try {
        orderQueryService.getOrderById(orderId)
      } catch (e: NotFoundException) {
        throw BadRequestException("订单不存在或不在配送中,无法上报轨迹")
      }

    // R-ADR-03:仅 DELIVERING 状态上报,非配送中上报丢弃
    if (order.status != OrderModelStatus.DELIVERING) {
      throw BadRequestException("订单不在配送中,无法上报轨迹")
    }

    // 本人订单校验(防越权:只能上报自己配送中的订单)
    if (order.deliveryWorkerId == null || order.deliveryWorkerId != deliveryWorkerId) {
      throw BadRequestException("只能上报本人配送中订单的轨迹")
    }

    val point =
      DeliveryTrajectoryModel(
        deliveryWorkerId = deliveryWorkerId,
        orderId = orderId,
        longitude = longitude,
        latitude = latitude,
        accuracy = accuracy,
        reportedAt = reportedAt ?: LocalDateTime.now(),
      )
    val saved = trajectoryRepository.save(point)
    logger.debug("Trajectory point saved: worker=$deliveryWorkerId order=$orderId ($longitude,$latitude)")
    return saved
  }
}
