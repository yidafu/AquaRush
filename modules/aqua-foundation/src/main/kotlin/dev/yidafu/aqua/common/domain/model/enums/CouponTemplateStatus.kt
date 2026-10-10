package dev.yidafu.aqua.common.domain.model.enums

/**
 * 优惠券模板状态:
 * - ACTIVE: 上架(可领取/发放/使用)
 * - DISABLED: 停用(不可再领取/发放,已领取券按快照继续有效)
 */
enum class CouponTemplateStatus {
  ACTIVE,
  DISABLED,
  ;

  companion object {
    fun fromValue(value: String): CouponTemplateStatus =
      when (value) {
        "ACTIVE" -> ACTIVE
        "DISABLED" -> DISABLED
        else -> throw IllegalArgumentException("Unknown CouponTemplateStatus value: '" + value + "'")
      }
  }
}
