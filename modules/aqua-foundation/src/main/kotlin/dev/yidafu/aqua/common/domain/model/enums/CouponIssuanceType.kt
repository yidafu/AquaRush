package dev.yidafu.aqua.common.domain.model.enums

/**
 * 优惠券发放方式(PRD 10-marketing.md §R-MKT-03):
 * - AUTO_CLAIM: 自动领取(用户端领券中心)
 * - TARGETED: 定向发放(管理员按用户/客户分组发放)
 */
enum class CouponIssuanceType {
  AUTO_CLAIM,
  TARGETED,
  ;

  companion object {
    fun fromValue(value: String): CouponIssuanceType =
      when (value) {
        "AUTO_CLAIM" -> AUTO_CLAIM
        "TARGETED" -> TARGETED
        else -> throw IllegalArgumentException("Unknown CouponIssuanceType value: '" + value + "'")
      }
  }
}
