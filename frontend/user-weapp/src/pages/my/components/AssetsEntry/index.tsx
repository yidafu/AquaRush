import React from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import CustomIcon from '@/components/CustomIcon'
import { authService } from '@/utils/auth'

/**
 * 「我的资产」聚合入口(my.html:单个 weui-cell):
 * 图标 + 标题 + 摘要行(登录态区分文案)+ 箭头;点击进 assets 聚合页。
 * 摘要行不做数量统计(需额外查询,后续再接);无储值余额(2026-10-09 合规决策)。
 * 样式:StyleX(原同目录 scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用)
 */
const styles = stylex.create({
  icon: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    color: 'var(--weui-BRAND)',
  },
  summary: {
    marginTop: '6rpx',
    fontSize: 'var(--weui-fs-tips)',
    lineHeight: 1.4,
    color: 'var(--weui-FG-1)',
  },
})
const AssetsEntry: React.FC = () => {
  const isLoggedIn = authService.isAuthenticated()
  const summary = isLoggedIn
    ? '优惠券 · 水票 · 收藏 · 水桶账户'
    : '登录后查看优惠券 · 水票 · 收藏 · 水桶账户'

  const handleClick = (): void => {
    Taro.navigateTo({ url: '/pages/assets/index' })
  }

  return (
    <>
      <View className='weui-cells__title'>我的资产</View>
      <View className='weui-cells'>
        <View
          className='weui-cell'
          trackTarget='my-assets-entry'
          trackElementType='cell'
          trackElementText='我的资产'
          onClick={handleClick}
        >
          <View className='weui-cell__hd'>
            <View {...props('weui-cell__icon', styles.icon)}>
              <CustomIcon value='/assets/icons/order/credit-card' size={24} />
            </View>
          </View>
          <View className='weui-cell__bd'>
            <View>
              <Text>我的资产</Text>
            </View>
            <View {...stylex.props(styles.summary)}>
              <Text>{summary}</Text>
            </View>
          </View>
          <View className='weui-cell__ft weui-cell__ft_in-access' />
        </View>
      </View>
    </>
  )
}

export default AssetsEntry
