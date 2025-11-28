package dev.yidafu.aqua.common.graphql.generated



data class AddressInput(
    val city: String,
    val detailAddress: String,
    val district: String,
    val isDefault: Boolean? = false,
    val phone: String,
    val province: String,
    val receiverName: String
) {
  constructor(args: Map<String, Any>) : this(
      args["city"] as String,
      args["detailAddress"] as String,
      args["district"] as String,
      args["isDefault"] as Boolean? ?: false,
      args["phone"] as String,
      args["province"] as String,
      args["receiverName"] as String
  )
}



data class CreateDeliveryAreaInput(
    val city: String,
    val district: String,
    val enabled: Boolean? = true,
    val name: String,
    val province: String
) {
  constructor(args: Map<String, Any>) : this(
      args["city"] as String,
      args["district"] as String,
      args["enabled"] as Boolean? ?: true,
      args["name"] as String,
      args["province"] as String
  )
}

data class CreateOrderInput(
    val addressId: java.lang.Long,
    val productId: java.lang.Long,
    val quantity: Int
) {
  constructor(args: Map<String, Any>) : this(
      args["addressId"] as java.lang.Long,
      args["productId"] as java.lang.Long,
      args["quantity"] as Int
  )
}

data class CreateProductInput(
    val coverImageUrl: String,
    val description: String? = null,
    val detailImages: String? = null,
    val name: String,
    val price: java.math.BigDecimal,
    val stock: Int
) {
  constructor(args: Map<String, Any>) : this(
      args["coverImageUrl"] as String,
      args["description"] as String?,
      args["detailImages"] as String?,
      args["name"] as String,
      args["price"] as java.math.BigDecimal,
      args["stock"] as Int
  )
}

data class CreateWechatPaymentInput(
    val amount: Int,
    val description: String,
    val orderId: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["amount"] as Int,
      args["description"] as String,
      args["orderId"] as java.lang.Long
  )
}





data class DateRangeInput(
    val endDate: java.time.LocalDateTime,
    val startDate: java.time.LocalDateTime
) {
  constructor(args: Map<String, Any>) : this(
      args["endDate"] as java.time.LocalDateTime,
      args["startDate"] as java.time.LocalDateTime
  )
}







data class MutationCancelOrderArgs(
    val orderId: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["orderId"] as java.lang.Long
  )
}
data class MutationCreateAddressArgs(
    val input: AddressInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      AddressInput(args["input"] as Map<String, Any>)
  )
}
data class MutationCreateDeliveryAreaArgs(
    val input: CreateDeliveryAreaInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      CreateDeliveryAreaInput(args["input"] as Map<String, Any>)
  )
}
data class MutationCreateOrderArgs(
    val input: CreateOrderInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      CreateOrderInput(args["input"] as Map<String, Any>)
  )
}
data class MutationCreateProductArgs(
    val input: CreateProductInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      CreateProductInput(args["input"] as Map<String, Any>)
  )
}
data class MutationCreateWechatPaymentArgs(
    val input: CreateWechatPaymentInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      CreateWechatPaymentInput(args["input"] as Map<String, Any>)
  )
}
data class MutationDecreaseStockArgs(
    val productId: java.lang.Long,
    val quantity: Int
) {
  constructor(args: Map<String, Any>) : this(
      args["productId"] as java.lang.Long,
      args["quantity"] as Int
  )
}
data class MutationDeleteAddressArgs(
    val id: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long
  )
}
data class MutationDeleteProductArgs(
    val id: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long
  )
}
data class MutationHandleWechatCallbackArgs(
    val input: WechatCallbackInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      WechatCallbackInput(args["input"] as Map<String, Any>)
  )
}
data class MutationIncreaseStockArgs(
    val productId: java.lang.Long,
    val quantity: Int
) {
  constructor(args: Map<String, Any>) : this(
      args["productId"] as java.lang.Long,
      args["quantity"] as Int
  )
}
data class MutationRefreshTokenArgs(
    val input: RefreshTokenInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      RefreshTokenInput(args["input"] as Map<String, Any>)
  )
}
data class MutationRefundArgs(
    val input: RefundInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      RefundInput(args["input"] as Map<String, Any>)
  )
}
data class MutationSetDefaultAddressArgs(
    val id: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long
  )
}
data class MutationUpdateAddressArgs(
    val id: java.lang.Long,
    val input: UpdateAddressInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long,
      UpdateAddressInput(args["input"] as Map<String, Any>)
  )
}
data class MutationUpdateOrderStatusArgs(
    val orderId: java.lang.Long,
    val status: OrderStatus
) {
  constructor(args: Map<String, Any>) : this(
      args["orderId"] as java.lang.Long,
      args["status"] as OrderStatus
  )
}
data class MutationUpdateProductArgs(
    val id: java.lang.Long,
    val input: UpdateProductInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long,
      UpdateProductInput(args["input"] as Map<String, Any>)
  )
}
data class MutationUpdateProfileArgs(
    val input: UpdateProfileInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      UpdateProfileInput(args["input"] as Map<String, Any>)
  )
}
data class MutationUpdateWorkerStatusArgs(
    val status: WorkerStatus,
    val workerId: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["status"] as WorkerStatus,
      args["workerId"] as java.lang.Long
  )
}
data class MutationWechatLoginArgs(
    val input: WechatLoginInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      WechatLoginInput(args["input"] as Map<String, Any>)
  )
}





