/**
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

package dev.yidafu.aqua.analytics.statistics.dto

/**
 * 营销方式使用明细类型(PRD 08-statistics.md §F-STA-007 营销方式使用明细表):
 * - COUPON_TEMPLATE:优惠券模板(ref_id = coupon_template_id)
 * - PROMOTION_ACTIVITY:满减/折扣活动
 * - FIRST_ORDER_ACTIVITY:首单立减/新人专享价活动(仅首单立减计入明细,新人价见开放问题)
 * - GIFT_ACTIVITY:买赠/满赠活动
 * - TICKET_TYPE:桶水票票种
 */
enum class MarketingBreakdownKind {
  COUPON_TEMPLATE,
  PROMOTION_ACTIVITY,
  FIRST_ORDER_ACTIVITY,
  GIFT_ACTIVITY,
  TICKET_TYPE,
  ;

  companion object {
    fun fromValue(value: String): MarketingBreakdownKind =
      when (value) {
        "COUPON_TEMPLATE" -> COUPON_TEMPLATE
        "PROMOTION_ACTIVITY" -> PROMOTION_ACTIVITY
        "FIRST_ORDER_ACTIVITY" -> FIRST_ORDER_ACTIVITY
        "GIFT_ACTIVITY" -> GIFT_ACTIVITY
        "TICKET_TYPE" -> TICKET_TYPE
        else -> throw IllegalArgumentException("Unknown MarketingBreakdownKind value: '" + value + "'")
      }
  }
}
