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

package dev.yidafu.aqua.order.service

import dev.yidafu.aqua.api.service.order.OrderMutationService
import dev.yidafu.aqua.api.service.order.OrderOperationService
import dev.yidafu.aqua.api.service.order.OrderQueryService
import dev.yidafu.aqua.common.domain.model.OrderModel
import dev.yidafu.aqua.common.domain.model.enums.DeliveryPhotoReviewStatus
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.common.graphql.generated.DeliveryPhotoReviewAction
import dev.yidafu.aqua.logging.util.BizLogger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 配送照片核验服务(G23/F-ORD-021 后台「通过/驳回」核验闭环):
 * 前置校验(照片非空、驳回必填原因)、核验留痕(订单操作时间线 + 业务日志)、幂等可纠错。
 */
@Service
class DeliveryPhotoReviewService(
  private val orderQueryService: OrderQueryService,
  private val orderMutationService: OrderMutationService,
  private val orderOperationService: OrderOperationService,
  private val bizLogger: BizLogger,
) {
  private val logger = LoggerFactory.getLogger(DeliveryPhotoReviewService::class.java)

  @Transactional
  fun reviewDeliveryPhoto(
    orderId: Long,
    action: DeliveryPhotoReviewAction,
    remark: String?,
    adminId: Long,
    username: String,
  ): OrderModel {
    val order = orderQueryService.getOrderById(orderId)

    // 前置校验:订单必须有配送照片(R-ORD-07 完成配送强制照片)
    if (order.deliveryPhotos.isNullOrBlank()) {
      throw BadRequestException("该订单无配送照片,无法核验")
    }
    // 驳回必填原因(核验留痕可追溯)
    if (action == DeliveryPhotoReviewAction.REJECT && remark.isNullOrBlank()) {
      throw BadRequestException("驳回必须填写原因")
    }

    val now = LocalDateTime.now()
    val newStatus =
      when (action) {
        DeliveryPhotoReviewAction.APPROVE -> DeliveryPhotoReviewStatus.APPROVED
        DeliveryPhotoReviewAction.REJECT -> DeliveryPhotoReviewStatus.REJECTED
      }
    // 幂等:重复核验覆盖上次结果并更新核验时间(可纠错)
    order.deliveryPhotoReviewStatus = newStatus
    order.deliveryPhotoReviewedAt = now
    order.deliveryPhotoReviewedBy = adminId
    order.deliveryPhotoReviewRemark = remark
    val saved = orderMutationService.updateOrder(order.id!!, order)

    // 留痕:订单操作时间线(核验通过/驳回)
    orderOperationService.recordOperation(
      orderId = saved.id!!,
      operationType =
        when (action) {
          DeliveryPhotoReviewAction.APPROVE -> OrderOperationType.DELIVERY_PHOTO_APPROVED
          DeliveryPhotoReviewAction.REJECT -> OrderOperationType.DELIVERY_PHOTO_REJECTED
        },
      operatorType = OperatorType.ADMIN,
      operatorId = adminId,
      description = remark ?: "配送照片核验通过",
      extraData = """{"action": "${action.name}", "remark": "${remark ?: ""}"}""",
    )

    // 留痕:业务日志(F-LOG-003 ORDER 域)
    bizLogger.logOperation(
      operation = "ORDER_PHOTO_REVIEW",
      module = "ORDER",
      result = "SUCCESS",
      target = saved.id.toString(),
      userId = adminId,
      username = username,
      additionalData = mapOf("orderId" to orderId, "action" to action.name, "remark" to (remark ?: "")),
    )

    logger.info("配送照片核验: orderId={}, action={}, adminId={}", orderId, action, adminId)
    return saved
  }
}
