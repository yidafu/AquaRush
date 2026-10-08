import React from 'react'
import { View, Text } from '@tarojs/components'

import './index.scss'

/**
 * 星级评分展示/选择 —— 收敛 order-review 页(两处)与订单详情 OrderReviewCard 的重复星标行。
 */
interface StarRatingProps {
  rating: number
  /** 只读展示(默认);false 时允许点击选择 */
  readonly?: boolean
  onChange?: (rating: number) => void
}

const StarRating: React.FC<StarRatingProps> = ({ rating, readonly = true, onChange }) => {
  return (
    <View className={`stars${readonly ? '' : ' picker'}`}>
      {[1, 2, 3, 4, 5].map(star => (
        <Text
          key={star}
          className={`star ${star <= rating ? 'active' : ''}`}
          onClick={readonly ? undefined : () => onChange?.(star)}
        >
          ★
        </Text>
      ))}
    </View>
  )
}

export default StarRating
