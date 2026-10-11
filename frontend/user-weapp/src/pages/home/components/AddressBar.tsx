import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import CustomIcon from '@/components/CustomIcon'

interface AddressBarProps {
  addressText: string
  onAddressClick: () => void
}

/**
 * 渐变 hero 内定位行(home.html .hero-loc):
 * 图钉图标 + 收货地址 + 「切换」;点击进地址列表选择。
 * 样式:StyleX(原 AddressBar.scss 迁移;750 基准 rpx,白字用 onBrand token)
 */
const styles = stylex.create({
  bar: {
    position: 'relative',
    zIndex: 1,
    display: 'flex',
    alignItems: 'center',
    rowGap: '16rpx',
    columnGap: '16rpx',
    paddingTop: '8rpx',
    paddingBottom: '8rpx',
  },
  pin: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: '0%',
    minWidth: '0',
    fontSize: '28rpx',
    color: tokens.onBrand,
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  more: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    marginLeft: 'auto',
    fontSize: '24rpx',
    fontWeight: 400,
    color: tokens.onBrand,
    opacity: 0.85,
  },
})

const AddressBar: React.FC<AddressBarProps> = ({ addressText, onAddressClick }) => {
  return (
    <View {...stylex.props(styles.bar)} onClick={onAddressClick}>
      <View {...stylex.props(styles.pin)}>
        <CustomIcon value='/assets/icons/service/map-pin' size={16} />
      </View>
      <Text {...stylex.props(styles.text)} numberOfLines={1}>{addressText}</Text>
      <Text {...stylex.props(styles.more)}>切换 ▾</Text>
    </View>
  )
}

export default AddressBar
