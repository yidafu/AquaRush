import React from 'react'
import { View, Text } from '@tarojs/components'
import {
  CouponOptionVo,
  UserCouponVo,
  Money,
  getCouponDiscountText,
  getCouponThresholdText,
  normalizeCents
} from '@aquarush/common'
import './CouponSelector.scss'

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
 */
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
    <View className='coupon-selector-section'>
      <View className='weui-cells__title'>优惠券</View>
      <View className='weui-cells'>
        {/* 不用券 */}
        <View className='weui-cell' onClick={() => onSelect(null)}>
          <View className='weui-cell__hd'>
            <View className={`weui-radio${selectedCouponId === null ? ' on' : ''}`} />
          </View>
          <View className='weui-cell__bd'>
            <Text className='coupon-name'>不使用优惠券</Text>
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
              className={`weui-cell${usable ? '' : ' coupon-option-disabled'}`}
              onClick={() => {
                if (usable) onSelect(coupon.id)
              }}
            >
              <View className='weui-cell__hd'>
                <View className={`weui-radio${selected ? ' on' : ''}`} />
              </View>
              <View className='weui-cell__bd'>
                <View className='coupon-name-row'>
                  <Text className='coupon-name'>{coupon.name}</Text>
                  {option?.isBest && <Text className='coupon-best'>最优</Text>}
                </View>
                <Text className='coupon-desc'>
                  {getCouponDiscountText(coupon)} · {getCouponThresholdText(coupon.thresholdCents)}
                </Text>
                {!usable && (
                  <Text className='coupon-hint'>门槛不足/不可用</Text>
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
