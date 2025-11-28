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

package dev.yidafu.aqua.logging.formatter

import java.time.format.DateTimeFormatter

/**
 * 业务日志格式化器，专门用于格式化业务相关的日志事件
 */
class BusinessLogFormatter {
  private val dateFormatter = DateTimeFormatter.ISO_INSTANT
  private val structuredFormatter = StructuredLogFormatter.instance

  /**
   * 格式化业务操作日志
   */
  fun formatBusinessOperation(
    operation: String,
    module: String,
    result: String,
    businessId: String? = null,
    description: String? = null,
    metadata: Map<String, Any> = emptyMap(),
  ): String {
    val additionalData = mutableMapOf<String, Any>()

    // 业务特定字段
    additionalData["businessOperation"] = operation
    additionalData["businessModule"] = module
    additionalData["businessResult"] = result
    additionalData["businessCategory"] = determineBusinessCategory(operation, module)

    businessId?.let { additionalData["businessId"] = it }
    description?.let { additionalData["businessDescription"] = it }

    if (metadata.isNotEmpty()) {
      additionalData["businessMetadata"] = metadata
    }

    // 根据业务模块添加特定的上下文
    addModuleSpecificContext(additionalData, module, metadata)

    val level =
      when (result.uppercase()) {
        "SUCCESS", "COMPLETED" -> "INFO"
        "FAILURE", "ERROR", "FAILED" -> "ERROR"
        "PARTIAL", "WARNING" -> "WARN"
        else -> "INFO"
      }

    val message = description ?: "Business operation '$operation' in module '$module' resulted in $result"

    return structuredFormatter.format(level, "business", message, null, additionalData)
  }

  /**
   * 格式化订单相关日志
   */
  fun formatOrderLog(
    operation: String,
    orderId: String,
    userId: String? = null,
    status: String,
    details: Map<String, Any> = emptyMap(),
  ): String {
    val additionalData = mutableMapOf<String, Any>()

    additionalData["businessOperation"] = operation
    additionalData["businessModule"] = "ORDER"
    additionalData["businessResult"] = status
    additionalData["businessCategory"] = "ORDER_MANAGEMENT"
    additionalData["orderId"] = orderId
    additionalData["orderStatus"] = status

    userId?.let { additionalData["customerId"] = it }

    if (details.isNotEmpty()) {
      additionalData["orderDetails"] = details
    }

    val level =
      when (status.uppercase()) {
        "COMPLETED", "CONFIRMED" -> "INFO"
        "FAILED", "ERROR", "CANCELLED" -> "ERROR"
        "PENDING", "PROCESSING" -> "INFO"
        "PARTIAL" -> "WARN"
        else -> "INFO"
      }

    val message = "Order $orderId: $operation resulted in status $status"

    return structuredFormatter.format(level, "order", message, null, additionalData)
  }

  /**
   * 格式化支付相关日志
   */
  fun formatPaymentLog(
    operation: String,
    paymentId: String,
    orderId: String,
    amount: Double,
    status: String,
    paymentMethod: String? = null,
    details: Map<String, Any> = emptyMap(),
  ): String {
    val additionalData = mutableMapOf<String, Any>()

    additionalData["businessOperation"] = operation
    additionalData["businessModule"] = "PAYMENT"
    additionalData["businessResult"] = status
    additionalData["businessCategory"] = "PAYMENT_PROCESSING"
    additionalData["paymentId"] = paymentId
    additionalData["orderId"] = orderId
    additionalData["amount"] = amount
    additionalData["currency"] = "CNY" // 假设是人民币

    paymentMethod?.let { additionalData["paymentMethod"] = it }

    if (details.isNotEmpty()) {
      additionalData["paymentDetails"] = details
    }

    val level =
      when (status.uppercase()) {
        "SUCCESS", "COMPLETED" -> "INFO"
        "FAILED", "ERROR", "DECLINED" -> "ERROR"
        "PENDING", "PROCESSING" -> "INFO"
        "REFUNDED" -> "INFO"
        else -> "INFO"
      }

    val message = "Payment $paymentId for order $orderId: $operation of amount $amount resulted in $status"

    return structuredFormatter.format(level, "payment", message, null, additionalData)
  }

  /**
   * 格式化配送相关日志
   */
  fun formatDeliveryLog(
    operation: String,
    deliveryId: String,
    orderId: String,
    status: String,
    workerId: String? = null,
    details: Map<String, Any> = emptyMap(),
  ): String {
    val additionalData = mutableMapOf<String, Any>()

    additionalData["businessOperation"] = operation
    additionalData["businessModule"] = "DELIVERY"
    additionalData["businessResult"] = status
    additionalData["businessCategory"] = "DELIVERY_MANAGEMENT"
    additionalData["deliveryId"] = deliveryId
    additionalData["orderId"] = orderId
    additionalData["deliveryStatus"] = status

    workerId?.let { additionalData["workerId"] = it }

    if (details.isNotEmpty()) {
      additionalData["deliveryDetails"] = details
    }

    val level =
      when (status.uppercase()) {
        "COMPLETED", "DELIVERED" -> "INFO"
        "FAILED", "ERROR" -> "ERROR"
        "PENDING", "ASSIGNED", "IN_PROGRESS" -> "INFO"
        "CANCELLED" -> "WARN"
        else -> "INFO"
      }

    val message = "Delivery $deliveryId for order $orderId: $operation resulted in status $status"

    return structuredFormatter.format(level, "delivery", message, null, additionalData)
  }

