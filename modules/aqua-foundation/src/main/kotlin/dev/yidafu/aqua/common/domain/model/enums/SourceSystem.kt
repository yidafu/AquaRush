package dev.yidafu.aqua.common.domain.model.enums

/**
 * 源系统类型
 */
enum class SourceSystem {
  INTERNAL, // 内部系统
  WECHAT, // 微信支付
  ;

  companion object {
    fun fromValue(value: String): SourceSystem =
      when (value) {
        "INTERNAL" -> INTERNAL
        "WECHAT" -> WECHAT
        else -> throw IllegalArgumentException("Unknown SourceSystem value: '" + value + "'")
      }
  }
}
