import React from 'react'
import { View, Text } from '@tarojs/components'
import { displayCents } from '@aquarush/common'
import './CostSummary.scss'

interface CostSummaryProps {
  /** 商品金额(分,preview.subtotalCents) */
  subtotalCents: number
  /** 水票抵扣桶数(preview.ticketCoveredQuantity) */
  ticketCoveredQuantity: number
  /** 满减/折扣优惠(分,preview.promotionDiscountCents) */
  promotionDiscountCents: number
  /** 促销活动名称(preview.promotionActivityName) */
  promotionActivityName?: string | null
  /** 首单立减优惠(分,preview.firstOrderDiscountCents,与满减互斥取最大 —— R-MKT-10) */
  firstOrderDiscountCents: number
  /** 首单活动名称(preview.firstOrderActivityName) */
  firstOrderActivityName?: string | null
  /** 优惠券优惠(分,preview.couponDiscountCents) */
  couponDiscountCents: number
  /** 选中优惠券名称(preview.selectedCouponName) */
  selectedCouponName?: string | null
  /** 买赠/满赠赠品(preview.giftItems,附加 0 价行 —— R-MKT-09) */
  giftItems?: Array<{ productName: string; quantity: number }>
  /** 配送费(分,preview.deliveryFeeCents) */
  deliveryFeeCents: number
  /** 实付合计(分,preview.payAmountCents) */
  payAmountCents: number
}

/**
 * 费用明细(服务端计价预览 R-MON-01)
 * 商品金额 / 水票抵扣 / 满减或首单立减 / 优惠券 / 赠品 / 配送费 / 实付,各项来自 orderPreview 服务端计算结果。
 */
const CostSummary: React.FC<CostSummaryProps> = ({
  subtotalCents,
  ticketCoveredQuantity,
  promotionDiscountCents,
  promotionActivityName,
  firstOrderDiscountCents,
  firstOrderActivityName,
  couponDiscountCents,
  selectedCouponName,
  giftItems,
  deliveryFeeCents,
  payAmountCents
}) => {
  return (
    <View className='cost-section'>
      <View className='cost-item'>
        <Text className='cost-label'>商品金额</Text>
        <Text className='cost-value'>{displayCents(subtotalCents)}</Text>
      </View>
      {ticketCoveredQuantity > 0 && (
        <View className='cost-item'>
          <Text className='cost-label'>水票抵扣（{ticketCoveredQuantity}桶）</Text>
          <Text className='cost-value cost-discount'>-{ticketCoveredQuantity}桶</Text>
        </View>
      )}
      {promotionDiscountCents > 0 && (
        <View className='cost-item'>
          <Text className='cost-label'>满减{promotionActivityName ? `（${promotionActivityName}）` : ''}</Text>
          <Text className='cost-value cost-discount'>-{displayCents(promotionDiscountCents)}</Text>
        </View>
      )}
      {firstOrderDiscountCents > 0 && (
        <View className='cost-item'>
          <Text className='cost-label'>首单立减{firstOrderActivityName ? `（${firstOrderActivityName}）` : ''}</Text>
          <Text className='cost-value cost-discount'>-{displayCents(firstOrderDiscountCents)}</Text>
        </View>
      )}
      {couponDiscountCents > 0 && (
        <View className='cost-item'>
          <Text className='cost-label'>优惠券{selectedCouponName ? `（${selectedCouponName}）` : ''}</Text>
          <Text className='cost-value cost-discount'>-{displayCents(couponDiscountCents)}</Text>
        </View>
      )}
      {giftItems && giftItems.length > 0 && (
        <View className='cost-item'>
          <Text className='cost-label'>赠品</Text>
          <Text className='cost-value'>
            {giftItems.map((gift) => `${gift.productName} ×${gift.quantity}`).join('、')}（¥0.00）
          </Text>
        </View>
      )}
      <View className='cost-item'>
        <Text className='cost-label'>配送费</Text>
        <Text className='cost-value'>
          {deliveryFeeCents === 0 ? '免配送费' : displayCents(deliveryFeeCents)}
        </Text>
      </View>
      <View className='cost-item total'>
        <Text className='cost-label'>实付金额</Text>
        <Text className='cost-value total-amount'>{displayCents(payAmountCents)}</Text>
      </View>
    </View>
  )
}

export default CostSummary
