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

import dev.yidafu.aqua.api.service.delivery.DeliveryWorkerMutationService
import dev.yidafu.aqua.common.domain.model.DeliverWorkerModelStatus
import dev.yidafu.aqua.common.domain.model.DeliveryWorkerModel
import dev.yidafu.aqua.delivery.domain.repository.DeliveryWorkerRepository
import dev.yidafu.aqua.logging.util.BizLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryWorkerMutationServiceImpl(
  private val workerRepository: DeliveryWorkerRepository,
  private val bizLogger: BizLogger,
) : DeliveryWorkerMutationService {
  override fun createDeliveryWorker(
    adminId: Long,
    name: String,
    phone: String,
    wechatOpenId: String,
  ): DeliveryWorkerModel {
    val worker =
      DeliveryWorkerModel(
        userId = null,
        adminId = adminId,
        wechatOpenId = wechatOpenId,
        name = name,
        phone = phone,
        onlineStatus = DeliverWorkerModelStatus.OFFLINE,
        isAvailable = true,
      )
    val saved = workerRepository.save(worker)

    // 业务日志(F-LOG-003 DELIVERY 域):配送员创建
    bizLogger.logDeliveryOperation(
      deliveryId = saved.id!!,
      operation = "DELIVERY_WORKER_CREATED",
      result = "SUCCESS",
      userId = adminId,
      additionalData = mapOf("name" to name, "phone" to phone),
    )
    return saved
  }

  @Transactional
  override fun updateWorkerStatus(
    workerId: Long,
    status: DeliverWorkerModelStatus,
  ): DeliveryWorkerModel {
    val worker =
      workerRepository.findById(workerId).orElseThrow {
        throw IllegalArgumentException("配送员不存在: $workerId")
      }
    val before = worker.onlineStatus
    worker.onlineStatus = status
    val saved = workerRepository.save(worker)

    // 业务日志(F-LOG-003 DELIVERY 域):配送员状态变更
    val (operatorId, operatorName) = bizLogger.resolveOperator()
    bizLogger.logDeliveryOperation(
      deliveryId = saved.id!!,
      operation = "DELIVERY_WORKER_STATUS_CHANGED",
      result = "SUCCESS",
      userId = operatorId ?: saved.adminId,
      username = operatorName,
      additionalData = mapOf("from" to before.name, "to" to status.name),
    )
    return saved
  }
}
