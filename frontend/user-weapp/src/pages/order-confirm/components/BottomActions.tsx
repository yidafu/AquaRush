import React from 'react'
import { View, Text, Button } from '@tarojs/components'
import { displayCents } from '@aquarush/common'
import './BottomActions.scss'

interface BottomActionsProps {
  totalAmount: number
  loading: boolean
  /** 计价失败/未就绪时禁用提交 */
  disabled?: boolean
  /** 总优惠金额(分,preview.totalDiscountCents);>0 时展示「已优惠 ¥x」 */
  discountCents?: number
  onSubmit: () => void
}

const BottomActions: React.FC<BottomActionsProps> = ({
  totalAmount,
  loading,
  disabled = false,
  discountCents = 0,
  onSubmit
}) => {
  return (
    <View className='bottom-actions'>
      <View className='total-info'>
        <View className='total-row'>
          <Text className='total-label'>合计：</Text>
          <Text className='total-amount'>{displayCents(totalAmount)}</Text>
        </View>
        {discountCents > 0 && (
          <Text className='total-discount'>已优惠 {displayCents(discountCents)}</Text>
        )}
      </View>
      <Button
        trackTarget="order-submit"
        trackAction="FORM_SUBMIT"
        trackElementType="button"
        trackElementText="提交订单"
        trackDisabled={disabled || loading}
        onClick={onSubmit}
        className='submit-button'
        loading={loading}
        disabled={disabled || loading}
      >
        {loading ? '提交中...' : '提交订单'}
      </Button>
    </View>
  )
}

export default BottomActions
