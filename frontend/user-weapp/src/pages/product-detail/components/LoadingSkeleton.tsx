import React from 'react'
import { View } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'

// 原 tailwind 工具类(web 375 基准)→ StyleX,长度 ×2 转 rpx(750 基准)。
// gray-50/100/200、渐变与 shimmer 均为 tailwind 默认值,未入共享 token。
const shimmer = stylex.keyframes({
  '0%': { backgroundPosition: '-2000rpx 0' },
  '100%': { backgroundPosition: '2000rpx 0' },
});

const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: '#F9FAFB',
    paddingBottom: '320rpx',
    '@media (min-width: 768px)': {
      maxWidth: '896rpx',
      marginLeft: 'auto',
      marginRight: 'auto',
      boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1)',
      borderRadius: '48rpx',
      overflow: 'hidden',
    },
  },
  galleryArea: {
    backgroundColor: tokens.card,
    paddingLeft: '64rpx',
    paddingRight: '64rpx',
    paddingTop: '80rpx',
    paddingBottom: '48rpx',
    '@media (min-width: 768px)': {
      paddingLeft: '80rpx',
      paddingRight: '80rpx',
      paddingTop: '96rpx',
      paddingBottom: '64rpx',
    },
  },
  shimmer: {
    animationName: shimmer,
    animationDuration: '2s',
    animationIterationCount: 'infinite',
    backgroundImage: 'linear-gradient(to right, #E5E7EB, #F3F4F6, #E5E7EB)',
    backgroundSize: '2000rpx 100%',
  },
  galleryPlaceholder: {
    width: '100%',
    height: '840rpx',
    borderRadius: '32rpx',
    '@media (min-width: 768px)': {
      height: '920rpx',
      borderRadius: '48rpx',
    },
  },
  infoArea: {
    backgroundColor: tokens.card,
    paddingTop: '128rpx',
    paddingBottom: '128rpx',
    paddingLeft: '64rpx',
    paddingRight: '64rpx',
  },
  titleBlock: {
    marginBottom: '64rpx',
  },
  titleLine1: {
    marginBottom: '32rpx',
    height: '64rpx',
    borderRadius: '16rpx',
    width: '80%',
  },
  titleLine2: {
    height: '40rpx',
    borderRadius: '16rpx',
    width: '60%',
  },
  metaBlock: {
    paddingTop: '48rpx',
    paddingBottom: '48rpx',
    marginBottom: '64rpx',
    borderBottomWidth: '1rpx',
    borderBottomStyle: 'solid',
    borderBottomColor: '#F3F4F6',
  },
  metaLine: {
    height: '80rpx',
    borderRadius: '16rpx',
    width: '50%',
  },
  cardsBlock: {
    marginBottom: '64rpx',
  },
  cardsRow: {
    display: 'flex',
    justifyContent: 'space-between',
    columnGap: '32rpx',
    marginBottom: '32rpx',
  },
  cardsRowLast: {
    marginBottom: 0,
  },
  card: {
    flexGrow: 1,
    height: '128rpx',
    borderRadius: '32rpx',
  },
  footerRow: {
    display: 'flex',
    justifyContent: 'center',
    paddingTop: '40rpx',
    paddingBottom: '40rpx',
    borderTopWidth: '1rpx',
    borderTopStyle: 'solid',
    borderTopColor: '#F3F4F6',
    columnGap: '120rpx',
  },
  footerLine: {
    height: '40rpx',
    borderRadius: '16rpx',
    width: '96rpx',
  },
})

const LoadingSkeleton: React.FC = () => {
  return (
    <View {...stylex.props(styles.page)}>
      {/* 骨架屏图片区域 */}
      <View {...stylex.props(styles.galleryArea)}>
        <View {...stylex.props(styles.shimmer, styles.galleryPlaceholder)}></View>
      </View>

      {/* 骨架屏产品信息 */}
      <View {...stylex.props(styles.infoArea)}>
        <View {...stylex.props(styles.titleBlock)}>
          <View {...stylex.props(styles.shimmer, styles.titleLine1)}></View>
          <View {...stylex.props(styles.shimmer, styles.titleLine2)}></View>
        </View>

        <View {...stylex.props(styles.metaBlock)}>
          <View {...stylex.props(styles.shimmer, styles.metaLine)}></View>
        </View>

        <View {...stylex.props(styles.cardsBlock)}>
          <View {...stylex.props(styles.cardsRow)}>
            <View {...stylex.props(styles.shimmer, styles.card)}></View>
            <View {...stylex.props(styles.shimmer, styles.card)}></View>
          </View>
          <View {...stylex.props(styles.cardsRow, styles.cardsRowLast)}>
            <View {...stylex.props(styles.shimmer, styles.card)}></View>
            <View {...stylex.props(styles.shimmer, styles.card)}></View>
          </View>
        </View>

        <View {...stylex.props(styles.footerRow)}>
          <View {...stylex.props(styles.shimmer, styles.footerLine)}></View>
          <View {...stylex.props(styles.shimmer, styles.footerLine)}></View>
        </View>
      </View>
    </View>
  )
}

export default LoadingSkeleton
