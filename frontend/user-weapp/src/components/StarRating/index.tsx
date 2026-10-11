import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'

interface StarRatingProps {
  rating: number
  /** 只读展示(默认);false 时允许点击选择 */
  readonly?: boolean
  onChange?: (rating: number) => void
}

/**
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 平台变量保留字符串引用)。
 * 原 `.star.active` / `.stars.picker .star` 拍平为条件 style(选中品牌蓝 / 点选态 60 号)。
 */
const styles = stylex.create({
  stars: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '16rpx',
  },
  star: {
    fontSize: '40rpx',
    lineHeight: 1,
    color: 'var(--weui-FG-3)',
    transitionProperty: 'color',
    transitionDuration: '0.2s',
  },
  starActive: {
    color: 'var(--weui-BRAND)',
  },
  starPicker: {
    fontSize: '60rpx',
  },
})

/**
 * 星级评分展示/选择 —— 收敛 order-review 页(两处)与订单详情 OrderReviewCard 的重复星标行。
 */
const StarRating: React.FC<StarRatingProps> = ({ rating, readonly = true, onChange }) => {
  return (
    <View {...stylex.props(styles.stars)}>
      {[1, 2, 3, 4, 5].map(star => (
        <Text
          key={star}
          {...stylex.props(
            styles.star,
            !readonly && styles.starPicker,
            star <= rating && styles.starActive
          )}
          onClick={readonly ? undefined : () => onChange?.(star)}
        >
          ★
        </Text>
      ))}
    </View>
  )
}

export default StarRating
