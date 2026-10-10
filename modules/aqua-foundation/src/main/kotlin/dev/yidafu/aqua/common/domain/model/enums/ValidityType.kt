package dev.yidafu.aqua.common.domain.model.enums

/**
 * 有效期类型(PRD 10-marketing.md):
 * - FIXED_RANGE: 固定区间(validFrom/validTo)
 * - DAYS_AFTER_CLAIM: 领取后 N 天(优惠券,validDays)
 * - DAYS_AFTER_PURCHASE: 购票后 N 天(桶水票,validDays)
 */
enum class ValidityType {
  FIXED_RANGE,
  DAYS_AFTER_CLAIM,
  DAYS_AFTER_PURCHASE,
  ;

  companion object {
    fun fromValue(value: String): ValidityType =
      when (value) {
        "FIXED_RANGE" -> FIXED_RANGE
        "DAYS_AFTER_CLAIM" -> DAYS_AFTER_CLAIM
        "DAYS_AFTER_PURCHASE" -> DAYS_AFTER_PURCHASE
        else -> throw IllegalArgumentException("Unknown ValidityType value: '" + value + "'")
      }
  }
}
