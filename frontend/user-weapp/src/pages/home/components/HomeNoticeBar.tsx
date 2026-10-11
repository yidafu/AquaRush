import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'

interface HomeNoticeBarProps {
  title: string
  onClick: () => void
}

/**
 * 通告栏(WeUI .weui-notice 橙 tint,仅生效公告时显示,F-OPS-002)
 * 样式:StyleX(原 HomeNoticeBar.scss 迁移;750 基准 rpx,后代选择器拍平)
 */
const styles = stylex.create({
  notice: {
    marginTop: '16rpx',
  },
  label: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    fontWeight: 600,
  },
  title: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: '0%',
    minWidth: '0',
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
})

const HomeNoticeBar: React.FC<HomeNoticeBarProps> = ({ title, onClick }) => {
  return (
    <View {...props('weui-notice', styles.notice)} onClick={onClick}>
      <Text {...stylex.props(styles.label)}>公告</Text>
      <Text {...stylex.props(styles.title)} numberOfLines={1}>{title}</Text>
    </View>
  )
}

export default HomeNoticeBar
