import React from 'react'
import { View, Text } from '@tarojs/components'
import {
  CouponOptionVo,
  UserCouponVo,
  displayCents,
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
 * 优惠券选择(F-MKT-003,F-ORD-023)
 * 单选式选择;最优券默认勾选由父级(引擎 isBest)决定,用户可改选或取消(不用券);
 * 门槛不足/不可用的券置灰不可选,并展示提示文案;无可用券时整个区块不渲染。
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
    <View className='section coupon-selector-section'>
      <View className='section-label-row'>
        <Text className='section-label'>优惠券</Text>
      </View>
      <View className='coupon-options'>
        {/* 不用券 */}
        <View
          className={`coupon-option ${selectedCouponId === null ? 'coupon-option-selected' : ''}`}
          onClick={() => onSelect(null)}
        >
          <View className='coupon-option-info'>
            <Text className='coupon-option-name'>不用券</Text>
            <Text className='coupon-option-hint'>不使用优惠券</Text>
          </View>
          <View className='coupon-option-radio'>
            {selectedCouponId === null && <View className='coupon-option-radio-dot' />}
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
              className={`coupon-option ${selected ? 'coupon-option-selected' : ''} ${usable ? '' : 'coupon-option-disabled'}`}
              onClick={() => {
                if (usable) onSelect(coupon.id)
              }}
            >
              <View className='coupon-option-info'>
                <View className='coupon-option-name-row'>
                  <Text className='coupon-option-name'>{coupon.name}</Text>
                  {option?.isBest && <Text className='coupon-option-best'>最优</Text>}
                </View>
                <Text className='coupon-option-desc'>
                  {getCouponDiscountText(coupon)} · {getCouponThresholdText(coupon.thresholdCents)}
                </Text>
                {!usable && (
                  <Text className='coupon-option-hint'>门槛不足/不可用</Text>
                )}
              </View>
              <View className='coupon-option-right'>
                {discountCents != null && (
                  <Text className='coupon-option-discount'>-{displayCents(discountCents)}</Text>
                )}
                <View className='coupon-option-radio'>
                  {selected && <View className='coupon-option-radio-dot' />}
                </View>
              </View>
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default CouponSelector
