/*
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

package dev.yidafu.aqua.order.service.impl

import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.order.domain.repository.OrderOperationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class OrderOperationServiceImpl(
  private val orderOperationRepository: OrderOperationRepository,
) : OrderOperationService {
  private val logger = LoggerFactory.getLogger(OrderOperationServiceImpl::class.java)

  override fun recordOperation(
    orderId: Long,
    operationType: OrderOperationType,
    operatorType: OperatorType,
    operatorId: Long?,
    description: String?,
    extraData: String?,
  ): OrderOperationModel {
    val operation =
      OrderOperationModel(
        orderId = orderId,
        operationType = operationType,
        operatorId = operatorId,
        operatorType = operatorType,
        description = description,
        extraData = extraData,
        createdAt = LocalDateTime.now(),
      )

    val savedOperation = orderOperationRepository.save(operation)
    logger.info(
      "Recorded order operation: orderId={}, operationType={}, operatorType={}",
      orderId,
      operationType,
      operatorType,
    )

    return savedOperation
  }

  override fun getOrderOperations(orderId: Long): List<OrderOperationModel> =
    orderOperationRepository.findByOrderIdOrderByCreatedAtDesc(orderId)

  override fun countOrderOperations(orderId: Long): Long = orderOperationRepository.countByOrderId(orderId)
}
