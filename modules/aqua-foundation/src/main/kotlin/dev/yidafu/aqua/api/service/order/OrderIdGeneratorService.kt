package dev.yidafu.aqua.api.service.order

interface OrderIdGeneratorService {
  /**
   * 生成16位订单号: YYMMDD + 10位序列号
   * 序列号使用专用MapDB持久化，重启后不重复
   */
  fun generateOrderId(): String
}
