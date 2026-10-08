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

import dev.yidafu.aqua.api.service.delivery.DeliveryAreaMutationService
import dev.yidafu.aqua.common.domain.model.DeliveryAreaModel
import dev.yidafu.aqua.common.exception.NotFoundException
import dev.yidafu.aqua.delivery.domain.repository.DeliveryAreaRepository
import dev.yidafu.aqua.logging.util.BizLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryAreaMutationServiceImpl(
  private val areaRepository: DeliveryAreaRepository,
  private val bizLogger: BizLogger,
) : DeliveryAreaMutationService {
  @Transactional
  override fun createDeliveryArea(area: DeliveryAreaModel): DeliveryAreaModel {
    val saved = areaRepository.save(area)

    // 业务日志(F-LOG-003 DELIVERY 域):配送区域创建
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logDeliveryOperation(
      deliveryId = saved.id!!,
      operation = "DELIVERY_AREA_CREATED",
      result = "SUCCESS",
      userId = operatorId,
      username = operatorName,
      additionalData = mapOf("province" to saved.province, "city" to saved.city, "district" to saved.district),
    )
    return saved
  }

  @Transactional
  override fun updateDeliveryArea(
    areaId: Long,
    enabled: Boolean,
  ): DeliveryAreaModel {
    val area =
      areaRepository.findById(areaId).orElseThrow {
        NotFoundException("配送区域不存在: $areaId")
      }
    val before = area.enabled
    area.enabled = enabled
    areaRepository.save(area)

    // 业务日志(F-LOG-003 DELIVERY 域):配送区域更新
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logDeliveryOperation(
      deliveryId = area.id!!,
      operation = "DELIVERY_AREA_UPDATED",
      result = "SUCCESS",
      userId = operatorId,
      username = operatorName,
      additionalData = mapOf("from" to before, "to" to enabled),
    )
    return area
  }
}
