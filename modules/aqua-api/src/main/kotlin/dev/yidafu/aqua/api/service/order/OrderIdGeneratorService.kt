package dev.yidafu.aqua.api.service.order

interface OrderIdGeneratorService {
  /**
   * YYYYMMDD+用户后10位+6位全局的序列号
   */
  fun generateOrderId(userId: Long): String
}
