import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import type { ReviewVo } from '@aquarush/common'
import StarRating from '../../../components/StarRating'

import "taro-ui/dist/style/components/card.scss"
import './OrderReviewCard.scss'

interface OrderReviewCardProps {
  review: ReviewVo
}

/**
 * 我的评价卡(F-ORD-007):已完成且已评价订单展示星级/评论内容/评价时间(含匿名标识)
 */
const OrderReviewCard: React.FC<OrderReviewCardProps> = ({ review }) => {
  return (
    <AtCard
      title='我的评价'
      className='review-card shadow-default'
    >
      <View className='review-content'>
        <StarRating rating={review.rating} />
        {review.comment && (
          <Text className='review-comment'>{review.comment}</Text>
        )}
        <Text className='review-time'>
          {review.createdAt}
          {review.isAnonymous ? ' · 匿名评价' : ''}
        </Text>
      </View>
    </AtCard>
  )
}

export default OrderReviewCard
