import React from 'react'
import { View, Text, Button } from '@tarojs/components'
import { displayCents } from '@aquarush/common'
import './BottomActions.scss'

interface BottomActionsProps {
  totalAmount: number
  loading: boolean
  /** 计价失败/未就绪时禁用提交 */
  disabled?: boolean
  onSubmit: () => void
}

const BottomActions: React.FC<BottomActionsProps> = ({
  totalAmount,
  loading,
  disabled = false,
  onSubmit
}) => {
  return (
    <View className='bottom-actions'>
      <View className='total-info'>
        <Text className='total-label'>合计：</Text>
        <Text className='total-amount'>{displayCents(totalAmount)}</Text>
      </View>
      <Button
        className='submit-button'
        loading={loading}
        disabled={disabled || loading}
        onClick={onSubmit}
      >
        {loading ? '提交中...' : '提交订单'}
      </Button>
    </View>
  )
}

export default BottomActions