enum class OrderStatus(val label: String) {
      Cancelled("CANCELLED"),
      Confirmed("CONFIRMED"),
      Delivered("DELIVERED"),
      OutForDelivery("OUT_FOR_DELIVERY"),
      Pending("PENDING"),
      Preparing("PREPARING"),
      ReadyForDelivery("READY_FOR_DELIVERY"),
      Refunded("REFUNDED");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): OrderStatus? {
      return values().find { it.label == label }
    }
  }
}



enum class PaymentMethod(val label: String) {
      Alipay("ALIPAY"),
      CashOnDelivery("CASH_ON_DELIVERY"),
      WechatPay("WECHAT_PAY");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): PaymentMethod? {
      return values().find { it.label == label }
    }
  }
}

enum class PaymentStatus(val label: String) {
      Failed("FAILED"),
      Processing("PROCESSING"),
      Refunded("REFUNDED"),
      Success("SUCCESS");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): PaymentStatus? {
      return values().find { it.label == label }
    }
  }
}



enum class ProductStatus(val label: String) {
      Offline("OFFLINE"),
      Online("ONLINE");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): ProductStatus? {
      return values().find { it.label == label }
    }
  }
}

data class QueryAddressArgs(
    val id: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long
  )
}
data class QueryDailyStatisticsArgs(
    val input: DateRangeInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      DateRangeInput(args["input"] as Map<String, Any>)
  )
}
data class QueryDeliveryAreaArgs(
    val id: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long
  )
}
data class QueryDeliveryWorkerArgs(
    val id: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long
  )
}
data class QueryMonthlyStatisticsArgs(
    val input: DateRangeInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      DateRangeInput(args["input"] as Map<String, Any>)
  )
}
data class QueryOrderArgs(
    val orderId: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["orderId"] as java.lang.Long
  )
}
data class QueryOrderByNumberArgs(
    val orderNumber: String
) {
  constructor(args: Map<String, Any>) : this(
      args["orderNumber"] as String
  )
}
data class QueryOrderStatisticsArgs(
    val input: DateRangeInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      DateRangeInput(args["input"] as Map<String, Any>)
  )
}
data class QueryOrdersByStatusArgs(
    val status: OrderStatus
) {
  constructor(args: Map<String, Any>) : this(
      args["status"] as OrderStatus
  )
}
data class QueryOrdersByUserArgs(
    val userId: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["userId"] as java.lang.Long
  )
}
data class QueryOrdersByUserAndStatusArgs(
    val status: OrderStatus,
    val userId: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["status"] as OrderStatus,
      args["userId"] as java.lang.Long
  )
}
data class QueryPaymentStatusArgs(
    val transactionId: String
) {
  constructor(args: Map<String, Any>) : this(
      args["transactionId"] as String
  )
}
data class QueryProductArgs(
    val id: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long
  )
}
data class QueryUserArgs(
    val id: java.lang.Long
) {
  constructor(args: Map<String, Any>) : this(
      args["id"] as java.lang.Long
  )
}
data class QueryValidateAddressArgs(
    val input: ValidateAddressInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      ValidateAddressInput(args["input"] as Map<String, Any>)
  )
}
data class QueryWeeklyStatisticsArgs(
    val input: DateRangeInput
) {
  @Suppress("UNCHECKED_CAST")
  constructor(args: Map<String, Any>) : this(
      DateRangeInput(args["input"] as Map<String, Any>)
  )
}

