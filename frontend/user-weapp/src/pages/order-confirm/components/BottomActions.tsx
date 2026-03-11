import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton } from 'taro-ui'
import './BottomActions.scss'

interface BottomActionsProps {
  totalAmount: number
  loading: boolean
  onSubmit: () => void
}

const BottomActions: React.FC<BottomActionsProps> = ({
  totalAmount,
  loading,
  onSubmit
}) => {
  return (
    <View className='bottom-actions'>
      <View className='total-info'>
        <Text className='total-label'>合计：</Text>
        <Text className='total-amount'>¥{totalAmount.toFixed(2)}</Text>
      </View>
      <AtButton
        type='primary'
        size='normal'
        loading={loading}
        disabled={loading}
        onClick={onSubmit}
        className='submit-button'
      >
        {loading ? '提交中...' : '提交订单'}
      </AtButton>
    </View>
  )
}

export default BottomActions