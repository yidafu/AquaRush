package dev.yidafu.aqua.api.service.order

import dev.yidafu.aqua.common.domain.model.OrderOperationModel
import dev.yidafu.aqua.common.domain.model.enums.OperatorType
import dev.yidafu.aqua.common.domain.model.enums.OrderOperationType

/**
 * 订单操作记录服务接口
 */
interface OrderOperationService {
  /**
   * 记录订单操作
   *
   * @param orderId 订单ID
   * @param operationType 操作类型
   * @param operatorId 操作人ID (可选)
   * @param operatorType 操作人类型
   * @param description 操作描述 (可选)
   * @param extraData 额外数据，JSON格式 (可选)
   * @return 创建的操作记录
   */
  fun recordOperation(
    orderId: Long,
    operationType: OrderOperationType,
    operatorType: OperatorType,
    operatorId: Long? = null,
    description: String? = null,
    extraData: String? = null,
  ): OrderOperationModel

  /**
   * 获取订单的操作历史
   *
   * @param orderId 订单ID
   * @return 操作记录列表，按时间倒序
   */
  fun getOrderOperations(orderId: Long): List<OrderOperationModel>

  /**
   * 统计订单的操作记录数量
   *
   * @param orderId 订单ID
   * @return 操作记录数量
   */
  fun countOrderOperations(orderId: Long): Long
}
