import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtButton } from 'taro-ui'

import "taro-ui/dist/style/components/button.scss"
import './index.scss'

/**
 * 空态占位 —— 收敛 BasePageLayout.EmptyState / AddressListEmptyState / OrderEmptyState /
 * cart / notice-detail 五处近重复实现(empty-container + 图/icon + 标题 + 描述 + 可选按钮)。
 * 视觉按 design system §4:居中、弱色图标/插图、主标题、副文案、主色 CTA。
 */
interface EmptyStateProps {
  /** 插图路径(PNG;与 icon 二选一,image 优先) */
  image?: string
  /** 图标(emoji/AtIcon;未传 image 时展示) */
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
    <View className='empty-container'>
      {image ? (
        <Image src={image} mode='aspectFit' className='empty-image' />
      ) : icon ? (
        <View className='empty-icon'>{icon}</View>
      ) : null}
      <Text className='empty-text'>{title}</Text>
      {description && <Text className='empty-desc'>{description}</Text>}
      {actionText && onAction && (
        <AtButton type='primary' size='normal' onClick={onAction} className='empty-action'>
          {actionText}
        </AtButton>
      )}
    </View>
  )
}

export default EmptyState
