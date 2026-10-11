import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import type { ReviewVo } from '@aquarush/common'
import StarRating from '../../../components/StarRating'

interface OrderReviewCardProps {
  review: ReviewVo
}

// 我的评价卡:WeUI cells 骨架由 weui.scss 提供,星级走共享 StarRating(品牌蓝)
const styles = stylex.create({
  titleRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  reviewHead: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    columnGap: '24rpx',
  },
  reviewTime: {
    flexGrow: 0,
    flexShrink: 0,
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
    fontVariantNumeric: 'tabular-nums',
  },
  reviewComment: {
    display: 'block',
    marginTop: '24rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-0)',
    lineHeight: 1.6,
  },
})

/**
 * 我的评价卡(F-ORD-007)—— WeUI .weui-cells 组(设计稿 02-order/order-review.html
 * 已评价回显:星级(品牌蓝)/标签/评论内容/评价时间,含匿名标识)
 */
const OrderReviewCard: React.FC<OrderReviewCardProps> = ({ review }) => {
  return (
    <View>
      <View {...props('weui-cells__title', styles.titleRow)}>
        <Text>我的评价</Text>
        {review.isAnonymous && <Text className='weui-tag weui-tag--outline'>匿名</Text>}
      </View>
      <View className='weui-cells'>
        <View className='weui-cell weui-cell_vertical'>
          <View {...stylex.props(styles.reviewHead)}>
            <StarRating rating={review.rating} />
            <Text {...stylex.props(styles.reviewTime)}>{review.createdAt}</Text>
          </View>
          {review.comment && (
            <Text {...stylex.props(styles.reviewComment)}>{review.comment}</Text>
          )}
        </View>
      </View>
    </View>
  )
}

export default OrderReviewCard
