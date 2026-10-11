import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import CustomIcon from '../CustomIcon'

/**
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 平台变量保留字符串引用;
 * .weui-empty 体系类名仍由 weui.scss 提供,empty-* 类名已内联为 style)
 */
const styles = stylex.create({
  emptyImage: {
    width: '120rpx',
    height: '120rpx',
    borderRadius: '50%',
  },
  emptyTitle: {
    display: 'block',
    color: 'var(--weui-FG-0)',
    fontSize: 'var(--weui-fs-cell)',
  },
  emptyDesc: {
    display: 'block',
    marginTop: '8rpx',
    color: 'var(--weui-FG-1)',
    fontSize: 'var(--weui-fs-tips)',
  },
})

/**
 * 空态占位 —— WeUI 2.x 视觉(.weui-empty:品牌 tint 圆 + 图标/插图 + 文案 + 可选主色按钮,
 * 设计稿 02-order/order-list.html 空态口径)。
 * 收敛 BasePageLayout.EmptyState / AddressListEmptyState / OrderEmptyState /
 * cart / notice-detail 五处近重复实现;props 签名保持兼容(image/icon/title/description/action)。
 */
interface EmptyStateProps {
  /** 插图路径(PNG;与 icon 二选一,image 优先) */
  image?: string
  /** 自定义图标节点(未传 image 时展示;缺省用 AtIcon 订单图标) */
  icon?: React.ReactNode
  title: string
  description?: string
  /** 操作按钮文案(传了才展示按钮) */
  actionText?: string
  onAction?: () => void
}

const EmptyState: React.FC<EmptyStateProps> = ({
  image,
  icon,
  title,
  description,
  actionText,
  onAction
}) => {
  return (
    <View className='weui-empty'>
      <View className='weui-empty__icon'>
        {image ? (
          <Image src={image} mode='aspectFit' {...stylex.props(styles.emptyImage)} />
        ) : icon ? (
          icon
        ) : (
          <CustomIcon value='bookmark' size={64} color='var(--weui-BRAND)' />
        )}
      </View>
      <View className='weui-empty__text'>
        <Text {...stylex.props(styles.emptyTitle)}>{title}</Text>
        {description && <Text {...stylex.props(styles.emptyDesc)}>{description}</Text>}
      </View>
      {actionText && onAction && (
        <View
          className='weui-btn weui-btn_plain-primary weui-btn_mini'
          onClick={onAction}
        >
          {actionText}
        </View>
      )}
    </View>
  )
}

export default EmptyState
