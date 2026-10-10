package dev.yidafu.aqua.common.domain.model.enums

/**
 * 水桶费价目状态(G06:一次性塑料水桶费——独立价目表,与商品解耦):
 * ENABLED(启用,用户端可购)→ DISABLED(停用,停用不影响已生成购桶单)
 */
enum class BarrelFeeStatus {
  ENABLED,
  DISABLED,
  ;

  companion object {
    fun fromValue(value: String): BarrelFeeStatus =
      when (value) {
        "ENABLED" -> ENABLED
        "DISABLED" -> DISABLED
        else -> throw IllegalArgumentException("Unknown BarrelFeeStatus value: '" + value + "'")
      }
  }
}