  /**
   * 格式化用户相关日志
   */
  fun formatUserLog(
    operation: String,
    userId: String,
    result: String,
    username: String? = null,
    details: Map<String, Any> = emptyMap(),
  ): String {
    val additionalData = mutableMapOf<String, Any>()

    additionalData["businessOperation"] = operation
    additionalData["businessModule"] = "USER"
    additionalData["businessResult"] = result
    additionalData["businessCategory"] = "USER_MANAGEMENT"
    additionalData["targetUserId"] = userId

    username?.let { additionalData["targetUsername"] = it }

    if (details.isNotEmpty()) {
      additionalData["userDetails"] = details
    }

    val level =
      when (result.uppercase()) {
        "SUCCESS", "COMPLETED" -> "INFO"
        "FAILED", "ERROR" -> "ERROR"
        "PENDING" -> "INFO"
        else -> "INFO"
      }

    val message = "User $userId: $operation resulted in $result"

    return structuredFormatter.format(level, "user", message, null, additionalData)
  }

  /**
   * 格式化产品相关日志
   */
  fun formatProductLog(
    operation: String,
    productId: String,
    result: String,
    productName: String? = null,
    details: Map<String, Any> = emptyMap(),
  ): String {
    val additionalData = mutableMapOf<String, Any>()

    additionalData["businessOperation"] = operation
    additionalData["businessModule"] = "PRODUCT"
    additionalData["businessResult"] = result
    additionalData["businessCategory"] = "PRODUCT_MANAGEMENT"
    additionalData["productId"] = productId

    productName?.let { additionalData["productName"] = it }

    if (details.isNotEmpty()) {
      additionalData["productDetails"] = details
    }

    val level =
      when (result.uppercase()) {
        "SUCCESS", "CREATED", "UPDATED" -> "INFO"
        "FAILED", "ERROR" -> "ERROR"
        "DELETED" -> "INFO"
        else -> "INFO"
      }

    val message = "Product $productId: $operation resulted in $result"

    return structuredFormatter.format(level, "product", message, null, additionalData)
  }

  /**
   * 根据操作和模块确定业务分类
   */
  private fun determineBusinessCategory(
    operation: String,
    module: String,
  ): String =
    when (module.uppercase()) {
      "ORDER" ->
        when {
          operation.contains("CREATE") -> "ORDER_CREATION"
          operation.contains("UPDATE") -> "ORDER_UPDATE"
          operation.contains("CANCEL") -> "ORDER_CANCELLATION"
          operation.contains("STATUS") -> "ORDER_STATUS_CHANGE"
          else -> "ORDER_MANAGEMENT"
        }
      "PAYMENT" ->
        when {
          operation.contains("CREATE") -> "PAYMENT_PROCESSING"
          operation.contains("REFUND") -> "PAYMENT_REFUND"
          operation.contains("VERIFY") -> "PAYMENT_VERIFICATION"
          else -> "PAYMENT_MANAGEMENT"
        }
      "DELIVERY" ->
        when {
          operation.contains("ASSIGN") -> "DELIVERY_ASSIGNMENT"
          operation.contains("COMPLETE") -> "DELIVERY_COMPLETION"
          operation.contains("STATUS") -> "DELIVERY_STATUS_CHANGE"
          else -> "DELIVERY_MANAGEMENT"
        }
      "USER" ->
        when {
          operation.contains("CREATE") -> "USER_REGISTRATION"
          operation.contains("AUTH") -> "USER_AUTHENTICATION"
          operation.contains("UPDATE") -> "USER_UPDATE"
          else -> "USER_MANAGEMENT"
        }
      "PRODUCT" ->
        when {
          operation.contains("CREATE") -> "PRODUCT_CREATION"
          operation.contains("UPDATE") -> "PRODUCT_UPDATE"
          operation.contains("DELETE") -> "PRODUCT_DELETION"
          else -> "PRODUCT_MANAGEMENT"
        }
      else -> "GENERAL_BUSINESS"
    }

  /**
   * 根据业务模块添加特定的上下文信息
   */
  private fun addModuleSpecificContext(
    additionalData: MutableMap<String, Any>,
    module: String,
    metadata: Map<String, Any>,
  ) {
    when (module.uppercase()) {
      "ORDER" -> {
        additionalData["businessDomain"] = "ORDER_MANAGEMENT"
        additionalData["workflowStage"] = metadata["workflowStage"] ?: "UNKNOWN"
      }
      "PAYMENT" -> {
        additionalData["businessDomain"] = "PAYMENT_PROCESSING"
        additionalData["transactionType"] = metadata["transactionType"] ?: "PAYMENT"
      }
      "DELIVERY" -> {
        additionalData["businessDomain"] = "DELIVERY_LOGISTICS"
        additionalData["deliveryType"] = metadata["deliveryType"] ?: "STANDARD"
      }
      "USER" -> {
        additionalData["businessDomain"] = "USER_MANAGEMENT"
        additionalData["userType"] = metadata["userType"] ?: "CUSTOMER"
      }
      "PRODUCT" -> {
        additionalData["businessDomain"] = "PRODUCT_MANAGEMENT"
        additionalData["productType"] = metadata["productType"] ?: "WATER"
      }
    }
  }

  companion object {
    @JvmStatic
    val instance: BusinessLogFormatter by lazy { BusinessLogFormatter() }
  }
}
