import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import {
  CouponOptionVo,
  UserCouponVo,
  Money,
  getCouponDiscountText,
  getCouponThresholdText,
  normalizeCents
} from '@aquarush/common'

interface CouponSelectorProps {
  /** 可用券列表(含不可用置灰项,preview.availableCoupons) */
  coupons: ReadonlyArray<UserCouponVo>
  /** 各券在本订单上下文下的优惠金额(preview.couponOptions) */
  options: ReadonlyArray<CouponOptionVo>
  /** 当前选中的券 ID(null = 不用券) */
  selectedCouponId: string | null
  /** 选择变化(不用券传入 null;触发父级重新计价) */
  onSelect: (couponId: string | null) => void
}

/**
 * 优惠券选择(F-MKT-003,F-ORD-023)—— WeUI .weui-cells 单选行(.weui-radio 圆点)
 * (设计稿 02-order/order-confirm.html 优惠券半屏的行结构,本页保持内联列表交互不变):
 * 最优券默认勾选由父级(引擎 isBest)决定,用户可改选或取消(不用券);
 * 门槛不足/不可用的券置灰不可选(.weui-price--gray),并展示提示文案;无可用券时整个区块不渲染。
 *
 * 样式:StyleX(原 CouponSelector.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原后代选择器拍平到子元素,置灰态用条件类挂在 name / desc 上;
 * 原 var(--action) 改 tokens.action)
 */
const styles = stylex.create({
  couponSelectorSection: {
    marginTop: '8rpx',
  },
  couponNameRow: {
    display: 'flex',
    alignItems: 'center',
    rowGap: '12rpx',
    columnGap: '12rpx',
  },
  couponName: {
    fontSize: 'var(--weui-fs-cell)',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
  },
  couponBest: {
    fontSize: '24rpx',
    color: tokens.action,
  },
  couponDesc: {
    display: 'block',
    marginTop: '8rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
  },
  couponHint: {
    display: 'block',
    marginTop: '8rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
  },
  // 不可用置灰(原 .coupon-option-disabled 后代覆盖拍平)
  couponDisabledText: {
    color: 'var(--weui-FG-2)',
  },
})
const CouponSelector: React.FC<CouponSelectorProps> = ({
  coupons,
  options,
  selectedCouponId,
  onSelect
}) => {
  // 无可用券时区块整体隐藏(无可选即无展示价值,C5)
  if (coupons.length === 0) {
    return null
  }

  const optionMap = new Map<string, CouponOptionVo>()
  options.forEach(option => {
    optionMap.set(option.couponId, option)
  })

  return (
    <View {...stylex.props(styles.couponSelectorSection)}>
      <View className='weui-cells__title'>优惠券</View>
      <View className='weui-cells'>
        {/* 不用券 */}
        <View className='weui-cell' onClick={() => onSelect(null)}>
          <View className='weui-cell__hd'>
            <View className={`weui-radio${selectedCouponId === null ? ' on' : ''}`} />
          </View>
          <View className='weui-cell__bd'>
            <Text {...stylex.props(styles.couponName)}>不使用优惠券</Text>
          </View>
        </View>

        {coupons.map(coupon => {
          const option = optionMap.get(coupon.id)
          const usable = option?.usable ?? false
          const discountCents = option ? normalizeCents(option.discountCents) : null
          const selected = selectedCouponId === coupon.id

          return (
            <View
              key={coupon.id}
              className='weui-cell'
              onClick={() => {
                if (usable) onSelect(coupon.id)
              }}
            >
              <View className='weui-cell__hd'>
                <View className={`weui-radio${selected ? ' on' : ''}`} />
              </View>
              <View className='weui-cell__bd'>
                <View {...stylex.props(styles.couponNameRow)}>
                  <Text {...props(styles.couponName, !usable && styles.couponDisabledText)}>{coupon.name}</Text>
                  {option?.isBest && <Text {...stylex.props(styles.couponBest)}>最优</Text>}
                </View>
                <Text {...props(styles.couponDesc, !usable && styles.couponDisabledText)}>
                  {getCouponDiscountText(coupon)} · {getCouponThresholdText(coupon.thresholdCents)}
                </Text>
                {!usable && (
                  <Text {...stylex.props(styles.couponHint)}>门槛不足/不可用</Text>
                )}
              </View>
              {discountCents != null && (
                <View className='weui-cell__ft'>
                  <Text className={`weui-price weui-price--md${usable ? '' : ' weui-price--gray'}`}>
                    -<Money cents={discountCents} showSymbol={false} />
                  </Text>
                </View>
              )}
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default CouponSelector
