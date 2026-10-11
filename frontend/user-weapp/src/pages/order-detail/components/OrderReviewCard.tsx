import React from 'react'
import { View, Text } from '@tarojs/components'
import type { ReviewVo } from '@aquarush/common'
import StarRating from '../../../components/StarRating'
import './OrderReviewCard.scss'

interface OrderReviewCardProps {
  review: ReviewVo
}

/**
 * 我的评价卡(F-ORD-007)—— WeUI .weui-cells 组(设计稿 02-order/order-review.html
 * 已评价回显:星级(品牌蓝)/标签/评论内容/评价时间,含匿名标识)
 */
const OrderReviewCard: React.FC<OrderReviewCardProps> = ({ review }) => {
  return (
    <View className='review-card'>
      <View className='weui-cells__title title-row'>
        <Text>我的评价</Text>
        {review.isAnonymous && <Text className='weui-tag weui-tag--outline'>匿名</Text>}
      </View>
      <View className='weui-cells'>
        <View className='weui-cell weui-cell_vertical'>
          <View className='review-head'>
            <StarRating rating={review.rating} />
            <Text className='review-time'>{review.createdAt}</Text>
          </View>
          {review.comment && (
            <Text className='review-comment'>{review.comment}</Text>
          )}
        </View>
      </View>
    </View>
  )
}

export default OrderReviewCard
