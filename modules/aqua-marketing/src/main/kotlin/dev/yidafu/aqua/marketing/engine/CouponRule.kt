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

package dev.yidafu.aqua.marketing.engine

import dev.yidafu.aqua.api.dto.CouponOption
import dev.yidafu.aqua.api.service.marketing.CouponQueryApiService
import dev.yidafu.aqua.common.domain.model.enums.CouponScope
import dev.yidafu.aqua.common.domain.model.enums.CouponType
import dev.yidafu.aqua.common.domain.model.enums.UserCouponStatus
import dev.yidafu.aqua.common.exception.BadRequestException
import dev.yidafu.aqua.marketing.coupon.domain.repository.UserCouponRepository
import org.springframework.stereotype.Component

/**
 * 优惠券规则(R-MKT-03/07,叠加③):
 * 一单一券;券门槛按现金部分商品小计(未含优惠)判定,不受满减影响;
 * 折扣作用于满减后余额并封顶(优惠后应付 ≥ 0);
 * 「最优优惠组合」:遍历全部可用券计算各自优惠金额,默认勾选最大者,用户可改选其他可用券或取消(0=不用券);
 * 下单消费时原子核销(RECEIVED→USED),受影响行数=0 → 回滚(防并发重复使用,核销幂等)。
 */
@Component
class CouponRule(
  private val couponQueryApiService: CouponQueryApiService,
  private val userCouponRepository: UserCouponRepository,
) : PricingRule {
  override val order: Int = 400

  override fun evaluate(
    ctx: PricingContext,
    state: PricingState,
  ) {
    val usableCoupons = couponQueryApiService.findUsableCoupons(ctx.userId)
    val cashSubtotal = state.cashSubtotalCents
    // 折扣作用于满减后余额(R-MKT-07 顺序),封顶不超余额
    val balanceAfterPromotion = maxOf(0, cashSubtotal - state.promotionDiscountCents)

    var options =
      usableCoupons.map { coupon ->
        // 门槛按现金部分商品小计(未含优惠)判定;指定商品券按商品匹配(R-MKT-03/07)
        val thresholdOk = coupon.thresholdCents?.let { cashSubtotal >= it } ?: true
        val scopeOk =
          when (coupon.scope) {
            CouponScope.ALL -> true
            CouponScope.PRODUCT -> coupon.productId != null && coupon.productId in ctx.productIds
          }
        val eligible = thresholdOk && scopeOk
        val discount =
          if (eligible) {
            when (coupon.couponType) {
              CouponType.CASH, CouponType.NO_THRESHOLD_CASH ->
                minOf(coupon.discountAmountCents ?: 0, balanceAfterPromotion)
              CouponType.DISCOUNT -> {
                val rate = coupon.discountRatePermille ?: 10000
                minOf(balanceAfterPromotion * (10000 - rate) / 10000, balanceAfterPromotion)
              }
            }
          } else {
            0
          }
        CouponOption(
          couponId = coupon.id!!,
          name = coupon.name,
          discountCents = discount,
          usable = eligible,
          isBest = false,
        )
      }

    // 最优券(优惠金额最大者,标记 isBest 供默认勾选)
    val best = options.filter { it.usable }.maxByOrNull { it.discountCents }
    if (best != null) {
      options = markBest(options, best.couponId)
    }

    // 选择:用户偏好(null=自动最优,0=不用券);偏好券不可用时按不可用处理(前端置灰限制,后端兜底)
    val selectedId =
      when (ctx.preferredCouponId) {
        null -> best?.couponId
        0L -> null
        else -> {
          val preferred = options.firstOrNull { it.couponId == ctx.preferredCouponId }
          if (preferred != null && preferred.usable) preferred.couponId else null
        }
      }

    val selected = options.firstOrNull { it.couponId == selectedId }
    state.couponOptions = options
    state.selectedCouponId = selected?.couponId
    state.selectedCouponName = selected?.name
    state.couponDiscountCents = selected?.discountCents ?: 0

    // 下单消费:原子核销
    val orderId = ctx.orderId
    if (orderId != null && selected != null) {
      val affected =
        userCouponRepository.redeemAtomically(
          id = selected.couponId,
          userId = ctx.userId,
          orderId = orderId,
          from = UserCouponStatus.RECEIVED,
          to = UserCouponStatus.USED,
          now = ctx.now,
        )
      if (affected == 0) {
        // 已被并发核销/过期:整体回滚
        throw BadRequestException("优惠券已被使用或已过期,请刷新后重试")
      }
    }
  }

  /**
   * 标记最优券(data class equals 含全部字段,按 couponId 定位替换)
   */
  private fun markBest(
    options: List<CouponOption>,
    bestCouponId: Long,
  ): List<CouponOption> {
    val index = options.indexOfFirst { it.couponId == bestCouponId }
    if (index == -1) return options
    return options.toMutableList().also { it[index] = it[index].copy(isBest = true) }
  }
}