data class RefreshTokenInput(
    val refreshToken: String
) {
  constructor(args: Map<String, Any>) : this(
      args["refreshToken"] as String
  )
}

data class RefundInput(
    val refundAmount: Int,
    val totalAmount: Int,
    val transactionId: String
) {
  constructor(args: Map<String, Any>) : this(
      args["refundAmount"] as Int,
      args["totalAmount"] as Int,
      args["transactionId"] as String
  )
}

data class UpdateAddressInput(
    val city: String? = null,
    val detailAddress: String? = null,
    val district: String? = null,
    val isDefault: Boolean? = null,
    val phone: String? = null,
    val province: String? = null,
    val receiverName: String? = null
) {
  constructor(args: Map<String, Any>) : this(
      args["city"] as String?,
      args["detailAddress"] as String?,
      args["district"] as String?,
      args["isDefault"] as Boolean?,
      args["phone"] as String?,
      args["province"] as String?,
      args["receiverName"] as String?
  )
}

data class UpdateProductInput(
    val coverImageUrl: String? = null,
    val description: String? = null,
    val detailImages: String? = null,
    val name: String? = null,
    val price: java.math.BigDecimal? = null,
    val stock: Int? = null
) {
  constructor(args: Map<String, Any>) : this(
      args["coverImageUrl"] as String?,
      args["description"] as String?,
      args["detailImages"] as String?,
      args["name"] as String?,
      args["price"] as java.math.BigDecimal?,
      args["stock"] as Int?
  )
}

data class UpdateProfileInput(
    val avatar: String? = null,
    val nickname: String? = null,
    val phone: String? = null
) {
  constructor(args: Map<String, Any>) : this(
      args["avatar"] as String?,
      args["nickname"] as String?,
      args["phone"] as String?
  )
}





enum class UserRole(val label: String) {
      Admin("ADMIN"),
      None("NONE"),
      User("USER"),
      Worker("WORKER");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): UserRole? {
      return values().find { it.label == label }
    }
  }
}

enum class UserStatus(val label: String) {
      Active("ACTIVE"),
      Deleted("DELETED"),
      Inactive("INACTIVE"),
      Suspended("SUSPENDED");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): UserStatus? {
      return values().find { it.label == label }
    }
  }
}

data class ValidateAddressInput(
    val city: String,
    val district: String,
    val province: String
) {
  constructor(args: Map<String, Any>) : this(
      args["city"] as String,
      args["district"] as String,
      args["province"] as String
  )
}





data class WechatCallbackInput(
    val resource: String,
    val resourceType: String,
    val summary: String
) {
  constructor(args: Map<String, Any>) : this(
      args["resource"] as String,
      args["resourceType"] as String,
      args["summary"] as String
  )
}

data class WechatLoginInput(
    val code: String
) {
  constructor(args: Map<String, Any>) : this(
      args["code"] as String
  )
}



enum class WorkerStatus(val label: String) {
      Offline("OFFLINE"),
      Online("ONLINE");
        
  companion object {
    @JvmStatic
    fun valueOfLabel(label: String): WorkerStatus? {
      return values().find { it.label == label }
    }
  }
}