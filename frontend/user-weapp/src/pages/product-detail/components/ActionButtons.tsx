import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton } from 'taro-ui'
import "taro-ui/dist/style/components/button.scss"
import * as stylex from '@stylexjs/stylex'
import { COLORS } from '@aquarush/common/theme'
import { tokens } from '@aquarush/common/theme/tokens.stylex'

// 原 tailwind 工具类(web 375 基准)→ StyleX,长度 ×2 转 rpx(750 基准)。
// AtButton customStyle 里的旧 theme.scss 变量(--brand-primary 等)随 theme.scss
// 移除,改用 COLORS 真实值(守卫测试与 tokens 一致)。
// gray-50 #F9FAFB / gray-200 #E5E7EB / red-500 #EF4444 / red-400 #F87171 /
// shadow-lg 为 tailwind 默认色板值,未入共享 token。
const slideInLeft = stylex.keyframes({
  '0%': { opacity: '0', transform: 'translateX(-40rpx)' },
  '100%': { opacity: '1', transform: 'translateX(0)' },
});

const styles = stylex.create({
  bar: {
    position: 'fixed',
    bottom: 0,
    left: 0,
    right: 0,
    zIndex: 50,
    paddingTop: '16rpx',
    paddingBottom: '16rpx',
    paddingLeft: '16rpx',
    paddingRight: '16rpx',
    backgroundColor: tokens.card,
    borderTopWidth: '1rpx',
    borderTopStyle: 'solid',
    borderTopColor: '#E5E7EB',
    boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)',
    animationName: slideInLeft,
    animationDuration: '0.6s',
    animationTimingFunction: 'ease-out',
    animationDelay: '0.8s',
    animationFillMode: 'both',
  },
  row: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '16rpx',
  },
  favWrap: {
    flexShrink: 0,
  },
  favBtn: {
    width: '80rpx',
    height: '80rpx',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: '50%',
    backgroundColor: '#F9FAFB',
    transitionProperty: 'all',
    transitionDuration: '0.3s',
    cursor: 'pointer',
  },
  favBtnActive: {
    backgroundImage: 'linear-gradient(to bottom right, #EF4444, #F87171)',
    transform: 'scale(1.05)',
    boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1)',
  },
  favIcon: {
    fontSize: '40rpx',
    transitionProperty: 'color',
    transitionDuration: '0.3s',
    color: tokens.textWeak,
  },
  favIconActive: {
    fontSize: '40rpx',
    transitionProperty: 'color',
    transitionDuration: '0.3s',
    color: tokens.onBrand,
  },
  serviceBtn: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    width: '80rpx',
    height: '80rpx',
    transitionProperty: 'all',
    transitionDuration: '0.3s',
    borderRadius: '8rpx',
    cursor: 'pointer',
    backgroundColor: '#F9FAFB',
    ':active': {
      backgroundColor: '#E5E7EB',
      transform: 'scale(0.95)',
    },
  },
  serviceIcon: {
    marginBottom: '8rpx',
    fontSize: '32rpx',
  },
  mainWrap: {
    display: 'flex',
    flexGrow: 1,
    alignItems: 'center',
    columnGap: '16rpx',
  },
  abtn: {
    height: '80rpx',
    flexGrow: 1,
  },
})

interface ActionButtonsProps {
  isFavorite: boolean
  onToggleFavorite: () => void
  onAddToCart: () => void
  onBuyNow: () => void
  onContactService: () => void
  onShare: () => void
  stock: number
  status: string
}

const ActionButtons: React.FC<ActionButtonsProps> = ({
  isFavorite,
  onToggleFavorite,
  onAddToCart,
  onBuyNow,
  onContactService,
  onShare,
  stock,
  status
}) => {
  return (
    <View {...stylex.props(styles.bar)}>
      <View {...stylex.props(styles.row)}>
        {/* 收藏按钮 */}
        <View {...stylex.props(styles.favWrap)}>
          <View
            trackTarget="toggle-favorite"
            trackElementType="button"
            trackElementText={isFavorite ? '取消收藏' : '收藏'}
            onClick={onToggleFavorite}
            {...stylex.props(styles.favBtn, isFavorite && styles.favBtnActive)}
          >
            <Text {...stylex.props(isFavorite ? styles.favIconActive : styles.favIcon)}>{isFavorite ? '♥' : '♡'}</Text>
          </View>
        </View>
        {/* 客服按钮 */}
        <View
          trackTarget="contact-service"
          trackElementType="button"
          trackElementText="客服"
          onClick={onContactService}
          {...stylex.props(styles.serviceBtn)}
        >
          <Text {...stylex.props(styles.serviceIcon)}>📞</Text>
          {/* <Text className='text-sm font-medium text-gray-600'>客服</Text> */}
        </View>

        {/* 主要操作按钮(F-PRO-004:加入购物车 + 立即购买) */}
        {status === 'ONLINE' && (
          <View {...stylex.props(styles.mainWrap)}>
            {/* 加入购物车按钮(幽灵按钮:品牌蓝边框字,§2) */}
            <AtButton
              trackTarget="add-to-cart"
              trackElementType="button"
              trackElementText={stock <= 0 ? '暂时缺货' : '加入购物车'}
              trackDisabled={stock <= 0}
              onClick={onAddToCart}
              size='small'
              {...stylex.props(styles.abtn)}
              customStyle={{
                backgroundColor: stock <= 0 ? COLORS.textWeak : 'transparent',
                borderColor: stock <= 0 ? COLORS.textWeak : COLORS.brand,
                color: stock <= 0 ? COLORS.card : COLORS.brand,
                borderWidth: '1px',
                borderStyle: 'solid'
              }}
            >
              {stock <= 0 ? '暂时缺货' : '加入购物车'}
            </AtButton>
            {/* 立即购买按钮(强调色 #FF5436 底白字,§2) */}
            <AtButton
              trackTarget="buy-now"
              trackElementType="button"
              trackElementText={stock <= 0 ? '暂时缺货' : '立即购买'}
              trackDisabled={stock <= 0}
              onClick={onBuyNow}
              type='primary'
              size='small'
              {...stylex.props(styles.abtn)}
              customStyle={{
                backgroundColor: stock <= 0 ? COLORS.textWeak : COLORS.action,
                borderColor: stock <= 0 ? COLORS.textWeak : COLORS.action,
                color: COLORS.card,
                boxShadow: stock <= 0 ? 'none' : '0 2px 8px rgba(255, 84, 54, 0.30)'
              }}
            >
              {stock <= 0 ? '暂时缺货' : '立即购买'}
            </AtButton>
          </View>
        )}

      </View>
    </View>
  )
}

export default ActionButtons
